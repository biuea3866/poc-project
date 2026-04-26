#!/usr/bin/env python3
"""
qa-followup-extract.py — QA 보고서(.md) 의 "후속 티켓 제안" 섹션을 파싱해
GitHub Issue 페이로드를 출력한다.

기대 포맷 (docs/qa/2026-04-sprint-4.md 등):

    ## 후속 티켓 제안

    | 우선순위 | 제목 | 사유 | 예상공수 | 연관 TC |
    |---|---|---|---|---|
    | P1 | 관리자 엔드포인트 권한 강화 | TC-014 우회 | 1d | TC-014 |
    | P2 | Kafka DLQ 재처리 배치 | TC-021 | 2d | TC-021 |

사용 예:
  python3 .claude/scripts/qa-followup-extract.py --file docs/qa/2026-04-sprint-4.md --format github
  python3 .claude/scripts/qa-followup-extract.py --changed-files "$(git diff --name-only HEAD^ HEAD)"

Exit codes:
  0 = OK (0 건이어도 정상)
  2 = 스크립트 오류
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path


SECTION_DEFAULT = "## 후속 티켓 제안"
HEADER_DEFAULT = ["우선순위", "제목", "사유", "예상공수", "연관 TC"]


@dataclass
class Followup:
    priority: str
    title: str
    reason: str
    effort: str
    related_tc: str
    source_report: str

    def to_issue(self, labels: list[str]) -> dict:
        body_lines = [
            f"**우선순위:** {self.priority}",
            f"**예상 공수:** {self.effort}",
            f"**연관 TC:** {self.related_tc}",
            f"**소스 보고서:** `{self.source_report}`",
            "",
            "## 사유",
            self.reason,
            "",
            "## 체크리스트",
            "- [ ] TDD/ADR 문서 업데이트",
            "- [ ] 구현 + 테스트",
            "- [ ] QA 재검증",
        ]
        return {
            "title": f"[QA-followup][{self.priority}] {self.title}",
            "body": "\n".join(body_lines),
            "labels": labels,
        }


# ---------- 파서 ----------


def _find_section(lines: list[str], marker: str) -> tuple[int, int]:
    """marker 로 시작하는 섹션의 [start, end) 라인 인덱스 반환. 없으면 (-1,-1)."""
    start = -1
    for idx, line in enumerate(lines):
        if line.strip().startswith(marker):
            start = idx + 1
            break
    if start < 0:
        return -1, -1
    # 다음 h2 섹션 또는 파일 끝
    end = len(lines)
    for idx in range(start, len(lines)):
        if re.match(r"^##\s+", lines[idx]):
            end = idx
            break
    return start, end


def _parse_table(lines: list[str], headers: list[str]) -> list[dict]:
    """| 로 시작하는 연속 라인을 Markdown 표로 파싱."""
    rows: list[dict] = []
    header_idx = -1
    for idx, line in enumerate(lines):
        if not line.strip().startswith("|"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if header_idx < 0:
            # 첫 '|' 라인 = 헤더
            header_idx = idx
            # 헤더 셀이 기대값과 하나라도 다르면 파싱 포기
            if len(cells) < len(headers):
                return []
            continue
        if re.match(r"^\|[\s\-:|]+\|?\s*$", line.strip()):
            # 구분선 |---|---|
            continue
        if len(cells) < len(headers):
            continue
        row = {headers[i]: cells[i] for i in range(len(headers))}
        rows.append(row)
    return rows


def parse_report(path: Path, section: str, headers: list[str]) -> list[Followup]:
    try:
        text = path.read_text(encoding="utf-8")
    except OSError:
        return []
    lines = text.splitlines()
    start, end = _find_section(lines, section)
    if start < 0:
        return []
    rows = _parse_table(lines[start:end], headers)
    out: list[Followup] = []
    for r in rows:
        out.append(
            Followup(
                priority=r.get("우선순위", "").strip(),
                title=r.get("제목", "").strip(),
                reason=r.get("사유", "").strip(),
                effort=r.get("예상공수", "").strip(),
                related_tc=r.get("연관 TC", "").strip(),
                source_report=str(path),
            )
        )
    # 빈 행 제거
    return [f for f in out if f.title]


# ---------- 대상 파일 수집 ----------


def _collect_files(
    root: Path,
    explicit: list[str] | None,
    changed: str | None,
    glob: str,
) -> list[Path]:
    if explicit:
        return [
            (root / p).resolve() if not Path(p).is_absolute() else Path(p)
            for p in explicit
        ]
    if changed:
        out: list[Path] = []
        for tok in changed.replace("\n", " ").split():
            p = (root / tok).resolve() if not Path(tok).is_absolute() else Path(tok)
            if p.exists() and p.suffix == ".md":
                out.append(p)
        # glob 에 매칭되는 것만 통과
        return [p for p in out if _match_glob(p, glob, root)]
    return list(root.glob(glob))


def _match_glob(path: Path, glob: str, root: Path) -> bool:
    rel = path.relative_to(root) if path.is_absolute() and root in path.parents else path
    return Path(str(rel)).match(glob) or path.match(glob)


# ---------- main ----------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".")
    parser.add_argument("--file", action="append", default=[], help="대상 파일 직접 지정")
    parser.add_argument(
        "--changed-files",
        default=None,
        help="git diff 결과(공백 구분) — docs/qa/*.md 만 자동 필터",
    )
    parser.add_argument(
        "--glob",
        default="docs/qa/**/*.md",
        help="기본 수집 글롭 (file/changed-files 미지정 시)",
    )
    parser.add_argument("--section", default=SECTION_DEFAULT)
    parser.add_argument("--headers", default=",".join(HEADER_DEFAULT))
    parser.add_argument(
        "--format",
        choices=("json", "github", "md"),
        default="json",
        help="json=원시, github=Issue 페이로드 배열, md=사람이 볼 보고",
    )
    parser.add_argument(
        "--labels",
        default="qa-followup,sprint-backlog",
        help="생성할 Issue 에 부착할 라벨 (콤마 구분)",
    )
    parser.add_argument("--out", default=None)
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    headers = [h.strip() for h in args.headers.split(",") if h.strip()]
    labels = [l.strip() for l in args.labels.split(",") if l.strip()]

    files = _collect_files(root, args.file, args.changed_files, args.glob)
    followups: list[Followup] = []
    for f in files:
        followups.extend(parse_report(f, args.section, headers))

    if args.format == "github":
        payload = [fu.to_issue(labels) for fu in followups]
        output = json.dumps(payload, ensure_ascii=False, indent=2)
    elif args.format == "md":
        lines = [f"# QA Follow-up Extract", "", f"Total: {len(followups)}", ""]
        for fu in followups:
            lines.append(f"- [{fu.priority}] {fu.title} ({fu.effort}) — {fu.related_tc}")
            lines.append(f"  source: `{fu.source_report}`")
        output = "\n".join(lines) + "\n"
    else:
        output = json.dumps(
            [fu.__dict__ for fu in followups], ensure_ascii=False, indent=2
        )

    if args.out:
        Path(args.out).parent.mkdir(parents=True, exist_ok=True)
        Path(args.out).write_text(output, encoding="utf-8")
    else:
        sys.stdout.write(output)

    return 0


if __name__ == "__main__":
    sys.exit(main())
