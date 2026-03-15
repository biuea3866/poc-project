from __future__ import annotations

from base64 import b64encode
from dataclasses import dataclass
import html
import json
from pathlib import Path
from typing import Any
from urllib import error, parse, request

from ai_orchestrator_lab.config import RuntimeConfig


class AtlassianApiError(RuntimeError):
    pass


@dataclass(frozen=True)
class JiraIssuePayload:
    summary: str
    description: str
    issue_type: str
    project_key: str


import re as _re


# ── Confluence Storage Format helpers ──────────────────────────────────────

def _inline_to_storage(text: str) -> str:
    """인라인 마크다운을 Confluence Storage Format XML로 변환."""
    # inline code (backtick) — before bold/italic to avoid collision
    text = _re.sub(r"`([^`]+)`", lambda m: f"<code>{html.escape(m.group(1))}</code>", text)
    # bold+italic
    text = _re.sub(r"\*\*\*(.+?)\*\*\*", lambda m: f"<strong><em>{html.escape(m.group(1))}</em></strong>", text)
    # bold
    text = _re.sub(r"\*\*(.+?)\*\*", lambda m: f"<strong>{html.escape(m.group(1))}</strong>", text)
    # italic
    text = _re.sub(r"\*(.+?)\*", lambda m: f"<em>{html.escape(m.group(1))}</em>", text)
    # strikethrough
    text = _re.sub(r"~~(.+?)~~", lambda m: f"<s>{html.escape(m.group(1))}</s>", text)
    # links [text](url)
    text = _re.sub(
        r"\[([^\]]+)\]\(([^)]+)\)",
        lambda m: f'<a href="{html.escape(m.group(2))}">{html.escape(m.group(1))}</a>',
        text,
    )
    return text


def _table_to_storage(lines: list[str]) -> str:
    """마크다운 테이블을 Confluence Storage Format 테이블로 변환."""
    rows: list[list[str]] = []
    is_header_row: list[bool] = []
    for line in lines:
        if _re.match(r"^\s*\|[-:| ]+\|\s*$", line):
            continue  # separator row
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        rows.append(cells)
        is_header_row.append(not is_header_row)  # first row = header

    out = ["<table><tbody>"]
    for i, row in enumerate(rows):
        out.append("<tr>")
        tag = "th" if i == 0 else "td"
        for cell in row:
            out.append(f"<{tag}><p>{_inline_to_storage(cell)}</p></{tag}>")
        out.append("</tr>")
    out.append("</tbody></table>")
    return "".join(out)


