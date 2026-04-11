#!/usr/bin/env python3
"""
Closet 프로젝트 GC(Garbage Collection) 스캐너.

코드베이스 전체를 스캔하여 harness-rules.json의 규칙 위반을 찾고,
위반 리포트를 생성하며, 자동 수정 가능한 항목은 fix 제안을 출력한다.

사용법:
  python3 harness-gc.py scan              — 전체 스캔 (리포트 출력)
  python3 harness-gc.py scan --json       — JSON 형식 리포트
  python3 harness-gc.py fix --dry-run     — 자동 수정 미리보기
  python3 harness-gc.py fix               — 자동 수정 실행
  python3 harness-gc.py watch             — 변경된 파일만 스캔 (git diff 기반)

종료 코드:
  0 = 위반 없음
  1 = 위반 감지 (scan), 또는 에러
"""

import json
import os
import re
import subprocess
import sys
from collections import defaultdict
from datetime import datetime, timezone
from fnmatch import fnmatch
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
RULES_FILE = SCRIPT_DIR / "harness-rules.json"
REPORT_FILE = SCRIPT_DIR / "gc-report.json"


def load_rules():
    with open(RULES_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def find_files(scan_dirs, file_glob):
    """scan_dirs에서 file_glob에 매칭되는 파일 목록 반환."""
    files = []
    for scan_dir in scan_dirs:
        full_dir = PROJECT_ROOT / scan_dir
        if not full_dir.exists():
            continue
        for path in full_dir.rglob("*"):
            if path.is_file() and fnmatch(path.name, file_glob):
                files.append(path)
    return files


def scan_file(file_path, rules):
    """파일 하나를 모든 규칙에 대해 스캔."""
    violations = []
    try:
        content = file_path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, PermissionError):
        return violations

    lines = content.split("\n")

    for rule in rules:
        file_glob = rule.get("file_glob", "*")
        if not fnmatch(file_path.name, file_glob):
            continue

        exclude_glob = rule.get("exclude_glob")
        if exclude_glob and fnmatch(str(file_path), exclude_glob):
            continue

        pattern = rule["pattern"]
        for line_num, line in enumerate(lines, 1):
            if re.search(pattern, line):
                violations.append({
                    "rule_id": rule["id"],
                    "file": str(file_path.relative_to(PROJECT_ROOT)),
                    "line": line_num,
                    "content": line.strip(),
                    "message": rule["message"],
                    "severity": rule.get("severity", "error"),
                })

    return violations


