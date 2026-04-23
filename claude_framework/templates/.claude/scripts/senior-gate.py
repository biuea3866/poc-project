#!/usr/bin/env python3
"""
senior-gate.py — PR 머지 전 보안/구조 휴리스틱 Critical 차단.

pr-reviewer 에이전트가 반복적으로 잡아내는 '구조적 Critical' 유형을
규칙 기반으로 선제 차단한다. harness-rules.json의 regex 로는 잡기 힘든
(클래스/어노테이션/파라미터 조합) 이슈를 다룬다.

차단 대상:
  1. admin_endpoint_without_role
     — Controller 메서드 path/매핑에 'admin'(대소문자 무시)이 포함되면
       @RoleRequired 또는 @PreAuthorize 가 필수.
  2. controller_http_servlet_request
     — Controller 클래스 안에서 HttpServletRequest 파라미터 직접 주입 금지.
  3. listener_direct_repository
     — Listener/Consumer 클래스의 생성자/필드에 *Repository 주입 금지.
  4. service_usecase_without_transactional
     — *UseCase 또는 @Service 클래스의 public 메서드가 Repository를 호출하는데
       클래스/메서드 어느 곳에도 @Transactional 이 없으면 차단.

사용 예:
  python3 .claude/scripts/senior-gate.py --diff-files "$(git diff --name-only origin/main...HEAD)"
  python3 .claude/scripts/senior-gate.py --paths src/main/kotlin --format json

Exit codes:
  0 = 통과
  1 = 하나 이상 Critical 차단
  2 = 스크립트 오류
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


# ---------- 모델 ----------


@dataclass
class Finding:
    rule: str
    file: str
    line: int
    snippet: str
    message: str


@dataclass
class GateReport:
    findings: list[Finding] = field(default_factory=list)
    scanned: int = 0


# ---------- 공통 유틸 ----------


_MAPPING_ANNOTATIONS = (
    "@RequestMapping",
    "@GetMapping",
    "@PostMapping",
    "@PutMapping",
    "@PatchMapping",
    "@DeleteMapping",
)
_ROLE_ANNOTATIONS = ("@RoleRequired", "@PreAuthorize", "@Secured", "@RolesAllowed")


def _read(path: Path) -> list[str]:
    try:
        return path.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError:
        return []


def _is_controller(path: str) -> bool:
    name = Path(path).name
    return name.endswith("Controller.kt") or name.endswith("ApiController.kt")


def _is_listener(path: str) -> bool:
    name = Path(path).name
    return "Listener" in name or "Consumer" in name


def _is_usecase(path: str) -> bool:
    return Path(path).name.endswith("UseCase.kt")


def _is_service(lines: list[str]) -> bool:
    head = "\n".join(lines[:40])
    return bool(re.search(r"@Service\b", head))


# ---------- 룰 구현 ----------


def check_admin_endpoint_without_role(path: str, lines: list[str]) -> list[Finding]:
    if not _is_controller(path):
        return []
    findings: list[Finding] = []

    # 1) 메서드 단위로 스캔: mapping 어노테이션이 붙은 fun 선언 바로 위 N줄에 RoleRequired 가 있는지 본다.
    for idx, line in enumerate(lines):
        stripped = line.strip()
        if not any(stripped.startswith(ann) for ann in _MAPPING_ANNOTATIONS):
            continue
        # path 문자열에 admin 존재 여부 (매핑 어노테이션 라인 + 다음 몇 줄)
        window = "\n".join(lines[idx : idx + 4])
        if not re.search(r"admin", window, re.IGNORECASE):
            continue
        # 위쪽 6줄 내에서 role 어노테이션 찾기
        above = lines[max(0, idx - 6) : idx]
        has_role = any(any(ann in a for ann in _ROLE_ANNOTATIONS) for a in above)
        if has_role:
            continue
        findings.append(
            Finding(
                rule="admin_endpoint_without_role",
                file=path,
                line=idx + 1,
                snippet=stripped[:160],
                message=(
                    "관리자 엔드포인트에 @RoleRequired / @PreAuthorize 누락 — "
                    "권한 체크 없이 배포되면 권한 상승 취약점."
                ),
            )
        )
    return findings


def check_controller_http_servlet_request(path: str, lines: list[str]) -> list[Finding]:
    if not _is_controller(path):
        return []
    findings: list[Finding] = []
    for idx, line in enumerate(lines):
        if "HttpServletRequest" not in line:
            continue
        if line.lstrip().startswith(("import ", "//", "/*", "*")):
            continue
        findings.append(
            Finding(
                rule="controller_http_servlet_request",
                file=path,
                line=idx + 1,
                snippet=line.strip()[:160],
                message=(
                    "Controller에서 HttpServletRequest 직접 주입 금지 — "
                    "@RequestHeader / @CookieValue / Spring Security Principal 사용."
                ),
            )
        )
    return findings


def check_listener_direct_repository(path: str, lines: list[str]) -> list[Finding]:
    if not _is_listener(path):
        return []
    findings: list[Finding] = []
    joined = "\n".join(lines)
    # 생성자/필드 주입에서 *Repository 참조
    for idx, line in enumerate(lines):
        m = re.search(r"\b([A-Z]\w*Repository)\b", line)
        if not m:
            continue
        if line.lstrip().startswith(("import ", "//", "/*", "*")):
            continue
        # 주석/import 가 아니고 파라미터/필드 선언 문맥
        if ":" not in line and "val " not in line and "private " not in line:
            continue
        findings.append(
            Finding(
                rule="listener_direct_repository",
                file=path,
                line=idx + 1,
                snippet=line.strip()[:160],
                message=(
                    f"Listener/Consumer에 {m.group(1)} 직접 주입 금지 — "
                    "Facade/Service 경유."
                ),
            )
        )
    return findings


def check_service_usecase_without_transactional(
    path: str, lines: list[str]
) -> list[Finding]:
    if not (_is_usecase(path) or (_is_service(lines))):
        return []
    # Repository 호출을 하는데 @Transactional 이 하나도 없으면 차단
    joined = "\n".join(lines)
    uses_repo = bool(re.search(r"Repository\s*\.", joined)) or bool(
        re.search(r"\b[a-z]\w*Repository\b", joined)
    )
    if not uses_repo:
        return []
    has_tx = bool(re.search(r"@Transactional\b", joined))
    if has_tx:
        return []
    # 파일 최상단 클래스 선언 라인 찾기
    class_line = 1
    for idx, line in enumerate(lines):
        if re.match(r"^(open\s+|abstract\s+)?class\s+\w+", line.strip()):
            class_line = idx + 1
            break
    return [
        Finding(
            rule="service_usecase_without_transactional",
            file=path,
            line=class_line,
            snippet=lines[class_line - 1].strip()[:160] if lines else "",
            message=(
                "UseCase/@Service 가 Repository 를 호출하는데 @Transactional "
                "선언이 없음 — UseCase 또는 DomainService 레벨에서 트랜잭션 경계 명시 필요."
            ),
        )
    ]


_CHECKS = (
    check_admin_endpoint_without_role,
    check_controller_http_servlet_request,
    check_listener_direct_repository,
    check_service_usecase_without_transactional,
)


# ---------- 드라이버 ----------


def _collect_files(root: Path, diff_files: str | None, paths: list[str]) -> list[Path]:
    out: list[Path] = []
    if diff_files:
        for tok in diff_files.replace("\n", " ").split():
            p = (root / tok).resolve() if not Path(tok).is_absolute() else Path(tok)
            if p.exists() and p.is_file() and p.suffix == ".kt":
                out.append(p)
        return out
    scan_roots = [root / p for p in (paths or ["src"])]
    for sr in scan_roots:
        if not sr.exists():
            continue
        for f in sr.rglob("*.kt"):
            if any(seg in f.parts for seg in ("build", "generated", ".gradle")):
                continue
            out.append(f)
    return out


def run(root: Path, files: list[Path]) -> GateReport:
    report = GateReport(scanned=len(files))
    for f in files:
        rel = str(f.relative_to(root)) if f.is_absolute() and root in f.parents else str(f)
        lines = _read(f)
        if not lines:
            continue
        for check in _CHECKS:
            report.findings.extend(check(rel, lines))
    return report


def _format(report: GateReport, fmt: str) -> str:
    if fmt == "json":
        return json.dumps(
            {
                "scanned": report.scanned,
                "findings": [f.__dict__ for f in report.findings],
            },
            ensure_ascii=False,
            indent=2,
        )
    lines = [f"# Senior Review Gate", "", f"Scanned: {report.scanned}", ""]
    if not report.findings:
        lines.append("PASS — no critical findings.")
    else:
        lines.append(f"FAIL — {len(report.findings)} critical finding(s):")
        lines.append("")
        for f in report.findings:
            lines.append(f"- [{f.rule}] `{f.file}:{f.line}`")
            lines.append(f"  - {f.message}")
            lines.append(f"  - snippet: `{f.snippet}`")
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".")
    parser.add_argument("--diff-files", default=None)
    parser.add_argument(
        "--paths",
        default="src",
        help="콤마 구분 스캔 루트 (diff-files 없을 때만 사용)",
    )
    parser.add_argument("--format", choices=("md", "json"), default="md")
    parser.add_argument("--out", default=None)
    parser.add_argument(
        "--allow-empty-diff",
        action="store_true",
        help="diff-files 가 비어 있으면 exit 0",
    )
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    files = _collect_files(
        root, args.diff_files, [p.strip() for p in args.paths.split(",") if p.strip()]
    )
    if args.diff_files is not None and not files and args.allow_empty_diff:
        sys.stdout.write("# Senior Review Gate\n\nNo changed .kt files.\n")
        return 0

    report = run(root, files)
    out = _format(report, args.format)
    if args.out:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(out, encoding="utf-8")
    else:
        sys.stdout.write(out)

    return 1 if report.findings else 0


if __name__ == "__main__":
    sys.exit(main())
