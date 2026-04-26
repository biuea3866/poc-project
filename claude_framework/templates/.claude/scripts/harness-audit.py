#!/usr/bin/env python3
"""
harness-audit.py — 프로젝트 전체 또는 변경 파일에 대해 harness-rules.json 전수 감사.

사용 예:
  # 전체 감사 (nightly cron)
  python3 .claude/scripts/harness-audit.py --out .analysis/harness-audit/$(date +%F).md

  # PR 변경 파일만
  python3 .claude/scripts/harness-audit.py --diff-files "$(git diff --name-only origin/main...HEAD)"

  # JSON 출력 (GitHub Actions 파이프)
  python3 .claude/scripts/harness-audit.py --format json

Exit codes:
  0 = 위반 없음 또는 warning 만
  1 = error severity 위반 발견 (CI fail)
  2 = 스크립트 자체 오류 (설정 누락, JSON 파싱 실패 등)
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from fnmatch import fnmatch
from pathlib import Path
from typing import Iterable


# ---------- 주석/문자열 리터럴 필터 ----------

_LINE_COMMENT_RE = re.compile(r"^\s*(//|#)")
_BLOCK_COMMENT_OPEN = "/*"
_BLOCK_COMMENT_CLOSE = "*/"


def is_comment_or_string_line(line: str, in_block: bool) -> tuple[bool, bool]:
    """
    해당 줄이 주석/빈 줄인지 판정. (is_comment, new_in_block_state)
    블록 주석 상태는 호출자가 유지한다.
    문자열 리터럴 전체로 둘러싸인 간단 케이스만 거름 — 완벽한 파서는 아님.
    """
    stripped = line.strip()
    if not stripped:
        return True, in_block
    if in_block:
        if _BLOCK_COMMENT_CLOSE in stripped:
            return True, False
        return True, True
    if stripped.startswith(_BLOCK_COMMENT_OPEN):
        if _BLOCK_COMMENT_CLOSE in stripped[2:]:
            return True, False
        return True, True
    if _LINE_COMMENT_RE.match(line):
        return True, in_block
    return False, in_block


# ---------- 감사 모델 ----------


@dataclass
class Violation:
    rule_id: str
    severity: str
    file_path: str
    line: int
    message: str
    matched_text: str


@dataclass
class AuditReport:
    violations: list[Violation] = field(default_factory=list)
    scanned_files: int = 0
    applied_rules: int = 0

    @property
    def by_severity(self) -> dict[str, int]:
        out: dict[str, int] = {}
        for v in self.violations:
            out[v.severity] = out.get(v.severity, 0) + 1
        return out

    @property
    def by_rule(self) -> dict[str, int]:
        out: dict[str, int] = {}
        for v in self.violations:
            out[v.rule_id] = out.get(v.rule_id, 0) + 1
        return out


# ---------- glob / 파일 수집 ----------


def _glob_match(path: str, pattern: str) -> bool:
    """rule.file_glob 는 콤마로 여러 패턴 허용."""
    patterns = [p.strip() for p in pattern.split(",") if p.strip()]
    for pat in patterns:
        if "/" in pat or "**" in pat:
            if fnmatch(path, pat):
                return True
        else:
            if fnmatch(Path(path).name, pat):
                return True
    return False


def _glob_exclude(path: str, exclude: str | None) -> bool:
    if not exclude:
        return False
    return _glob_match(path, exclude)


def _collect_all_files(root: Path) -> list[Path]:
    """build/, .gradle/, node_modules/, 생성 코드 제외."""
    skip_dirs = {
        ".git",
        "build",
        "out",
        "target",
        "node_modules",
        ".gradle",
        ".idea",
        "dist",
        "generated",
    }
    result: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        parts = set(path.parts)
        if parts & skip_dirs:
            continue
        result.append(path)
    return result


# ---------- 룰 실행 ----------


def scan_file(
    file_path: Path,
    relative_path: str,
    rules: list[dict],
) -> list[Violation]:
    try:
        text = file_path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return []

    lines = text.splitlines()
    # 주석 라인 번호 세트 (1-indexed)
    comment_lines: set[int] = set()
    in_block = False
    for idx, line in enumerate(lines, start=1):
        is_comment, in_block = is_comment_or_string_line(line, in_block)
        if is_comment:
            comment_lines.add(idx)

    violations: list[Violation] = []
    for rule in rules:
        if not _glob_match(relative_path, rule.get("file_glob", "*")):
            continue
        if _glob_exclude(relative_path, rule.get("exclude_glob")):
            continue
        pattern = rule.get("pattern")
        if not pattern:
            continue
        try:
            regex = re.compile(pattern)
        except re.error:
            continue
        for line_no, line in enumerate(lines, start=1):
            if line_no in comment_lines:
                continue
            m = regex.search(line)
            if not m:
                continue
            violations.append(
                Violation(
                    rule_id=rule.get("id", "?"),
                    severity=rule.get("severity", "error"),
                    file_path=relative_path,
                    line=line_no,
                    message=rule.get("message", ""),
                    matched_text=m.group(0)[:120],
                )
            )
    return violations


# ---------- 엔트리 포인트 ----------


def _load_rules(rules_path: Path) -> list[dict]:
    try:
        raw = json.loads(rules_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        sys.stderr.write(f"[harness-audit] rules 로드 실패: {exc}\n")
        sys.exit(2)
    rules: list[dict] = []
    rules.extend((raw.get("forbidden_patterns") or {}).get("rules", []) or [])
    rules.extend((raw.get("layer_dependency") or {}).get("rules", []) or [])
    return rules


def _format_md(report: AuditReport, scope: str) -> str:
    lines: list[str] = []
    lines.append("# Harness Audit Report")
    lines.append("")
    lines.append(f"**Scope:** {scope}")
    lines.append(f"**Scanned files:** {report.scanned_files}")
    lines.append(f"**Rules applied:** {report.applied_rules}")
    lines.append(f"**Violations:** {len(report.violations)}")
    lines.append("")
    if report.violations:
        lines.append("## By severity")
        for sev, cnt in sorted(report.by_severity.items()):
            lines.append(f"- {sev}: {cnt}")
        lines.append("")
        lines.append("## By rule")
        for rule_id, cnt in sorted(report.by_rule.items(), key=lambda x: -x[1]):
            lines.append(f"- {rule_id}: {cnt}")
        lines.append("")
        lines.append("## Details")
        for v in report.violations:
            lines.append(
                f"- [{v.severity.upper()}] `{v.file_path}:{v.line}` — "
                f"{v.rule_id}: {v.message}"
            )
            lines.append(f"  - match: `{v.matched_text}`")
    else:
        lines.append("No violations.")
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="스캔 루트 (기본: cwd)")
    parser.add_argument(
        "--rules",
        default=".claude/harness-rules.json",
        help="룰 파일 경로",
    )
    parser.add_argument(
        "--diff-files",
        default=None,
        help="전수 대신 이 파일 목록만 스캔 (개행 또는 공백 구분)",
    )
    parser.add_argument("--out", default=None, help="보고서 출력 경로 (.md)")
    parser.add_argument(
        "--format", choices=("md", "json"), default="md", help="출력 포맷"
    )
    parser.add_argument(
        "--fail-on",
        choices=("error", "warning", "none"),
        default="error",
        help="지정 severity 이상이면 exit 1",
    )
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    rules_path = Path(args.rules)
    if not rules_path.is_absolute():
        rules_path = root / rules_path

    rules = _load_rules(rules_path)

    if args.diff_files:
        files: list[Path] = []
        for tok in args.diff_files.replace("\n", " ").split():
            p = (root / tok).resolve() if not Path(tok).is_absolute() else Path(tok)
            if p.exists() and p.is_file():
                files.append(p)
        scope = f"diff ({len(files)} files)"
    else:
        files = _collect_all_files(root)
        scope = f"full ({root})"

    report = AuditReport(scanned_files=len(files), applied_rules=len(rules))

    for f in files:
        rel = str(f.relative_to(root)) if f.is_absolute() else str(f)
        report.violations.extend(scan_file(f, rel, rules))

    if args.format == "json":
        output = json.dumps(
            {
                "scope": scope,
                "scanned_files": report.scanned_files,
                "applied_rules": report.applied_rules,
                "by_severity": report.by_severity,
                "by_rule": report.by_rule,
                "violations": [
                    {
                        "rule_id": v.rule_id,
                        "severity": v.severity,
                        "file": v.file_path,
                        "line": v.line,
                        "message": v.message,
                        "match": v.matched_text,
                    }
                    for v in report.violations
                ],
            },
            ensure_ascii=False,
            indent=2,
        )
    else:
        output = _format_md(report, scope)

    if args.out:
        out_path = Path(args.out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(output, encoding="utf-8")
    else:
        sys.stdout.write(output)

    # exit code
    if args.fail_on == "none":
        return 0
    threshold = ("error", "warning", "info") if args.fail_on == "warning" else ("error",)
    for v in report.violations:
        if v.severity in threshold:
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
