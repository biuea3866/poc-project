#!/usr/bin/env python3
"""
PR Senior Gate — LLM-free Critical 검출.

GitHub Actions 의 pr-senior-review.yml 워크플로우에서 호출되어
PR diff 에 다음 4유형 Critical 패턴이 있는지 검출. 있으면 exit 2.

  1. 관리자 엔드포인트 @RoleRequired / @PreAuthorize / @Secured 누락
  2. HttpServletRequest 직접 주입 (Controller 파라미터)
  3. Listener 의 Repository 직접 호출 (DomainService 우회)
  4. UseCase @Transactional 누락

사용법:
  python3 senior-gate.py --diff-files <file1> <file2> ...
  python3 senior-gate.py --pr <PR번호>      # gh CLI 로 변경 파일 가져옴

종료 코드:
  0 = Critical 없음
  2 = Critical 1건 이상 (PR fail)
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


CRITICALS: list[dict] = [
    {
        "id": "admin-without-role-required",
        "description": "관리자 엔드포인트에 @RoleRequired/@PreAuthorize/@Secured 누락",
        "file_glob": re.compile(r".*Admin\w*Controller\.kt$|.*Admin\w*Resource\.kt$"),
        "must_have_any": [
            r"@RoleRequired",
            r"@PreAuthorize",
            r"@Secured",
        ],
        "scope_keyword": "fun ",  # 함수 단위 검사
    },
    {
        "id": "http-servlet-request-injection",
        "description": "Controller 에 HttpServletRequest 직접 주입 (인증 컨텍스트는 별도 추상화 사용)",
        "file_glob": re.compile(r".*Controller\.kt$|.*Resource\.kt$"),
        "forbidden": [r"\bHttpServletRequest\b"],
    },
    {
        "id": "listener-repository-direct",
        "description": "Listener 가 Repository 직접 호출 (DomainService 경유 필수)",
        "file_glob": re.compile(r".*Listener\.kt$"),
        "forbidden": [r"\b\w*Repository\b\s*[,)]", r"\bjpaRepository\b"],
    },
    {
        "id": "usecase-missing-transactional",
        "description": "UseCase 에 @Transactional 누락",
        "file_glob": re.compile(r".*UseCase\.kt$"),
        "must_have_any": [r"@Transactional"],
        "exclude_if_match": [r"@TransactionalEventListener", r"interface\s+\w+UseCase"],
    },
]


def get_pr_files(pr_number: str) -> list[Path]:
    try:
        result = subprocess.run(
            ["gh", "pr", "view", pr_number, "--json", "files", "-q", ".files[].path"],
            capture_output=True,
            text=True,
            check=True,
            timeout=30,
        )
    except (subprocess.SubprocessError, FileNotFoundError):
        return []
    return [Path(p.strip()) for p in result.stdout.splitlines() if p.strip()]


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return ""


def check_admin_role(rule: dict, content: str) -> list[str]:
    findings: list[str] = []
    pattern_any = "|".join(rule["must_have_any"])
    fn_re = re.compile(r"^\s*(@\w+(\([^)]*\))?\s*)*(suspend\s+)?fun\s+(\w+)\s*\(", re.MULTILINE)
    for match in fn_re.finditer(content):
        # 함수 시그니처 직전 32라인을 어노테이션 윈도우로
        start = max(0, match.start() - 2000)
        window = content[start : match.end()]
        if re.search(pattern_any, window):
            continue
        # API 핸들러로 보이는 경우만 (GetMapping/PostMapping 등 매핑 어노테이션 보유)
        if not re.search(r"@(Get|Post|Put|Delete|Patch|Request)Mapping", window):
            continue
        findings.append(f"function `{match.group(4)}` (offset {match.start()})")
    return findings


def check_pattern_forbidden(rule: dict, content: str) -> list[str]:
    findings: list[str] = []
    for pat in rule["forbidden"]:
        for m in re.finditer(pat, content):
            line_no = content[: m.start()].count("\n") + 1
            findings.append(f"line {line_no}: `{m.group(0)}`")
    return findings


def check_must_have(rule: dict, content: str) -> list[str]:
    must_any = "|".join(rule["must_have_any"])
    if re.search(must_any, content):
        return []
    for excl in rule.get("exclude_if_match", []):
        if re.search(excl, content):
            return []
    return ["전체 파일에서 어노테이션 미발견"]


def run_rule(rule: dict, files: list[Path]) -> list[str]:
    rule_findings: list[str] = []
    pattern: re.Pattern = rule["file_glob"]
    for path in files:
        if not pattern.match(str(path)):
            continue
        content = read_text(path)
        if not content:
            continue
        if rule["id"] == "admin-without-role-required":
            findings = check_admin_role(rule, content)
        elif "forbidden" in rule:
            findings = check_pattern_forbidden(rule, content)
        elif "must_have_any" in rule:
            findings = check_must_have(rule, content)
        else:
            findings = []
        for f in findings:
            rule_findings.append(f"  - `{path}`: {f}")
    return rule_findings


def main() -> int:
    parser = argparse.ArgumentParser(description="PR Senior Gate")
    parser.add_argument("--diff-files", nargs="*", help="검사할 파일 경로 목록")
    parser.add_argument("--pr", help="PR 번호 (gh CLI 로 파일 가져옴)")
    args = parser.parse_args()

    if args.diff_files:
        files = [Path(p) for p in args.diff_files if Path(p).exists()]
    elif args.pr:
        files = [p for p in get_pr_files(args.pr) if p.exists()]
    else:
        sys.stderr.write("usage: senior-gate.py --diff-files <files> | --pr <number>\n")
        return 2

    if not files:
        print("[senior-gate] 검사할 파일 없음 — 통과")
        return 0

    total_critical = 0
    print(f"[senior-gate] 검사 대상 {len(files)}개 파일\n")
    for rule in CRITICALS:
        findings = run_rule(rule, files)
        if findings:
            total_critical += len(findings)
            print(f"## ❌ Critical: {rule['id']}")
            print(f"   {rule['description']}\n")
            for f in findings:
                print(f)
            print()
        else:
            print(f"## ✅ {rule['id']}: 통과")

    print()
    if total_critical > 0:
        print(f"[senior-gate] Critical {total_critical}건 검출 — REQUEST_CHANGES")
        return 2
    print("[senior-gate] Critical 없음 — 통과")
    return 0


if __name__ == "__main__":
    sys.exit(main())
