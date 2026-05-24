# Spring AI PoC — 테스트 결과

| 항목 | 값 |
|---|---|
| 실행일 | 2026-05-23 |
| Spring Boot | 3.4.5 |
| Spring AI | 1.0.1 |
| JDK | 21 (Java 21.0.10) |
| 빌드 | `./gradlew test` |
| 추가 보안 스택 | Spring Security 6.4.x + JJWT 0.12.6 + Resilience4j 2.2.0 |
| LLM (수동 검증) | Ollama llama3.2:3b |
| UI 자동 검증 | Playwright + Chromium |

---

## 1. 자동 테스트 — 46/46 PASS

| 클래스 | 건수 | 결과 |
|---|---|---|
| `SpringAiPracticeApplicationTests` | 1 | ✅ |
| `CatalogToolsTest` | 22 | ✅ |
| `ToolGuardTest` | 15 | ✅ |
| `SecurityIntegrationTest` | 7 | ✅ |
| `RateLimitIntegrationTest` | 1 | ✅ |

쓰기 도구 4종(`placeOrder` / `cancelOrder` / `addTrackingEvent` / `restockProduct`) + 새 스코프 3종(`order:write` / `shipment:write` / `catalog:write`) 추가로 32 → 46 건으로 늘었습니다.

추가 케이스 요약:
- `CatalogToolsTest` (13 → 22): U-14~U-22 — 주문 생성/취소, 배송 이벤트 추가, 재고 증감 동작 + 경계값
- `ToolGuardTest` (10 → 15): G-11~G-15 — 쓰기 도구별 스코프 거부 + 운영자 권한 통합

### 1-1. 컨텍스트 로드 (`SpringAiPracticeApplicationTests`)

| 테스트 | 결과 | 검증 |
|---|---|---|
| `contextLoads()` | ✅ | ChatModel 모킹 후 전체 빈 그래프(ChatClient·ChatMemory·CatalogTools·ToolGuard·MCP 서버 도구 3개·보안 필터 체인) 정상 구성 |

### 1-2. 도구 단위 (`CatalogToolsTest`)

SecurityContext 에 `SCOPE_catalog:read` + `SCOPE_order:read` 권한을 가진 가짜 주체를 주입해 ToolGuard 통과.

| ID | 케이스 | 결과 |
|---|---|---|
| U-01 | `searchProducts` 색상 필터 (color=블랙) | ✅ |
| U-02 | `searchProducts` 최대가격 필터 (≤30,000) | ✅ |
| U-03 | `checkInventory` 보유 재고 반환 | ✅ |
| U-04 | `checkInventory` 품절 안내 | ✅ |
| U-05 | `checkInventory` 미존재 상품 | ✅ |
| U-06 | `getOrderStatus` 정상 (ORD-1001 → 배송중) | ✅ |
| U-07 | `getOrderStatus` 미존재 주문 | ✅ |
| U-08 | `getProductDetails` 사이즈별 재고·메타정보 반환 | ✅ |
| U-09 | `listCategories` 카테고리·색상 목록 반환 (distinct) | ✅ |
| U-10 | `listOrders(null)` 전체 5건 | ✅ |
| U-11 | `listOrders("배송완료")` 필터링 → ORD-1002 1건 | ✅ |
| U-12 | `listOrders("폐기")` → IllegalArgumentException (허용 상태 외) | ✅ |
| U-13 | `trackShipment` 이벤트 시간순 + 결제완료~배송완료 포함 | ✅ |

### 1-3. ToolGuard / ScopeGuard (`ToolGuardTest`)

| ID | 케이스 | 결과 |
|---|---|---|
| G-01 | 인증 객체 없음 → `AccessDeniedException("authentication required")` | ✅ |
| G-02 | `order:read` 만 보유 + `searchProducts` → `"missing required scope: catalog:read"` | ✅ |
| G-03 | `catalog:read` 보유 + `searchProducts` 정상 동작 | ✅ |
| G-04 | `catalog:read` 만 보유 + `getOrderStatus` → `"missing required scope: order:read"` | ✅ |
| G-05 | `catalog:read` 보유 + `checkInventory("DROP_TABLE", "M")` → `IllegalArgumentException` (입력 검증) | ✅ |
| G-06 | 양쪽 스코프 보유 + 모든 도구 정상 동작 | ✅ |
| G-07 | `order:read` 만 보유 + `getProductDetails` 거부 | ✅ |
| G-08 | `order:read` 만 보유 + `listCategories` 거부 | ✅ |
| G-09 | `catalog:read` 만 보유 + `listOrders` 거부 | ✅ |
| G-10 | `catalog:read` 만 보유 + `trackShipment` 거부 | ✅ |

