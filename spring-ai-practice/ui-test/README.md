# ui-test — 브라우저 자동 검증

Playwright 로 UI 의 다음 흐름을 자동 검증하고 스크린샷 5장을 `docs/screenshots/` 에 저장합니다.

1. UI 로드 (인증 불필요한 정적 페이지)
2. `shopper-llm` 로그인 → 채팅창 + 스코프 배지 표시
3. "재고 있는 블랙 옷 추천해줘" → LLM 이 `searchProducts` 도구를 동적 호출
4. 로그아웃 → `catalog-only-llm` 로그인 (order:read 배지가 회색·취소선)
5. "주문 ORD-1001 배송 상태 알려줘" → LLM 이 권한 부족을 자연어로 안내

## 실행

```bash
# 1. Ollama 와 모델 준비
brew install ollama
ollama serve &                          # 별도 터미널 또는 백그라운드
ollama pull llama3.2:3b                 # 도구 콜링 가능한 작은 모델 (~2GB)

# 2. Spring Boot 서버 기동
cd spring-ai-practice
./gradlew bootRun &
# health 확인
until curl -sf http://localhost:8080/actuator/health; do sleep 1; done

# 3. UI 자동 검증 (Playwright + Chromium 자동 설치)
uv run --with playwright python ui-test/test-ui-flow.py
```

스크린샷은 `docs/screenshots/01-login-view.png` ~ `05-chat-response-catalog-only.png` 로 저장됩니다.

## 환경변수

| 변수 | 기본값 |
|---|---|
| `MCP_SERVER_BASE` (별도, mcp-client-demo 용) | `http://localhost:8080` |
| 본 스크립트는 항상 `http://localhost:8080/` 로 접속합니다 (수정은 [test-ui-flow.py](./test-ui-flow.py) 안에서). |
