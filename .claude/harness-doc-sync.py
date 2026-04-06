#!/usr/bin/env python3
"""
.md 파일 변경 시 노션 자동 동기화.

PostToolUse(Write|Edit) 훅에서 호출된다.
- docs/ 하위 .md 파일이 변경되면 대응하는 노션 페이지를 업데이트한다.
- 노션 페이지가 없으면 새로 생성한다.
- 매핑: docs/roadmap.md → 노션 "의류 이커머스 프로젝트" 하위 페이지

환경변수:
  NOTION_CLOSET_TOKEN — Closet 전용 노션 API 토큰
  NOTION_CLOSET_PAGE_ID — 메인 페이지 ID
  TOOL_INPUT — 훅에서 전달하는 tool input JSON (file_path 포함)

토큰/페이지 ID가 없으면 노션 동기화를 건너뛴다.
"""

import json
import os
import re
import sys
import urllib.request

TOKEN = os.environ.get("NOTION_CLOSET_TOKEN")
PARENT_PAGE_ID = os.environ.get("NOTION_CLOSET_PAGE_ID")
HEADERS = {
    "Authorization": f"Bearer {TOKEN}" if TOKEN else "",
    "Notion-Version": "2022-06-28",
    "Content-Type": "application/json",
}

# docs 경로 → 노션 페이지 제목 매핑
DOC_MAP = {
    "docs/roadmap.md": "로드맵",
    "docs/ddd-architecture-review.md": "DDD 아키텍처 리뷰",
    "docs/adr/ADR-001-k8s-scheduler-migration.md": "ADR-001 K8s 스케줄러 마이그레이션",
    "docs/prd/phase5-subscription.md": "PRD Phase 5 의류 구독/대여",
}

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLOSET_ROOT = os.path.join(PROJECT_ROOT, "closet-ecommerce")


def api(method, url, data=None):
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=HEADERS, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError:
        return None


def find_or_create_page(title):
    """노션에서 제목으로 페이지 검색, 없으면 생성."""
    # 부모 페이지 하위에서 검색
    result = api("GET", f"https://api.notion.com/v1/blocks/{PARENT_PAGE_ID}/children?page_size=100")
    if result:
        for block in result.get("results", []):
            if block.get("type") == "child_page" and block["child_page"]["title"] == title:
                return block["id"]

    # 없으면 생성
    new_page = api("POST", "https://api.notion.com/v1/pages", {
        "parent": {"page_id": PARENT_PAGE_ID},
        "properties": {"title": [{"text": {"content": title}}]},
    })
    if new_page:
        return new_page["id"]
    return None


def md_to_blocks(md_content):
    """마크다운을 노션 블록 배열로 변환 (간이 파서)."""
    blocks = []
    lines = md_content.split("\n")
    i = 0
    in_code = False
    code_lines = []
    code_lang = ""

    while i < len(lines):
        line = lines[i]

        # 코드 블록 시작/종료
        if line.strip().startswith("```"):
            if in_code:
                # 코드 블록 종료
                code_text = "\n".join(code_lines)
                # Notion code block 최대 2000자
                if len(code_text) > 1900:
                    code_text = code_text[:1900] + "\n... (truncated)"
                blocks.append({
                    "object": "block", "type": "code",
                    "code": {"rich_text": [{"text": {"content": code_text}}], "language": code_lang or "plain text"}
                })
                code_lines = []
                in_code = False
            else:
                # 코드 블록 시작
                code_lang = line.strip().lstrip("`").strip() or "plain text"
                lang_map = {"sql": "sql", "kotlin": "kotlin", "json": "json", "yaml": "yaml",
                            "bash": "bash", "mermaid": "mermaid", "python": "python"}
                code_lang = lang_map.get(code_lang.lower(), "plain text")
                in_code = True
            i += 1
            continue

        if in_code:
            code_lines.append(line)
            i += 1
            continue

        stripped = line.strip()

        # 빈 줄 스킵
        if not stripped:
            i += 1
            continue

        # 제목
        if stripped.startswith("# ") and not stripped.startswith("## "):
            # H1은 페이지 제목이므로 스킵
            i += 1
            continue
        elif stripped.startswith("## "):
            blocks.append({
                "object": "block", "type": "heading_2",
                "heading_2": {"rich_text": [{"text": {"content": stripped[3:].strip()}}]}
            })
        elif stripped.startswith("### "):
            blocks.append({
                "object": "block", "type": "heading_3",
                "heading_3": {"rich_text": [{"text": {"content": stripped[4:].strip()}}]}
            })
        # 구분선
        elif stripped == "---":
            blocks.append({"object": "block", "type": "divider", "divider": {}})
        # 불릿
        elif stripped.startswith("- ") or stripped.startswith("* "):
            text = stripped[2:].strip()
            blocks.append({
                "object": "block", "type": "bulleted_list_item",
                "bulleted_list_item": {"rich_text": [{"text": {"content": text[:2000]}}]}
            })
        # 번호 목록
        elif re.match(r"^\d+\.\s", stripped):
            text = re.sub(r"^\d+\.\s", "", stripped).strip()
            blocks.append({
                "object": "block", "type": "numbered_list_item",
                "numbered_list_item": {"rich_text": [{"text": {"content": text[:2000]}}]}
            })
        # 인용
        elif stripped.startswith("> "):
            blocks.append({
                "object": "block", "type": "quote",
                "quote": {"rich_text": [{"text": {"content": stripped[2:].strip()[:2000]}}]}
            })
        # 테이블 (간이 — 첫 줄만 파싱)
        elif stripped.startswith("|") and stripped.endswith("|"):
            # 테이블 전체를 텍스트 블록으로 변환
            table_lines = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_lines.append(lines[i].strip())
                i += 1
            table_text = "\n".join(table_lines)
            if len(table_text) > 1900:
                table_text = table_text[:1900] + "\n..."
            blocks.append({
                "object": "block", "type": "code",
                "code": {"rich_text": [{"text": {"content": table_text}}], "language": "plain text"}
            })
            continue
        # 일반 텍스트
        else:
            blocks.append({
                "object": "block", "type": "paragraph",
                "paragraph": {"rich_text": [{"text": {"content": stripped[:2000]}}]}
            })

        i += 1

    return blocks


