#!/usr/bin/env python3
"""
Notion MCP ↔ 워크플로우 자동 연동.

PostToolUse 훅으로 호출되어:
  - notion-create-pages 결과에서 페이지 ID/제목 추출
  - workflow-state.json에 자동 등록 (idle → ticket)

환경변수:
  CLAUDE_TOOL_INPUT  — 도구 입력 JSON
  CLAUDE_TOOL_RESULT — 도구 결과 JSON
"""

import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

# closet-ecommerce 디렉토리에서만 실행
if "closet-ecommerce" not in str(Path.cwd()):
    sys.exit(0)

SCRIPT_DIR = Path(__file__).parent
WORKFLOW_STATE = SCRIPT_DIR / "workflow-state.json"


def load_workflow():
    if WORKFLOW_STATE.exists():
        with open(WORKFLOW_STATE, "r", encoding="utf-8") as f:
            return json.load(f)
    return None


def save_workflow(state):
    state["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(WORKFLOW_STATE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2, ensure_ascii=False)


def extract_page_info(tool_result_raw):
    """Notion 도구 결과에서 페이지 ID와 제목을 추출한다."""
    page_id = None
    title = None
    url = None

    try:
        result = json.loads(tool_result_raw) if isinstance(tool_result_raw, str) else tool_result_raw
    except (json.JSONDecodeError, TypeError):
        # JSON이 아니면 텍스트에서 추출 시도
        result = tool_result_raw or ""

    # 결과가 dict인 경우 (Notion API 응답)
    if isinstance(result, dict):
        page_id = result.get("id") or result.get("pageId")
        url = result.get("url")

        # properties에서 제목 추출
        props = result.get("properties", {})
        for key in ["Name", "Title", "이름", "제목", "title", "name"]:
            if key in props:
                prop = props[key]
                if isinstance(prop, dict):
                    # rich_text 또는 title 타입
                    for field in ["title", "rich_text"]:
                        items = prop.get(field, [])
                        if items and isinstance(items, list):
                            title = items[0].get("plain_text", "")
                            break
                break

        # content 필드에서 추출 시도
        if not title:
            content = result.get("content", "")
            if isinstance(content, str):
                lines = content.strip().split("\n")
                for line in lines:
                    clean = line.strip().lstrip("#").strip()
                    if clean:
                        title = clean[:100]
                        break

    # 결과가 문자열인 경우 텍스트에서 추출
    elif isinstance(result, str):
        text = result

        # 페이지 ID 추출 (UUID 패턴)
        id_match = re.search(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', text)
        if id_match:
            page_id = id_match.group()

        # Notion URL에서 ID 추출
        url_match = re.search(r'notion\.so/[^/]+/([a-f0-9]{32})', text)
        if url_match:
            raw_id = url_match.group(1)
            page_id = f"{raw_id[:8]}-{raw_id[8:12]}-{raw_id[12:16]}-{raw_id[16:20]}-{raw_id[20:]}"

        # 제목 추출 (첫 번째 의미 있는 텍스트)
        for line in text.split("\n"):
            clean = line.strip().lstrip("#").strip()
            if clean and len(clean) > 3 and not clean.startswith("{") and not clean.startswith("http"):
                title = clean[:100]
                break

    return page_id, title, url


def main():
    tool_result_raw = os.environ.get("CLAUDE_TOOL_RESULT", "")
    tool_input_raw = os.environ.get("CLAUDE_TOOL_INPUT", "")

    if not tool_result_raw:
        sys.exit(0)

    # 워크플로우 상태 확인
    state = load_workflow()
    if not state:
        sys.exit(0)

    # idle이 아니면 이미 작업 중 — 건드리지 않음
    if state.get("phase") != "idle":
        sys.exit(0)

    # 페이지 정보 추출
    page_id, title, url = extract_page_info(tool_result_raw)

    if not page_id and not title:
        sys.exit(0)

    # 워크플로우 자동 전환: idle → ticket
    ticket_id = page_id or "notion-auto"
    ticket_title = title or "Notion 티켓"

    state["phase"] = "ticket"
    state["ticket"] = {
        "id": ticket_id,
        "title": ticket_title,
        "url": url,
    }
    state["started_at"] = datetime.now(timezone.utc).isoformat()
    save_workflow(state)

    # 에이전트에게 피드백
    print(f"[워크플로우 자동 연동] 노션 티켓 등록 완료 → Phase: ticket")
    print(f"  ID: {ticket_id}")
    print(f"  제목: {ticket_title}")
    if url:
        print(f"  URL: {url}")
    print(f"  → 테스트 케이스부터 작성하세요.")


if __name__ == "__main__":
    main()
