#!/usr/bin/env python3
"""
Harness Audit — harness-rules.json 기반 전수/diff 감사.

용도:
  - nightly: 코드베이스 전수 스캔 → silent-pass 감지
  - PR: --diff-files 로 변경 파일만 스캔

사용법:
  python3 harness-audit.py                                # 전체 스캔
  python3 harness-audit.py --diff-files <f1> <f2> ...     # 지정 파일만
  python3 harness-audit.py --fail-on error                # severity error 1건이상이면 exit 2
  python3 harness-audit.py --rules <path>                 # custom rules path

종료 코드:
  0 = 위반 없음 또는 fail-on 미충족
  2 = fail-on 충족 (위반 검출)
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from fnmatch import fnmatch
from pathlib import Path


def load_rules(path: Path) -> dict:
    if not path.exists():
        sys.stderr.write(f"[harness-audit] rules 파일 없음: {path}\n")
        sys.exit(2)
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        sys.stderr.write(f"[harness-audit] JSON 파싱 실패: {e}\n")
        sys.exit(2)


def collect_files(diff_files: list[str] | None, root: Path) -> list[Path]:
    if diff_files:
        return [Path(p) for p in diff_files if Path(p).exists()]
    files: list[Path] = []
    for ext in ("*.kt", "*.java", "*.ts", "*.tsx", "*.sql"):
        files.extend(root.rglob(ext))
    # 빌드/노드 디렉토리 제외
    excluded = {"build", "node_modules", "dist", "out", ".gradle", ".idea"}
    return [p for p in files if not any(part in excluded for part in p.parts)]


def glob_match(path: str, pattern: str) -> bool:
    if "/" in pattern or "**" in pattern:
        return fnmatch(path, pattern)
    return fnmatch(Path(path).name, pattern)


def strip_comments_kt(content: str) -> str:
    # 라인 주석 + 블록 주석 제거 (간이 — 룰 false positive 방지)
    content = re.sub(r"//.*$", "", content, flags=re.MULTILINE)
    content = re.sub(r"/\*.*?\*/", "", content, flags=re.DOTALL)
    return content


def audit_file(rules: list[dict], path: Path) -> list[dict]:
    findings: list[dict] = []
    try:
        raw = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return findings
    content = strip_comments_kt(raw) if path.suffix in (".kt", ".java", ".ts", ".tsx") else raw

    for rule in rules:
        file_glob = rule.get("file_glob", "*")
        if not glob_match(str(path), file_glob):
            continue
        excl = rule.get("exclude_glob")
        if excl and glob_match(str(path), excl):
            continue
        pattern = rule.get("pattern", "")
        if not pattern:
            continue
        try:
            for m in re.finditer(pattern, content):
                line_no = content[: m.start()].count("\n") + 1
                findings.append(
                    {
                        "rule_id": rule.get("id", "?"),
                        "file": str(path),
                        "line": line_no,
                        "severity": rule.get("severity", "error"),
                        "message": rule.get("message", ""),
                        "match": m.group(0)[:80],
                    }
                )
        except re.error:
            continue
    return findings


def collect_layer_findings(rules_layer: list[dict], path: Path) -> list[dict]:
    return audit_file(rules_layer, path)


def main() -> int:
    parser = argparse.ArgumentParser(description="Harness Audit")
    parser.add_argument("--diff-files", nargs="*", default=None)
    parser.add_argument("--fail-on", choices=["any", "error"], default="error")
    parser.add_argument(
        "--rules",
        default=str(Path(__file__).parent.parent / "harness-rules.json"),
    )
    parser.add_argument("--root", default=".")
    args = parser.parse_args()

    rules_data = load_rules(Path(args.rules))
    forbidden = (rules_data.get("forbidden_patterns") or {}).get("rules", [])
    layer = (rules_data.get("layer_dependency") or {}).get("rules", [])
    all_rules = forbidden + layer

    files = collect_files(args.diff_files, Path(args.root).resolve())
    if not files:
        print("[harness-audit] 검사 대상 파일 없음")
        return 0

    findings: list[dict] = []
    for path in files:
        findings.extend(audit_file(all_rules, path))

    if not findings:
        print(f"[harness-audit] {len(files)}개 파일 검사 — 위반 없음")
        return 0

    by_rule: dict[str, list[dict]] = {}
    for f in findings:
        by_rule.setdefault(f["rule_id"], []).append(f)

    print(f"[harness-audit] {len(findings)}건 위반 검출 ({len(by_rule)}개 룰)\n")
    for rule_id, items in sorted(by_rule.items()):
        sev = items[0]["severity"].upper()
        msg = items[0]["message"]
        print(f"## [{sev}] {rule_id}: {msg}")
        for f in items[:20]:
            print(f"  - {f['file']}:{f['line']}  `{f['match']}`")
        if len(items) > 20:
            print(f"  ... 외 {len(items) - 20}건")
        print()

    if args.fail_on == "any":
        return 2
    if any(f["severity"] == "error" for f in findings):
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
