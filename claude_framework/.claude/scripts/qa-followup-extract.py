#!/usr/bin/env python3
"""
QA Follow-up Extractor — `docs/qa/*.md` 의 "후속 티켓" 섹션을 파싱해
Issue 자동 발행용 JSON 또는 gh CLI 명령을 출력.

QA 보고서 형식 가정 (기본):
  ## 후속 티켓 / 후속 작업 / 후속 조치
  | 티켓 | 근거 | 우선순위 |
  |------|------|---------|
  | XX-123 X 제목 | ... | P1 |
  | XX-124 Y 제목 | ... | P2 |

사용법:
  python3 qa-followup-extract.py <qa.md> [--format json|gh]
  python3 qa-followup-extract.py docs/qa/sprint-4-final.md --format gh

종료 코드:
  0 = 추출 성공 (또는 후속 티켓 섹션 없음)
  2 = 파싱 실패 / 파일 없음
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


SECTION_HEADERS = [
    r"^##+\s*후속\s*티켓",
    r"^##+\s*후속\s*작업",
    r"^##+\s*후속\s*조치",
    r"^##+\s*Follow[-\s]?up",
]


def find_section(content: str) -> str:
    pattern = "|".join(SECTION_HEADERS)
    m = re.search(pattern, content, re.MULTILINE | re.IGNORECASE)
    if not m:
        return ""
    section_start = m.end()
    next_section = re.search(r"^##\s", content[section_start:], re.MULTILINE)
    if next_section:
        return content[section_start : section_start + next_section.start()]
    return content[section_start:]


def parse_table(section: str) -> list[dict]:
    """| 티켓 | 근거 | 우선순위 | 형식 파싱."""
    rows: list[dict] = []
    lines = [
        line.strip()
        for line in section.split("\n")
        if line.strip().startswith("|") and "---" not in line
    ]
    if len(lines) < 2:
        return rows
    header = [c.strip().lower() for c in lines[0].strip("|").split("|")]
    for line in lines[1:]:
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) != len(header):
            continue
        row = dict(zip(header, cells))
        ticket_field = next((row[k] for k in row if "티켓" in k or "ticket" in k), "")
        priority_field = next(
            (row[k] for k in row if "우선" in k or "priority" in k), "P3"
        )
        reason_field = next((row[k] for k in row if "근거" in k or "reason" in k), "")
        if not ticket_field:
            continue
        # "RC-BE-441 Presentation 통합 테스트" → key="RC-BE-441", title="Presentation..."
        m = re.match(r"^([A-Z]+-\w+(?:-\w+)*)\s+(.+)$", ticket_field)
        if m:
            key, title = m.group(1), m.group(2)
        else:
            key, title = "", ticket_field
        rows.append(
            {
                "key": key,
                "title": title,
                "priority": priority_field,
                "reason": reason_field,
            }
        )
    return rows


def output_json(rows: list[dict]) -> None:
    print(json.dumps(rows, ensure_ascii=False, indent=2))


def output_gh(rows: list[dict]) -> None:
    """gh issue create 명령을 stdout 에 출력."""
    for r in rows:
        title = (
            f"[{r['key']}] {r['title']}"
            if r["key"]
            else r["title"]
        )
        body = f"**우선순위**: {r['priority']}\n\n**근거**: {r['reason']}\n\n_QA 후속 티켓 자동 발행_"
        # 안전한 escape
        title_safe = title.replace('"', '\\"')
        body_safe = body.replace('"', '\\"').replace("\n", "\\n")
        print(
            f'gh issue create --title "{title_safe}" --body "{body_safe}" --label "qa-followup,{r["priority"]}"'
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("qa_file")
    parser.add_argument("--format", choices=["json", "gh"], default="json")
    args = parser.parse_args()

    path = Path(args.qa_file)
    if not path.exists():
        sys.stderr.write(f"파일 없음: {path}\n")
        return 2

    content = path.read_text(encoding="utf-8")
    section = find_section(content)
    if not section:
        sys.stderr.write("[qa-followup] '후속 티켓' 섹션 미발견 — 추출할 것 없음\n")
        return 0

    rows = parse_table(section)
    if not rows:
        sys.stderr.write("[qa-followup] 표 파싱 실패 — 형식을 확인하세요\n")
        return 0

    if args.format == "json":
        output_json(rows)
    else:
        output_gh(rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
