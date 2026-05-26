"""
외부 MCP 클라이언트 데모 — Spring AI MCP 서버(의류 이커머스)에 SSE 로 접속해
도구를 조회·호출한다. 외부 LLM 이 이 클라이언트를 사용해 도구를 부르는 구조를 보여준다.

보안 PoC 활성 시 흐름:
  1) POST /auth/login 으로 JWT 발급 (clientId/clientSecret)
  2) Authorization: Bearer <JWT> 헤더로 /sse 연결 + tools/call
  3) catalog:read 스코프만 가진 클라이언트는 getOrderStatus 호출 시 403 을 받게 됨

실행:
    uv run --with mcp --with httpx spring-ai-practice/mcp-client-demo/client.py
환경변수 (선택):
    MCP_CLIENT_ID, MCP_CLIENT_SECRET (기본: shopper-llm / dev-secret-1)
    MCP_SERVER_BASE (기본: http://localhost:8080)
"""

import asyncio
import json
import os
import sys

import httpx
from mcp import ClientSession
from mcp.client.sse import sse_client

SERVER_BASE = os.environ.get("MCP_SERVER_BASE", "http://localhost:8080")
SSE_URL = f"{SERVER_BASE}/sse"
LOGIN_URL = f"{SERVER_BASE}/auth/login"
CLIENT_ID = os.environ.get("MCP_CLIENT_ID", "shopper-llm")
CLIENT_SECRET = os.environ.get("MCP_CLIENT_SECRET", "dev-secret-1")


def issue_token() -> dict:
    """POST /auth/login → JWT 발급"""
    body = {"clientId": CLIENT_ID, "clientSecret": CLIENT_SECRET}
    response = httpx.post(LOGIN_URL, json=body, timeout=5.0)
    if response.status_code != 200:
        print(f"✗ 토큰 발급 실패: HTTP {response.status_code} — {response.text}")
        sys.exit(1)
    return response.json()


def show_result(title: str, result) -> None:
    print(f"\n  ▶ {title}")
    if getattr(result, "isError", False):
        print(f"    ⚠ 오류: {result}")
        return
    for item in result.content:
        text = getattr(item, "text", None)
        if text is None:
            print(f"    {item}")
            continue
        try:
            parsed = json.loads(text)
            for line in json.dumps(parsed, ensure_ascii=False, indent=2).splitlines():
                print(f"    {line}")
        except (json.JSONDecodeError, TypeError):
            print(f"    {text}")


async def main() -> None:
    print(f"▶ 로그인: {LOGIN_URL} (clientId={CLIENT_ID})")
    token_info = issue_token()
    access_token = token_info["accessToken"]
    scopes = token_info.get("scopes", [])
    print(f"✓ JWT 발급 — scopes: {scopes}")
    print(f"  expiresAt: {token_info.get('expiresAt')}")

    auth_headers = {"Authorization": f"Bearer {access_token}"}

    print(f"\n▶ MCP 서버 SSE 접속: {SSE_URL}")
    async with sse_client(SSE_URL, headers=auth_headers) as (read, write):
        async with ClientSession(read, write) as session:
            # 1) initialize
            init = await session.initialize()
            info = init.serverInfo
            print(f"✓ initialize 완료 — 서버: {info.name} v{info.version}")
            print(f"  프로토콜 버전: {init.protocolVersion}")

            # 2) tools/list
            tools = await session.list_tools()
            print(f"\n✓ tools/list — 도구 {len(tools.tools)}개:")
            for tool in tools.tools:
                schema = tool.inputSchema or {}
                props = schema.get("properties", {})
                required = set(schema.get("required", []))
                params = ", ".join(
                    f"{name}: {info.get('type', '?')}" + ("*" if name in required else "")
                    for name, info in props.items()
                )
                print(f"  - {tool.name}({params})")
                print(f"      {tool.description}")

            # 3) tools/call — 시나리오
            print("\n" + "=" * 60)
            print("시나리오: 외부 LLM 이 사용자 질문을 받고 도구를 호출")
            print("=" * 60)
            print(
                '사용자 질문(한국어): "재고 있는 검은색 옷을 추천하고,\n'
                '                 그 중 첫 상품의 M 사이즈 재고도 확인해줘.\n'
                '                 그리고 주문 ORD-1001 배송상태도 알려줘."\n'
                "→ 외부 LLM 이 JWT 인증으로 도구 3종을 순차 호출합니다."
            )

            search = await session.call_tool("searchProducts", {"color": "블랙"})
            show_result("call_tool: searchProducts(color='블랙')", search)

            first_id = "P-1002"
            for item in search.content:
                txt = getattr(item, "text", None)
                if txt:
                    try:
                        arr = json.loads(txt)
                        if isinstance(arr, list) and arr:
                            first_id = arr[0].get("id", first_id)
                    except (json.JSONDecodeError, TypeError):
                        pass

            inv = await session.call_tool(
                "checkInventory",
                {"productId": first_id, "size": "M"},
            )
            show_result(f"call_tool: checkInventory(productId='{first_id}', size='M')", inv)

            order = await session.call_tool("getOrderStatus", {"orderId": "ORD-1001"})
            show_result("call_tool: getOrderStatus(orderId='ORD-1001')", order)

    print("\n✓ MCP 클라이언트 ↔ JWT 인증 ↔ 서버 ↔ 도구 실행 ↔ 결과 도출 완료")


if __name__ == "__main__":
    asyncio.run(main())