def _markdown_to_storage(markdown_text: str) -> str:
    """마크다운을 Confluence Storage Format XML로 변환."""
    lines = markdown_text.splitlines()
    blocks: list[str] = []
    i = 0

    def flush_list(items: list[tuple[str, bool]]) -> None:
        """(text, ordered) 쌍 목록을 ul/ol 태그로 추가."""
        if not items:
            return
        ordered = items[0][1]
        tag = "ol" if ordered else "ul"
        blocks.append(f"<{tag}>")
        for text, _ in items:
            blocks.append(f"<li><p>{_inline_to_storage(text)}</p></li>")
        blocks.append(f"</{tag}>")

    list_items: list[tuple[str, bool]] = []

    while i < len(lines):
        raw = lines[i].rstrip()

        # ── 코드 블록 ──
        if raw.startswith("```"):
            if list_items:
                flush_list(list_items)
                list_items = []
            lang = html.escape(raw[3:].strip()) or "none"
            code_lines: list[str] = []
            i += 1
            while i < len(lines) and not lines[i].rstrip().startswith("```"):
                code_lines.append(lines[i])
                i += 1
            code_body = "\n".join(code_lines)
            # draw.io excalidraw 펜스 → draw.io Confluence 매크로
            if lang in ("drawio", "excalidraw"):
                blocks.append(
                    '<ac:structured-macro ac:name="drawio">'
                    '<ac:parameter ac:name="simple">true</ac:parameter>'
                    f'<ac:plain-text-body><![CDATA[{code_body}]]></ac:plain-text-body>'
                    "</ac:structured-macro>"
                )
            else:
                blocks.append(
                    f'<ac:structured-macro ac:name="code">'
                    f'<ac:parameter ac:name="language">{lang}</ac:parameter>'
                    f"<ac:plain-text-body><![CDATA[{code_body}]]></ac:plain-text-body>"
                    f"</ac:structured-macro>"
                )
            i += 1
            continue

        # ── 수평선 ──
        if _re.match(r"^[-*_]{3,}\s*$", raw):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append("<hr/>")
            i += 1
            continue

        # ── 테이블 ──
        if raw.startswith("|") and raw.endswith("|"):
            if list_items:
                flush_list(list_items)
                list_items = []
            table_lines: list[str] = []
            while i < len(lines) and lines[i].rstrip().startswith("|"):
                table_lines.append(lines[i].rstrip())
                i += 1
            blocks.append(_table_to_storage(table_lines))
            continue

        # ── 제목 ──
        if raw.startswith("#### "):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append(f"<h4>{_inline_to_storage(raw[5:])}</h4>")
            i += 1
            continue
        if raw.startswith("### "):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append(f"<h3>{_inline_to_storage(raw[4:])}</h3>")
            i += 1
            continue
        if raw.startswith("## "):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append(f"<h2>{_inline_to_storage(raw[3:])}</h2>")
            i += 1
            continue
        if raw.startswith("# "):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append(f"<h1>{_inline_to_storage(raw[2:])}</h1>")
            i += 1
            continue

        # ── 인용 ──
        if raw.startswith("> "):
            if list_items:
                flush_list(list_items)
                list_items = []
            blocks.append(f"<blockquote><p>{_inline_to_storage(raw[2:])}</p></blockquote>")
            i += 1
            continue

        # ── 순서 있는 목록 ──
        ol_m = _re.match(r"^\d+\.\s+(.*)", raw)
        if ol_m:
            if list_items and not list_items[-1][1]:  # 이전이 ul이면 flush
                flush_list(list_items)
                list_items = []
            list_items.append((ol_m.group(1), True))
            i += 1
            continue

        # ── 순서 없는 목록 ──
        if raw.startswith("- ") or raw.startswith("* "):
            if list_items and list_items[-1][1]:  # 이전이 ol이면 flush
                flush_list(list_items)
                list_items = []
            list_items.append((raw[2:], False))
            i += 1
            continue

        # ── 빈 줄 ──
        if not raw:
            if list_items:
                flush_list(list_items)
                list_items = []
            i += 1
            continue

        # ── 일반 단락 ──
        if list_items:
            flush_list(list_items)
            list_items = []
        blocks.append(f"<p>{_inline_to_storage(raw)}</p>")
        i += 1

    if list_items:
        flush_list(list_items)

    return "".join(blocks) or "<p>Empty document</p>"


# ── Jira ADF helpers ────────────────────────────────────────────────────────

def _inline_to_adf_content(text: str) -> list[dict[str, Any]]:
    """인라인 마크다운을 ADF inline content 노드 목록으로 변환."""
    nodes: list[dict[str, Any]] = []
    pattern = _re.compile(
        r"(`[^`]+`)"           # inline code
        r"|(\*\*\*[^*]+\*\*\*)"  # bold+italic
        r"|(\*\*[^*]+\*\*)"    # bold
        r"|(\*[^*]+\*)"        # italic
        r"|(\[([^\]]+)\]\(([^)]+)\))"  # link
    )
    pos = 0
    for m in pattern.finditer(text):
        if m.start() > pos:
            nodes.append({"type": "text", "text": text[pos:m.start()]})
        raw = m.group(0)
        if raw.startswith("`"):
            nodes.append({"type": "text", "text": raw[1:-1], "marks": [{"type": "code"}]})
        elif raw.startswith("***"):
            nodes.append({"type": "text", "text": raw[3:-3], "marks": [{"type": "strong"}, {"type": "em"}]})
        elif raw.startswith("**"):
            nodes.append({"type": "text", "text": raw[2:-2], "marks": [{"type": "strong"}]})
        elif raw.startswith("*"):
            nodes.append({"type": "text", "text": raw[1:-1], "marks": [{"type": "em"}]})
        elif raw.startswith("["):
            url = m.group(7)
            label = m.group(6)
            nodes.append({"type": "text", "text": label, "marks": [{"type": "link", "attrs": {"href": url}}]})
        pos = m.end()
    if pos < len(text):
        nodes.append({"type": "text", "text": text[pos:]})
    return nodes or [{"type": "text", "text": text}]


