# mcp-external-server

**독립 실행 MCP 서버**. Spring AI 앱(`../spring-ai-practice`) 이 *클라이언트* 로 접속해 도구를 끌어다 쓰는 외부 도구 공급자.

저장소 안에서 별도 프로젝트 디렉토리로 분리되어 있어, 자체 빌드(Dockerfile / pyproject.toml) 와 자체 의존성을 가진다. Spring 앱과 독립적으로 실행·재시작 가능.

## 노출 도구

| 도구 | 인자 | 설명 |
|---|---|---|
| `convert_krw` | `amount_krw: int`, `target_currency: USD/JPY/EUR/CNY` | 원화 금액을 다른 통화로 환산 |
| `season_tip` | `month: 1~12` | 월별 시즌 추천 의류 카테고리 |

## 실행 방식

### 방법 1 — docker-compose (권장)

루트 디렉토리의 [`docker-compose.yml`](../docker-compose.yml) 이 본 서버와 Ollama 를 함께 띄운다.

```bash
cd /Users/biuea/feature/flag_project
docker compose up -d mcp-external-server ollama
docker compose ps
```

### 방법 2 — 단독 Docker 빌드

```bash
cd mcp-external-server
docker build -t mcp-external-server .
docker run -d --rm -p 9090:9090 --name mcp-external mcp-external-server
```

### 방법 3 — 로컬 uv (Docker 없이)

```bash
cd mcp-external-server
MCP_HOST=0.0.0.0 MCP_PORT=9090 uv run --with mcp python server.py
```

## Spring 앱 연동

`spring-ai-practice` 의 환경변수:

```bash
EXTERNAL_MCP_ENABLED=true \
EXTERNAL_MCP_URL=http://localhost:9090 \
  ./gradlew bootRun
```

Spring 앱 부팅 로그에 다음이 보이면 정상:
```
INFO McpClientConfig : External MCP server 연결 완료: http://localhost:9090
INFO McpClientConfig : External MCP server 도구 2개 노출됨
INFO McpServerAutoConfiguration : Registered tools: 16
```

내부 14개 + 외부 2개 = 16. Spring 앱의 `/sse` MCP 서버가 외부 도구까지 **gateway** 처럼 함께 노출.

## 검증

```bash
# 1) 서버 단독으로 SSE 응답 확인
curl -s -o /dev/null -w "HTTP %{http_code}, %{content_type}\n" -m 2 http://localhost:9090/sse
# → HTTP 200, text/event-stream

# 2) Python MCP 클라이언트로 도구 listing
uv run --with mcp python <<'EOF'
import asyncio
from mcp import ClientSession
from mcp.client.sse import sse_client

async def main():
    async with sse_client("http://localhost:9090/sse") as (r, w):
        async with ClientSession(r, w) as s:
            await s.initialize()
            for t in (await s.list_tools()).tools:
                print(f"- {t.name}: {t.description[:60]}")
asyncio.run(main())
EOF
```

기대 출력:
```
- convert_krw: 한국 원화 금액을 다른 통화로 환산한다. ...
- season_tip: 1~12 월을 입력받아 해당 시즌에 어울리는 의류 ...
```

## 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `MCP_HOST` | `0.0.0.0` | 바인딩 호스트 (Docker 안에서는 0.0.0.0 필수) |
| `MCP_PORT` | `9090` | 바인딩 포트 |

## 파일 구성

```
mcp-external-server/
├── server.py          # FastMCP 서버 본체
├── pyproject.toml     # Python 프로젝트 메타데이터
├── Dockerfile         # 컨테이너 이미지 (python:3.12-slim 기반)
├── .dockerignore
└── README.md
```
