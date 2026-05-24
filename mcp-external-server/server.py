"""
외부 MCP 서버 — Spring AI 앱이 *클라이언트*로 접속해 도구를 끌어다 쓰는 독립 서비스.

도구 2종:
  - convert_krw: 한국 원화를 다른 통화로 변환
  - season_tip: 월(1~12) 입력 → 추천 의류 카테고리

실행 방식 (3가지 중 택일):
  1) docker-compose 로 함께:
       docker compose up mcp-external-server
  2) 로컬 uv:
       uv run --with mcp python server.py
  3) 호스트 바인딩 변경 (외부 접속 허용):
       MCP_HOST=0.0.0.0 MCP_PORT=9090 uv run --with mcp python server.py

환경변수:
  MCP_HOST (기본: 0.0.0.0) — Docker 안에서 외부 노출 위해 0.0.0.0
  MCP_PORT (기본: 9090)
"""

import os
from datetime import datetime

from mcp.server.fastmcp import FastMCP

HOST = os.environ.get("MCP_HOST", "0.0.0.0")
PORT = int(os.environ.get("MCP_PORT", "9090"))

mcp = FastMCP("clothing-external-mcp", host=HOST, port=PORT)

# 단순 PoC 환율 (실제로는 외부 API 호출). 2026-05 기준 대략값.
KRW_PER_UNIT = {
    "USD": 1380.0,
    "JPY": 9.0,
    "EUR": 1480.0,
    "CNY": 190.0,
}

SEASON_TIPS = {
    "spring": ["트렌치 코트", "얇은 니트", "코튼 셔츠"],
    "summer": ["반팔 티셔츠", "린넨 셔츠", "와이드 슬랙스"],
    "autumn": ["헤비웨이트 후드", "자켓", "면 청바지"],
    "winter": ["구스다운 패딩", "울 코트", "터틀넥 니트"],
}


@mcp.tool()
def convert_krw(amount_krw: int, target_currency: str) -> dict:
    """한국 원화 금액을 다른 통화로 환산한다. target_currency 는 USD/JPY/EUR/CNY 중 하나."""
    code = target_currency.upper()
    rate = KRW_PER_UNIT.get(code)
    if rate is None:
        return {"error": f"지원하지 않는 통화: {target_currency}", "supported": list(KRW_PER_UNIT.keys())}
    return {
        "amountKrw": amount_krw,
        "targetCurrency": code,
        "converted": round(amount_krw / rate, 2),
        "rate": rate,
        "asOf": datetime.utcnow().date().isoformat(),
    }


@mcp.tool()
def season_tip(month: int) -> dict:
    """1~12 월을 입력받아 해당 시즌에 어울리는 의류 카테고리 추천을 반환한다."""
    if month not in range(1, 13):
        return {"error": "month 는 1~12 사이여야 합니다."}
    if month in (3, 4, 5):
        season = "spring"
    elif month in (6, 7, 8):
        season = "summer"
    elif month in (9, 10, 11):
        season = "autumn"
    else:
        season = "winter"
    return {"month": month, "season": season, "recommended": SEASON_TIPS[season]}


if __name__ == "__main__":
    mcp.run(transport="sse")