def _markdown_to_adf(markdown_text: str) -> dict[str, Any]:
    """마크다운을 Jira ADF(Atlassian Document Format)로 변환."""
    content: list[dict[str, Any]] = []
    lines = markdown_text.splitlines()
    i = 0
    bullet_items: list[dict[str, Any]] = []
    ordered_items: list[dict[str, Any]] = []

    def flush_bullets() -> None:
        if bullet_items:
            content.append({"type": "bulletList", "content": list(bullet_items)})
            bullet_items.clear()

    def flush_ordered() -> None:
        if ordered_items:
            content.append({"type": "orderedList", "content": list(ordered_items)})
            ordered_items.clear()

    def make_list_item(text: str) -> dict[str, Any]:
        return {
            "type": "listItem",
            "content": [{"type": "paragraph", "content": _inline_to_adf_content(text)}],
        }

    while i < len(lines):
        raw = lines[i].rstrip()

        # ── 코드 블록 ──
        if raw.startswith("```"):
            flush_bullets(); flush_ordered()
            lang = raw[3:].strip() or "plain"
            code_lines: list[str] = []
            i += 1
            while i < len(lines) and not lines[i].rstrip().startswith("```"):
                code_lines.append(lines[i])
                i += 1
            content.append({
                "type": "codeBlock",
                "attrs": {"language": lang},
                "content": [{"type": "text", "text": "\n".join(code_lines)}],
            })
            i += 1
            continue

        # ── 제목 ──
        for prefix, level in (("#### ", 4), ("### ", 3), ("## ", 2), ("# ", 1)):
            if raw.startswith(prefix):
                flush_bullets(); flush_ordered()
                content.append({
                    "type": "heading",
                    "attrs": {"level": level},
                    "content": _inline_to_adf_content(raw[len(prefix):]),
                })
                i += 1
                break
        else:
            # ── 순서 없는 목록 ──
            if raw.startswith("- ") or raw.startswith("* "):
                flush_ordered()
                bullet_items.append(make_list_item(raw[2:]))
                i += 1
                continue

            # ── 순서 있는 목록 ──
            ol_m = _re.match(r"^\d+\.\s+(.*)", raw)
            if ol_m:
                flush_bullets()
                ordered_items.append(make_list_item(ol_m.group(1)))
                i += 1
                continue

            # ── 빈 줄 ──
            if not raw:
                flush_bullets(); flush_ordered()
                i += 1
                continue

            # ── 수평선 ──
            if _re.match(r"^[-*_]{3,}\s*$", raw):
                flush_bullets(); flush_ordered()
                content.append({"type": "rule"})
                i += 1
                continue

            # ── 일반 단락 ──
            flush_bullets(); flush_ordered()
            content.append({
                "type": "paragraph",
                "content": _inline_to_adf_content(raw),
            })
            i += 1
            continue

        continue

    flush_bullets()
    flush_ordered()
    return {"type": "doc", "version": 1, "content": content}