### 1-4. 보안 통합 (`SecurityIntegrationTest`)

`@SpringBootTest(webEnvironment=RANDOM_PORT)` + 실 내장 톰캣 + 실 JWT 발급/검증.

| ID | 케이스 | 결과 |
|---|---|---|
| S-01 | 정상 자격증명 → JWT 발급 (scopes 포함) | ✅ |
| S-02 | 잘못된 자격증명 → 401 | ✅ |
| S-03 | 토큰 없이 `/sse` → 401 | ✅ |
| S-04 | 잘못된 토큰 `/sse` → 401 | ✅ |
| S-05 | `catalog-only-llm` 자격증명 → JWT 발급, scopes=[catalog:read] | ✅ |
| S-06 | UI 페이지 `/` → 200 (permitAll) | ✅ |
| S-07 | `/actuator/health` → 200 (permitAll) | ✅ |

### 1-5. Rate Limit (`RateLimitIntegrationTest`)

| ID | 케이스 | 결과 |
|---|---|---|
| S-08 | 5회 호출 정상 → 6번째 429 + `Retry-After` ≥ 1 + body `{"error":"rate_limited",...}` | ✅ |

---

## 2. UI 자동 검증 — Playwright 5단계 시나리오

Ollama llama3.2:3b 모델로 실 LLM 응답을 받았고, Playwright Chromium 으로 UI 전 흐름을 캡처했습니다.

스크립트: [`ui-test/test-ui-flow.py`](../ui-test/test-ui-flow.py) — `uv run --with playwright python ui-test/test-ui-flow.py` 한 줄로 재현 가능.

### 시나리오 + 스크린샷

| # | 단계 | 캡처 | 결과 |
|---|---|---|---|
| 1 | UI 로드 (인증 불필요한 정적 페이지) | [01-login-view.png](./screenshots/01-login-view.png) | 로그인 폼 표시 — Client ID 셀렉트, Client Secret 입력, JWT 발급 버튼 |
| 2 | shopper-llm 로그인 → 채팅창 전환 | [02-chat-view-shopper.png](./screenshots/02-chat-view-shopper.png) | 헤더에 `catalog:read` + `order:read` 배지 (둘 다 파란색), 우측에 subject=shopper-llm |
| 3 | **"재고 있는 블랙 옷 추천해줘"** | [03-chat-response-shopper.png](./screenshots/03-chat-response-shopper.png) | LLM 이 `searchProducts(color=블랙)` 도구를 자동 호출 → P-1002 / P-1007 / P-1011 정확히 추천 |
| 4 | 로그아웃 → catalog-only-llm 재로그인 | [04-chat-view-catalog-only.png](./screenshots/04-chat-view-catalog-only.png) | `catalog:read` 배지만 파란색, `order:read` 배지는 **회색 + 취소선** (미보유) |
| 5 | **"주문 ORD-1001 배송 상태 알려줘"** | [05-chat-response-catalog-only.png](./screenshots/05-chat-response-catalog-only.png) | LLM 응답: *"저는 주문 status를 확인할 수 없습니다. **권한이 부족하여** 주문 status를 확인하지 못합니다."* |

### 3단계 (shopper-llm) — LLM 실제 응답

> 저는 재고 있는 블랙 옷을 추천해 드리겠습니다.
>
> 1. 오버핏 그래픽 반팔 티셔츠 (P-1002) - 44개 재고, 가격: 29,000원
>    이 티셔츠에는 그래픽 패턴이 …. 오버핏의 디자인으로 편안한 착용감을 느낄 수 있습니다.
>
> 2. 구스다운 푸퍼 패딩 (P-1007) - 15개 재고, 가격: 199,000원
>    이 패딩은 블랙으로 … 고급스러운 디자인을 보입니다. 구스다운의 재질로 추운 날씨에도 편안하게 착용할 수 있습니다.
>
> 3. 와이드 코튼 슬랙스 (P-1011) - 22개 재고, 가격: 72,000원
>    이 슬랙스는 블랙으로 … 스타일리시한 디자인을 보입니다. 코튼 재질로 편안하고 부드러운 착용감을 느낄 수 있습니다.

→ 상품 ID·재고·가격이 시드 데이터(`products.json`)와 100% 일치. LLM 이 도구 결과만 참조해 합성했음을 확인.

### 5단계 (catalog-only-llm) — LLM 실제 응답

> 저는 주문 status를 확인할 수 없습니다. **권한이 부족하여** 주문 status를 확인하지 못합니다.

→ 도구가 `AccessDeniedException("missing required scope: order:read")` 를 던지자, LLM 이 그 사실을 자연어로 사용자에게 안내. (작은 모델 특성상 이후에 환각 텍스트가 일부 따라붙으나, 권한 거부 인지 자체는 정확히 수행됨.)

