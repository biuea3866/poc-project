# mcp-client-demo

Spring AI MCP 서버에 **외부 MCP 클라이언트**가 SSE 로 접속해 도구를 호출하는 흐름을 보여주는 데모 스크립트.

LLM 이 들어가는 자리는 의도적으로 비워두고 (스크립트가 도구를 직접 호출), 결과 JSON 을 보면 외부 LLM 이 어떤 입력으로 어떤 결과를 받게 될지가 그대로 드러나도록 했다.

## 실행

```bash
# 1. 다른 터미널에서 MCP 서버 띄우기
cd spring-ai-practice && ./gradlew bootRun

# 2. 이 데모 실행 (uv 가 mcp 패키지를 자동 설치)
uv run --with mcp spring-ai-practice/mcp-client-demo/client.py
```

## 출력 흐름

1. `initialize` — MCP 핸드셰이크 (서버명·프로토콜 버전 확인)
2. `tools/list` — 노출된 도구 3종과 입력 스키마 출력
3. `tools/call` × 3 — `searchProducts` → `checkInventory` → `getOrderStatus` 를 순차 호출
4. 각 결과 JSON 을 콘솔에 출력

## 실제 LLM 연결

이 스크립트는 "MCP 트랜스포트 + 도구 호출" 부분만 시연한다. LLM 이 직접 도구를 부르게 하려면:

- **Claude Code**: `claude mcp add --transport sse clothing-ecommerce http://localhost:8080/sse` 또는 루트 [README.md](../README.md) 의 `.mcp.json` 스니펫을 직접 저장
- **Claude Desktop**: 루트 [README.md](../README.md) 의 `claude_desktop_config.json` 예시 참고
- **MCP Inspector (대화형 GUI)**: `npx @modelcontextprotocol/inspector` → SSE `http://localhost:8080/sse`