class AtlassianClient:
    def __init__(self, config: RuntimeConfig):
        if not config.atlassian_email or not config.atlassian_api_token:
            raise AtlassianApiError(
                "Missing Atlassian credentials. Set AI_ORCHESTRATOR_ATLASSIAN_EMAIL and "
                "AI_ORCHESTRATOR_ATLASSIAN_API_TOKEN."
            )
        self._config = config
        auth = f"{config.atlassian_email}:{config.atlassian_api_token}".encode("utf-8")
        self._auth_header = f"Basic {b64encode(auth).decode('utf-8')}"
        self._confluence_site = config.confluence_site_name
        self._jira_site = config.jira_site_name
        if not self._confluence_site and not self._jira_site:
            raise AtlassianApiError("Missing Atlassian site configuration.")

    def _request_json(
        self,
        method: str,
        url: str,
        payload: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        body = None
        headers = {
            "Authorization": self._auth_header,
            "Accept": "application/json",
        }
        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = request.Request(url, data=body, method=method, headers=headers)
        try:
            with request.urlopen(req) as response:
                raw = response.read().decode("utf-8")
                return json.loads(raw) if raw else {}
        except error.HTTPError as exc:  # pragma: no cover
            details = exc.read().decode("utf-8", errors="replace")
            raise AtlassianApiError(f"{method} {url} failed: {exc.code} {details}") from exc
        except error.URLError as exc:  # pragma: no cover
            raise AtlassianApiError(f"{method} {url} failed: {exc.reason}") from exc

    def _confluence_url(self, path: str, query: dict[str, str] | None = None) -> str:
        if not self._confluence_site:
            raise AtlassianApiError("Missing Confluence site configuration.")
        base = f"https://{self._confluence_site}{path}"
        if not query:
            return base
        return f"{base}?{parse.urlencode(query)}"

    def _jira_url(self, path: str) -> str:
        if not self._jira_site:
            raise AtlassianApiError("Missing Jira site configuration.")
        return f"https://{self._jira_site}{path}"

    def get_space_id(self, space_key: str) -> str:
        result = self._request_json(
            "GET",
            self._confluence_url("/wiki/api/v2/spaces", {"keys": space_key}),
        )
        spaces = result.get("results", [])
        if not spaces:
            raise AtlassianApiError(f"Confluence space not found: {space_key}")
        return str(spaces[0]["id"])

    def get_page(self, page_id: str) -> dict[str, Any]:
        return self._request_json(
            "GET",
            self._confluence_url(
                f"/wiki/api/v2/pages/{page_id}",
                {"body-format": "storage"},
            ),
        )

    def update_page(self, page_id: str, title: str, markdown_body: str) -> dict[str, Any]:
        page = self.get_page(page_id)
        version = int(page["version"]["number"]) + 1
        payload = {
            "id": page_id,
            "status": "current",
            "title": title,
            "spaceId": page["spaceId"],
            "body": {
                "representation": "storage",
                "value": _markdown_to_storage(markdown_body),
            },
            "version": {"number": version},
        }
        return self._request_json("PUT", self._confluence_url(f"/wiki/api/v2/pages/{page_id}"), payload)

    def create_page(
        self,
        space_key: str,
        title: str,
        markdown_body: str,
        parent_page_id: str | None = None,
    ) -> dict[str, Any]:
        payload: dict[str, Any] = {
            "spaceId": self.get_space_id(space_key),
            "status": "current",
            "title": title,
            "body": {
                "representation": "storage",
                "value": _markdown_to_storage(markdown_body),
            },
        }
        if parent_page_id:
            payload["parentId"] = parent_page_id
        return self._request_json("POST", self._confluence_url("/wiki/api/v2/pages"), payload)

    def create_jira_issue(self, issue: JiraIssuePayload) -> dict[str, Any]:
        payload = {
            "fields": {
                "project": {"key": issue.project_key},
                "summary": issue.summary,
                "issuetype": {"name": issue.issue_type},
                "description": _markdown_to_adf(issue.description),
            }
        }
        return self._request_json("POST", self._jira_url("/rest/api/3/issue"), payload)

    def update_jira_issue(self, issue_key: str, summary: str, description: str) -> dict[str, Any]:
        payload = {
            "fields": {
                "summary": summary,
                "description": _markdown_to_adf(description),
            }
        }
        return self._request_json("PUT", self._jira_url(f"/rest/api/3/issue/{issue_key}"), payload)

    def get_jira_transitions(self, issue_key: str) -> list[dict[str, Any]]:
        result = self._request_json("GET", self._jira_url(f"/rest/api/3/issue/{issue_key}/transitions"))
        return result.get("transitions", [])

    def transition_jira_issue(self, issue_key: str, transition_id: str) -> dict[str, Any]:
        return self._request_json(
            "POST",
            self._jira_url(f"/rest/api/3/issue/{issue_key}/transitions"),
            {"transition": {"id": transition_id}},
        )

    def transition_jira_issue_by_name(self, issue_key: str, transition_name: str) -> dict[str, Any]:
        transitions = self.get_jira_transitions(issue_key)
        for transition in transitions:
            if transition.get("name") == transition_name:
                result = self.transition_jira_issue(issue_key, transition["id"])
                return {
                    "issue_key": issue_key,
                    "transition_id": transition["id"],
                    "transition_name": transition_name,
                    "result": result,
                }
        available = ", ".join(t.get("name", "") for t in transitions)
        raise AtlassianApiError(
            f"Transition not found for {issue_key}: {transition_name}. Available: {available}"
        )

    @staticmethod
    def load_markdown(path: Path) -> str:
        return path.read_text(encoding="utf-8")