### 검증된 audit 로그 (서버 로그)

```
INFO audit.tool : {"timestamp":"2026-05-23T02:52:45.320078Z","subject":"shopper-llm",
  "tool":"searchProducts","args":{"keyword":"","category":"","color":"블랙","maxPrice":0},
  "outcome":"success","latencyMs":0}

WARN audit.tool : {"timestamp":"2026-05-23T02:52:53.389317Z","subject":"catalog-only-llm",
  "tool":"getOrderStatus","args":{"orderId":"ORD-1001"},
  "outcome":"denied","latencyMs":0,"reason":"missing required scope: order:read"}
```

---

## 3. 수동 보안 검증 — curl

| 케이스 | 명령 | 결과 |
|---|---|---|
| 토큰 없이 `/chat` | `curl -X POST localhost:8080/chat -H 'Content-Type: application/json' -d '...'` | 401 `missing or invalid bearer token` |
| 잘못된 자격증명 `/auth/login` | `-d '{"clientId":"shopper-llm","clientSecret":"WRONG"}'` | 401 `invalid client credentials` |
| 토큰 없이 `/sse` | `curl -N -m 2 localhost:8080/sse` | 401 |
| Ollama 미실행 시 `/chat` (Bearer 정상) | (LLM 연결 실패) | 502 `llm_unreachable` |
| UI 페이지 | `GET /` | 200 `text/html` |

---

## 4. 보안 PoC 7종 — 종합

| # | 항목 | 구현 | 검증 |
|---|---|---|---|
| 1 | JWT 인증 (HS256) | `JwtService` + `JwtAuthenticationFilter` + `AuthController` | S-01~04 + UI 로그인 + curl |
| 2 | 스코프 인가 (catalog:read / order:read) | `ScopeGuard` + `ToolGuard` (도구 진입부 명시 호출) | G-01~04, G-06 + **UI 5단계 (LLM 응답에 권한 부족 반영)** |
| 3 | Rate Limit (Resilience4j) | `RateLimitFilter` | S-08 |
| 4 | 감사 로그 (`audit.tool` JSON 한 줄) | `ToolAuditLogger` | G-01~06 + **UI 검증 시 success/denied 라인 확인** |
| 5 | TLS 자체서명 | `application-tls.yml` + `keystore.p12` (RSA 2048) | 수동 (`--spring.profiles.active=tls`) |
| 6 | 도구 입력 검증 (정규식 화이트리스트) | `ToolInputValidator` | G-05 |
| 7 | 시크릿 외부화 | `${JWT_SECRET}` 등 환경변수 fallback | application.yml 정상 부트 |

추가 인프라:
- **Reactor SecurityContext propagation** — `ContextRegistry.registerThreadLocalAccessor("security.context", ...)` + `Hooks.enableAutomaticContextPropagation()` 으로 MCP boundedElastic worker 에 servlet thread 의 SecurityContext 가 정확히 복원됨.

---

## 5. 산출물 경로

| 형식 | 경로 |
|---|---|
| JUnit XML | [build/test-results/test/](../build/test-results/test/) |
| HTML 리포트 | [build/reports/tests/test/index.html](../build/reports/tests/test/index.html) |
| UI 검증 스크린샷 (5장) | [docs/screenshots/](./screenshots/) |
| UI 검증 스크립트 | [ui-test/test-ui-flow.py](../ui-test/test-ui-flow.py) |
| 본 문서 | [docs/test-results.md](./test-results.md) |

---

## 6. 전체 재현 명령

```bash
# ─ 사전 준비 ─────────────────────────────────────────────────
brew install ollama
ollama serve &                       # 별도 터미널 또는 백그라운드
ollama pull llama3.2:3b              # 도구 콜링 가능한 작은 모델 (~2GB)

# ─ 자동 테스트 (22 건) ───────────────────────────────────────
cd spring-ai-practice
./gradlew test --rerun-tasks

# ─ 서버 기동 ─────────────────────────────────────────────────
./gradlew bootRun &
until curl -sf http://localhost:8080/actuator/health; do sleep 1; done

# ─ UI 자동 검증 (스크린샷 5장 캡처) ──────────────────────────
uv run --with playwright python ui-test/test-ui-flow.py

# ─ 외부 MCP 클라이언트 (Python) ──────────────────────────────
uv run --with mcp --with httpx mcp-client-demo/client.py

# ─ TLS 프로필 ────────────────────────────────────────────────
./gradlew bootRun --args='--spring.profiles.active=tls'   # https://localhost:8443

# ─ 브라우저로 직접 체험 ──────────────────────────────────────
open http://localhost:8080
```
