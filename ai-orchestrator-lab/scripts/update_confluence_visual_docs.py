from __future__ import annotations

import html
import json
import re
import subprocess
from base64 import b64encode
from pathlib import Path
from urllib import request

from ai_orchestrator_lab.config import RuntimeConfig


IMAGE_PATTERN = re.compile(r"!\[(.*?)\]\((.*?)\)")


def to_storage(markdown_text: str) -> str:
    blocks: list[str] = []
    in_list = False

    for raw in markdown_text.splitlines():
        line = raw.rstrip()
        if not line:
            if in_list:
                blocks.append("</ul>")
                in_list = False
            continue

        match = IMAGE_PATTERN.fullmatch(line.strip())
        if match:
            if in_list:
                blocks.append("</ul>")
                in_list = False
            _alt, path = match.groups()
            filename = Path(path).name
            blocks.append(
                '<p><ac:image ac:layout="center" ac:width="900">'
                f'<ri:attachment ri:filename="{html.escape(filename)}" />'
                "</ac:image></p>"
            )
            continue

        if line.startswith("### "):
            if in_list:
                blocks.append("</ul>")
                in_list = False
            blocks.append(f"<h3>{html.escape(line[4:])}</h3>")
            continue
        if line.startswith("## "):
            if in_list:
                blocks.append("</ul>")
                in_list = False
            blocks.append(f"<h2>{html.escape(line[3:])}</h2>")
            continue
        if line.startswith("# "):
            if in_list:
                blocks.append("</ul>")
                in_list = False
            blocks.append(f"<h1>{html.escape(line[2:])}</h1>")
            continue
        if line.startswith("- "):
            if not in_list:
                blocks.append("<ul>")
                in_list = True
            blocks.append(f"<li>{html.escape(line[2:])}</li>")
            continue
        if re.match(r"\d+\. ", line):
            if in_list:
                blocks.append("</ul>")
                in_list = False
            text = re.sub(r"^\d+\.\s+", "", line)
            blocks.append(f"<p>{html.escape(text)}</p>")
            continue

        if in_list:
            blocks.append("</ul>")
            in_list = False
        blocks.append(f"<p>{html.escape(line)}</p>")

    if in_list:
        blocks.append("</ul>")

    return "".join(blocks)


def make_headers(email: str, token: str) -> dict[str, str]:
    auth = b64encode(f"{email}:{token}".encode()).decode()
    return {
        "Authorization": f"Basic {auth}",
        "Accept": "application/json",
        "Content-Type": "application/json",
    }


def get_page(site: str, headers: dict[str, str], page_id: str) -> dict:
    url = f"https://{site}/wiki/api/v2/pages/{page_id}?body-format=storage"
    req = request.Request(url, headers=headers)
    with request.urlopen(req) as resp:
        return json.loads(resp.read().decode())


def update_page(
    site: str,
    headers: dict[str, str],
    page_id: str,
    title: str,
    storage_body: str,
) -> None:
    page = get_page(site, headers, page_id)
    payload = {
        "id": page_id,
        "status": "current",
        "title": title,
        "spaceId": page["spaceId"],
        "body": {
            "representation": "storage",
            "value": storage_body,
        },
        "version": {"number": int(page["version"]["number"]) + 1},
    }
    req = request.Request(
        f"https://{site}/wiki/api/v2/pages/{page_id}",
        data=json.dumps(payload).encode(),
        method="PUT",
        headers=headers,
    )
    with request.urlopen(req) as resp:
        json.loads(resp.read().decode())


def upload_attachment(site: str, email: str, token: str, page_id: str, file_path: Path) -> None:
    subprocess.run(
        [
            "curl",
            "-sS",
            "-u",
            f"{email}:{token}",
            "-H",
            "X-Atlassian-Token: no-check",
            "-F",
            f"file=@{file_path}",
            f"https://{site}/wiki/rest/api/content/{page_id}/child/attachment",
        ],
        check=True,
        stdout=subprocess.DEVNULL,
    )


def main() -> None:
    project_root = Path(__file__).resolve().parents[1]
    config = RuntimeConfig.from_env(project_root)
    if not config.confluence_site_name or not config.atlassian_email or not config.atlassian_api_token:
        raise RuntimeError("Missing Confluence credentials or site configuration.")

    site = config.confluence_site_name
    email = config.atlassian_email
    token = config.atlassian_api_token
    headers = make_headers(email, token)

    pages = [
        {
            "page_id": "622611",
            "title": "Pinpoint 모니터링 부트스트랩 2026-03-13",
            "markdown": project_root / "plans/2026-03-13-pinpoint-tech-doc.md",
            "attachments": [
                project_root / "docs/pinpoint-data-flow.svg",
                project_root / "docs/pinpoint-erd.svg",
                project_root / "docs/pinpoint-component-diagram.svg",
                project_root / "docs/pinpoint-flow-diagram.svg",
            ],
        },
        {
            "page_id": "720897",
            "title": "AI Wiki 문서 작성 및 분석 상태 화면 설계 2026-03-14",
            "markdown": project_root / "plans/2026-03-14-fe-product-tech-doc.md",
            "attachments": [
                project_root / "docs/ai-wiki-fe-data-flow.svg",
                project_root / "docs/ai-wiki-fe-erd.svg",
                project_root / "docs/ai-wiki-fe-component-diagram.svg",
                project_root / "docs/ai-wiki-fe-flow-diagram.svg",
            ],
        },
        {
            "page_id": "720917",
            "title": "AI Wiki 문서 및 분석 API 설계 2026-03-14",
            "markdown": project_root / "plans/2026-03-14-be-product-tech-doc.md",
            "attachments": [
                project_root / "docs/ai-wiki-be-data-flow.svg",
                project_root / "docs/ai-wiki-be-erd.svg",
                project_root / "docs/ai-wiki-be-component-diagram.svg",
                project_root / "docs/ai-wiki-be-flow-diagram.svg",
            ],
        },
    ]

    for page in pages:
        for attachment in page["attachments"]:
            upload_attachment(site, email, token, page["page_id"], attachment)
        storage_body = to_storage(page["markdown"].read_text(encoding="utf-8"))
        update_page(site, headers, page["page_id"], page["title"], storage_body)
        print(f"updated {page['page_id']} {page['title']}")


if __name__ == "__main__":
    main()
