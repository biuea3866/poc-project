# mcp-external-server — 외부 MCP 서버 데모

우리 Spring AI 앱이 **클라이언트**로 접속해 도구를 끌어다 쓰는 외부 MCP 서버.

노출 도구:

- `convert_krw(amount_krw, target_currency)` — 원화를 USD/JPY/EUR/CNY 로 환산
- `season_tip(month)` — 월 1~12 입력 → 시즌별 추천 의류 카테고리

## 실행 순서

```bash
# 1. 외부 MCP 서버 띄우기 (9090 포트)
uv run --with mcp spring-ai-practice/mcp-external-server/server.py

# 2. Spring AI 앱을 외부 서버 연결 모드로 띄우기
EXTERNAL_MCP_ENABLED=true EXTERNAL_MCP_URL=http://localhost:9090 \
  ./gradlew bootRun
```

이제 UI 채팅창에서 *"5만원이면 미국에서 얼마야?"* 같은 질문을 하면 LLM 이 `convert_krw` 외부 도구를 자동 선택해 호출합니다. 우리 내부 도구(11개) + 외부 도구(2개) 가 LLM 입장에서 같은 도구 목록으로 보입니다.

서버 미실행이면 Spring 앱은 부팅 시 graceful fallback (외부 도구 0개) 하고 내부 도구만으로 동작합니다.