def cmd_scan(args):
    """전체 코드베이스 스캔."""
    rules_data = load_rules()
    forbidden = rules_data.get("forbidden_patterns", {}).get("rules", [])
    gc_config = rules_data.get("gc_scanner", {})
    scan_dirs = gc_config.get("scan_dirs", [])

    if not scan_dirs:
        print("gc_scanner.scan_dirs가 harness-rules.json에 정의되지 않았습니다.", file=sys.stderr)
        sys.exit(1)

    all_violations = []
    scanned_files = 0

    for rule in forbidden:
        file_glob = rule.get("file_glob", "*")
        files = find_files(scan_dirs, file_glob)
        for f in files:
            scanned_files += 1
            violations = scan_file(f, [rule])
            all_violations.extend(violations)

    # 중복 제거 (같은 파일 여러 glob에서 매칭될 수 있음)
    seen = set()
    unique_violations = []
    for v in all_violations:
        key = (v["rule_id"], v["file"], v["line"])
        if key not in seen:
            seen.add(key)
            unique_violations.append(v)

    # JSON 출력
    if "--json" in args:
        report = {
            "scanned_at": datetime.now(timezone.utc).isoformat(),
            "scanned_files": scanned_files,
            "total_violations": len(unique_violations),
            "violations": unique_violations,
            "by_rule": {},
        }
        by_rule = defaultdict(list)
        for v in unique_violations:
            by_rule[v["rule_id"]].append(v)
        report["by_rule"] = {k: len(v) for k, v in by_rule.items()}

        with open(REPORT_FILE, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        print(json.dumps(report, indent=2, ensure_ascii=False))
        return len(unique_violations) > 0

    # 텍스트 출력
    if not unique_violations:
        print(f"GC 스캔 완료: 위반 없음 (파일 {scanned_files}개 검사)")
        return False

    by_rule = defaultdict(list)
    for v in unique_violations:
        by_rule[v["rule_id"]].append(v)

    print(f"GC 스캔 결과: {len(unique_violations)}개 위반 감지 (파일 {scanned_files}개 검사)")
    print("=" * 70)

    for rule_id, violations in sorted(by_rule.items()):
        severity = violations[0]["severity"].upper()
        message = violations[0]["message"]
        print(f"\n[{severity}] {rule_id} ({len(violations)}건)")
        print(f"  규칙: {message}")
        for v in violations[:10]:  # 최대 10건만 표시
            print(f"  • {v['file']}:{v['line']} → {v['content'][:80]}")
        if len(violations) > 10:
            print(f"  ... 외 {len(violations) - 10}건")

    print(f"\n총 {len(unique_violations)}개 위반 | "
          f"auto-fixable: {sum(1 for v in unique_violations if v['rule_id'] in gc_config.get('auto_fixable', []))}건")

    return True


def cmd_fix(args):
    """자동 수정 가능한 위반 항목 수정."""
    dry_run = "--dry-run" in args
    rules_data = load_rules()
    gc_config = rules_data.get("gc_scanner", {})
    auto_fixable = set(gc_config.get("auto_fixable", []))

    # 자동 수정 맵핑
    fix_map = {
        "no-local-datetime": (r"\bLocalDateTime\b", "ZonedDateTime"),
        "no-db-boolean-type": (r"\bBOOLEAN\b", "TINYINT(1)"),
        "no-db-datetime-no-precision": (r"\bDATETIME\b(?!\s*\()", "DATETIME(6)"),
    }

    forbidden = rules_data.get("forbidden_patterns", {}).get("rules", [])
    scan_dirs = gc_config.get("scan_dirs", [])

    fixed_count = 0
    fixed_files = set()

    for rule in forbidden:
        if rule["id"] not in auto_fixable:
            continue
        if rule["id"] not in fix_map:
            continue

        search_pattern, replacement = fix_map[rule["id"]]
        file_glob = rule.get("file_glob", "*")
        exclude_glob = rule.get("exclude_glob")
        files = find_files(scan_dirs, file_glob)

        for f in files:
            if exclude_glob and fnmatch(str(f), exclude_glob):
                continue

            try:
                content = f.read_text(encoding="utf-8")
            except (UnicodeDecodeError, PermissionError):
                continue

            new_content = re.sub(search_pattern, replacement, content)
            if new_content != content:
                rel_path = f.relative_to(PROJECT_ROOT)
                count = len(re.findall(search_pattern, content))
                fixed_count += count
                fixed_files.add(str(rel_path))

                if dry_run:
                    print(f"[DRY-RUN] {rel_path}: {rule['id']} → {count}건 수정 예정")
                else:
                    f.write_text(new_content, encoding="utf-8")
                    print(f"[FIXED] {rel_path}: {rule['id']} → {count}건 수정")

    action = "수정 예정" if dry_run else "수정 완료"
    print(f"\n{action}: {fixed_count}건 ({len(fixed_files)}개 파일)")

    if not dry_run and fixed_count > 0:
        # LocalDateTime → ZonedDateTime 수정 시 import도 추가해야 함
        print("\n[주의] 자동 수정 후 확인 필요:")
        print("  1. ZonedDateTime 변경 시 import java.time.ZonedDateTime 추가 확인")
        print("  2. ./gradlew compileKotlin 으로 빌드 확인")
        print("  3. git diff 로 변경 내용 리뷰")


def cmd_watch(args):
    """git diff 기반 변경 파일만 스캔."""
    try:
        result = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACM"],
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
        )
        staged = subprocess.run(
            ["git", "diff", "--cached", "--name-only", "--diff-filter=ACM"],
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        print("git을 찾을 수 없습니다.", file=sys.stderr)
        sys.exit(1)

    changed_files = set(result.stdout.strip().split("\n") + staged.stdout.strip().split("\n"))
    changed_files = {f for f in changed_files if f}

    if not changed_files:
        print("변경된 파일이 없습니다.")
        return False

    rules_data = load_rules()
    forbidden = rules_data.get("forbidden_patterns", {}).get("rules", [])

    all_violations = []
    for file_str in changed_files:
        file_path = PROJECT_ROOT / file_str
        if not file_path.exists():
            continue
        violations = scan_file(file_path, forbidden)
        all_violations.extend(violations)

    if not all_violations:
        print(f"변경 파일 {len(changed_files)}개 검사: 위반 없음")
        return False

    print(f"변경 파일 {len(changed_files)}개 검사: {len(all_violations)}개 위반 감지")
    for v in all_violations:
        print(f"  [{v['severity'].upper()}] {v['file']}:{v['line']} — {v['message']}")

    return True


def main():
    if len(sys.argv) < 2:
        print("Usage: harness-gc.py <scan|fix|watch> [options]")
        print("  scan [--json]     — 전체 스캔")
        print("  fix [--dry-run]   — 자동 수정")
        print("  watch             — 변경 파일만 스캔")
        sys.exit(1)

    cmd = sys.argv[1]
    args = sys.argv[2:]

    if cmd == "scan":
        has_violations = cmd_scan(args)
        sys.exit(1 if has_violations else 0)
    elif cmd == "fix":
        cmd_fix(args)
    elif cmd == "watch":
        has_violations = cmd_watch(args)
        sys.exit(1 if has_violations else 0)
    else:
        print(f"Unknown command: {cmd}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