def sync_doc(file_path):
    """파일 경로로 노션 동기화."""
    if not TOKEN or not PARENT_PAGE_ID:
        print("[doc-sync] NOTION_CLOSET_TOKEN/NOTION_CLOSET_PAGE_ID 미설정, 동기화 건너뜀", file=sys.stderr)
        return

    # closet-ecommerce/ 상대 경로 추출
    rel_path = None
    if "closet-ecommerce/" in file_path:
        rel_path = file_path.split("closet-ecommerce/")[-1]
    elif file_path.startswith(CLOSET_ROOT):
        rel_path = os.path.relpath(file_path, CLOSET_ROOT)

    if not rel_path or not rel_path.endswith(".md"):
        return

    # 매핑된 문서인지 확인
    title = DOC_MAP.get(rel_path)
    if not title:
        # 매핑에 없으면 파일명에서 제목 생성
        basename = os.path.splitext(os.path.basename(rel_path))[0]
        title = basename.replace("-", " ").replace("_", " ").title()

    # 파일 읽기
    full_path = os.path.join(CLOSET_ROOT, rel_path) if not os.path.isabs(file_path) else file_path
    if not os.path.exists(full_path):
        return

    with open(full_path, encoding="utf-8") as f:
        content = f.read()

    # 노션 페이지 찾기/생성
    page_id = find_or_create_page(title)
    if not page_id:
        print(f"[doc-sync] 노션 페이지 생성 실패: {title}", file=sys.stderr)
        return

    # 기존 블록 삭제
    existing = api("GET", f"https://api.notion.com/v1/blocks/{page_id}/children?page_size=100")
    if existing:
        for block in existing.get("results", []):
            api("DELETE", f"https://api.notion.com/v1/blocks/{block['id']}")

    # 새 블록 추가
    blocks = md_to_blocks(content)
    for i in range(0, len(blocks), 100):
        chunk = blocks[i:i + 100]
        api("PATCH", f"https://api.notion.com/v1/blocks/{page_id}/children", {"children": chunk})

    print(f"[doc-sync] 노션 동기화 완료: {rel_path} → {title} ({len(blocks)} blocks)")


def main():
    """훅에서 호출 시 TOOL_INPUT에서 file_path 추출."""
    tool_input = os.environ.get("TOOL_INPUT", "")

    if tool_input:
        try:
            data = json.loads(tool_input)
            file_path = data.get("file_path", "")
        except (json.JSONDecodeError, TypeError):
            file_path = tool_input
    else:
        # stdin에서 읽기 시도
        if not sys.stdin.isatty():
            try:
                data = json.loads(sys.stdin.read())
                file_path = data.get("file_path", "")
            except (json.JSONDecodeError, TypeError):
                return
        else:
            return

    if file_path and file_path.endswith(".md") and "docs/" in file_path:
        sync_doc(file_path)


if __name__ == "__main__":
    main()
