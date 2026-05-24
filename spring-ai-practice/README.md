# spring-ai-practice

Spring AI MCP 서버 + 채팅 UI PoC — 의류 이커머스 도메인.

**유저가 채팅창에 자연어로 입력 → LLM 이 도구 12종(조회 7 + 쓰기 4 + 추출 1)을 동적으로 선택·호출 → 결과를 자연어로 응답** 하는 흐름을 보여줍니다. 같은 도구 빈을 MCP 프로토콜로도 노출해 외부 LLM 호스트(Claude Desktop 등)에서도 사용 가능합니다.

REST 로 도구를 직접 노출하는 API(`/api/tools/...`)는 의도적으로 두지 않습니다. 사용자는 자연어 채팅창 + 이미지 첨부만 봅니다.

> 🧭 **사용 흐름 다이어그램 (Mermaid)** — [`docs/architecture.md`](docs/architecture.md) 에 컴포넌트 다이어그램 1개 + 시퀀스 다이어그램 2개 (UI 흐름·MCP 흐름) 가 있습니다.

## 사용 중인 Spring AI 핵심 컴포넌트 (전부)

| # | 컴포넌트 | 본 PoC 내 사용 위치 |
|---|---|---|
| 1 | **`ChatClient`** + Builder + fluent prompt API | [ChatClientConfig.kt](src/main/kotlin/com/biuea/springai/config/ChatClientConfig.kt), [ChatGatewayService.kt](src/main/kotlin/com/biuea/springai/chat/ChatGatewayService.kt) |
| 2 | **`ChatModel`** (Ollama 어댑터 + Vision 어댑터) | autoconfig + [VisionService.kt](src/main/kotlin/com/biuea/springai/chat/VisionService.kt) (낮은 레벨 호출) |
| 3 | **`ChatMemory`** (`MessageWindowChatMemory` + `InMemoryChatMemoryRepository`) | ChatClientConfig |
| 4 | **`MessageChatMemoryAdvisor`** (대화 메모리 advisor) | ChatClientConfig |
| 5 | **`PromptTemplate`** (ST 기반 동적 시스템 프롬프트) | ChatGatewayService — 사용자 scopes 를 `{scopes}` 에 주입 |
| 6 | **`@Tool` / `@ToolParam`** | [CatalogTools.kt](src/main/kotlin/com/biuea/springai/tool/CatalogTools.kt), [ExtractionTool.kt](src/main/kotlin/com/biuea/springai/tool/ExtractionTool.kt) |
| 7 | **`MethodToolCallbackProvider`** (도구 → MCP ToolCallback) | [McpServerConfig.kt](src/main/kotlin/com/biuea/springai/config/McpServerConfig.kt) |
| 8 | **`EmbeddingModel`** (Ollama nomic-embed-text) | autoconfig — VectorStore 가 사용 |
| 9 | **`VectorStore`** (`SimpleVectorStore`) | [VectorStoreConfig.kt](src/main/kotlin/com/biuea/springai/config/VectorStoreConfig.kt) |
| 10 | **`Document`** + **`TokenTextSplitter`** | [KnowledgeBaseLoader.kt](src/main/kotlin/com/biuea/springai/service/KnowledgeBaseLoader.kt) — 시작 시 rag/*.md 청킹·임베딩 |
| 11 | **`QuestionAnswerAdvisor`** (RAG 자동 컨텍스트 주입) | ChatClientConfig |
| 12 | **`BeanOutputConverter`** (`chatClient.entity(...)` 또는 수동) | [ChatGatewayService.kt](src/main/kotlin/com/biuea/springai/chat/ChatGatewayService.kt) — `/chat` 응답을 `ChatAnswer` 로 받음 · [VisionService.kt](src/main/kotlin/com/biuea/springai/chat/VisionService.kt) — `VisionAnswer` 수동 변환 · [ExtractionTool.kt](src/main/kotlin/com/biuea/springai/tool/ExtractionTool.kt) — `ExtractedProduct` |
| 13 | **`Media`** + multimodal `UserMessage` | [VisionService.kt](src/main/kotlin/com/biuea/springai/chat/VisionService.kt) — 이미지 첨부 |
| 14 | **`chatClient.stream()`** + `Flux<String>` | [ChatGatewayController.kt](src/main/kotlin/com/biuea/springai/chat/ChatGatewayController.kt) — SSE `/chat/stream` |
| 15 | **MCP Server (WebMVC SSE/JSONRPC)** | `spring-ai-starter-mcp-server-webmvc` autoconfig — `/sse`, `/mcp/message` |
| 16 | **`McpSyncClient` + `SyncMcpToolCallbackProvider`** (외부 MCP 서버 클라이언트) | [McpClientConfig.kt](src/main/kotlin/com/biuea/springai/config/McpClientConfig.kt) — 외부 도구를 우리 ChatClient + MCP gateway 로 노출 |
| 17 | **`ImageModel`** (OpenAI DALL-E) + **`ImagePrompt`** | [ImageGenerationTool.kt](src/main/kotlin/com/biuea/springai/tool/ImageGenerationTool.kt) — 텍스트 → 이미지 도구 |
| 18 | **`spring-ai-bom`** (의존성 통합) | build.gradle.kts |
| 19 | **`@McpTool` / `@McpToolParam`** (Spring AI 1.1.x community 어노테이션) | [tool/McpAnnotationDemo.kt](src/main/kotlin/com/biuea/springai/tool/McpAnnotationDemo.kt) — `todayTip` 도구. 1.1.x autoconfig 가 빈을 자동 발견해 MCP 서버에 노출 |

각 사용 위치 파일의 클래스/메서드 KDoc 에 "이 컴포넌트가 무엇이고 왜 여기에 쓰는지" 가 주석으로 남겨져 있습니다.

## Ollama 가 뭐예요

**[Ollama](https://ollama.com/)** 는 **로컬 PC 에서 LLM 을 띄우는 무료 OSS 런타임**입니다. `ollama serve` 한 줄로 11434 포트에 OpenAI 호환 HTTP API 가 열리고, `ollama pull llama3.2:3b` 같이 모델을 받으면 추론이 가능합니다. 본 PoC 는:

- **API 키 없이** 인증·도구·MCP 흐름을 전부 학습할 수 있고
- 외부로 데이터가 나가지 않으며
- 작은 모델(`llama3.2:3b`, 약 2GB)로도 도구 콜링 데모가 충분합니다.

운영에서는 `application.yml` 의 `spring.ai.model.chat` 만 `openai` 또는 `anthropic` 으로 바꾸면 코드 변경 없이 외부 LLM 으로 전환됩니다. 즉 **Ollama 는 로컬에서 GPT 자리를 채우는 대체재**입니다. 상세 비교·동작 확인 명령은 [docs/architecture.md §1](docs/architecture.md#1-ollama-가-뭐예요).

## 스택

| 항목 | 버전 |
|---|---|
| Spring Boot | 3.4.5 (Spring AI 1.0.x 호환 — Boot 4.x 금지) |
| Spring AI | 1.1.6 (1.0.1 → 1.1.6 마이그레이션 완료) |
| Kotlin / Java | 2.1.20 / 21 |
| 기본 LLM | Ollama (로컬·무료) |
| MCP 전송 | SSE / streamable-HTTP (WebMVC) |
| 보안 | Spring Security 6.4.x + JJWT 0.12.6 + Resilience4j RateLimiter |

## 사전 준비 — docker-compose (권장)

레포 루트의 [`docker-compose.yml`](../docker-compose.yml) 이 Ollama + 외부 MCP 서버를 함께 띄우고 필수 모델 2개를 자동 pull 한다.

```bash
cd /Users/biuea/feature/flag_project
docker compose up -d                              # ollama + mcp-external-server 동시 기동
docker compose logs -f ollama-pull                # llama3.2:3b + nomic-embed-text 자동 다운로드 진행
```

Spring 앱은 호스트에서 띄우고 두 컨테이너의 노출 포트(11434, 9090) 를 그대로 사용한다.

```bash
cd spring-ai-practice
./gradlew bootRun                                                # 외부 MCP 비활성
EXTERNAL_MCP_ENABLED=true ./gradlew bootRun                      # 외부 MCP 도구도 함께 노출
./gradlew bootRun --args='--spring.profiles.active=tls'          # HTTPS (자체서명)
open http://localhost:8080
```

### Ollama 직접 설치 (Docker 없이)

```bash
brew install ollama
ollama serve &
ollama pull llama3.2:3b           # 채팅 + 도구 콜링 (필수)
ollama pull nomic-embed-text      # RAG 임베딩 (필수)
ollama pull llava:7b              # 멀티모달 (선택, /chat/vision 사용 시)
```

> **모델별 용도**
> - `llama3.2:3b` → 채팅 + 도구 선택. 미설치 시 `/chat` 호출이 502 `llm_unreachable` 응답.
> - `nomic-embed-text` → RAG 임베딩. 미설치 시 앱 시작 시 KnowledgeBaseLoader 가 graceful skip + 경고 로그.
> - `llava:7b` → 이미지 분석. 미설치 시 `/chat/vision` 응답이 비전 모델을 못 찾는다는 에러. docker-compose 시 `docker compose exec ollama ollama pull llava:7b` 추가 실행.

## 사용 흐름 (UI)

브라우저에서 `http://localhost:8080/` 접속:

1. **로그인 폼** — 클라이언트 자격증명으로 JWT 발급
   - `shopper-llm / dev-secret-1` → 조회 + 자기 주문 생성/취소
   - `catalog-only-llm / dev-secret-2` → 조회만
   - `ops-llm / dev-secret-3` → 운영자 (전체 권한 — 재고/배송 단계 수정 포함)
2. **채팅창** — 자연어 입력 (예시 아래 표)
3. **LLM 이 도구를 동적으로 선택** — Spring AI ChatClient 가 도구 11종의 JSON Schema 를 LLM 에 함께 제공, LLM 이 적절한 도구를 골라 인자를 채워 호출
4. **권한 부족 시** — 도구가 `AccessDeniedException` 을 던지고 LLM 이 그 사실을 자연어로 안내

### 도구 12종 — 자연어 질문 예시

조회 도구 (7개)

| 도구 | 스코프 | 자연어 질문 예시 |
|---|---|---|
| `searchProducts` | catalog:read | "재고 있는 블랙 옷 추천해줘" / "5만원 이하 자켓 보여줘" |
| `getProductDetails` | catalog:read | "P-1001 사이즈별 재고 알려줘" / "이 옷 무슨 소재야?" |
| `listCategories` | catalog:read | "어떤 카테고리가 있어?" / "색상 종류 뭐 있어?" |
| `checkInventory` | catalog:read | "P-1001 M 사이즈 있어?" |
| `listOrders` | order:read | "배송중인 주문 보여줘" / "전체 주문 목록" |
| `getOrderStatus` | order:read | "주문 ORD-1001 상태 알려줘" |
| `trackShipment` | order:read | "ORD-1002 어디까지 왔어?" |

쓰기 도구 (4개)

| 도구 | 스코프 | 자연어 질문 예시 |
|---|---|---|
| `placeOrder` | order:write | "P-1001 M 사이즈 2개 주문해줘" |
| `cancelOrder` | order:write | "ORD-1003 취소해줘" |
| `addTrackingEvent` | shipment:write | "ORD-1005 발송 처리해줘 (성남 풀필먼트)" |
| `restockProduct` | catalog:write | "P-1001 S 10개 입고" / "P-1002 M 3개 출고" |

LLM 추출 도구 (1개 — BeanOutputConverter)

| 도구 | 스코프 | 자연어 질문 예시 |
|---|---|---|
| `extractProductAttributes` | catalog:read | "이 설명에서 속성 뽑아줘 — '빈티지 오버핏 카키 면 자켓, M사이즈, 가을용'" |

### 추가 채팅 경로

| 경로 | 사용 컴포넌트 | 설명 |
|---|---|---|
| `POST /chat` | ChatClient `.call().entity(ChatAnswer)` | 동기 + **구조화 응답** (`{ answer, intent, mentionedProductIds, mentionedOrderIds, suggestedFollowUps }`) |
| `POST /chat/stream` | ChatClient `.stream()` → `Flux<String>` | SSE 스트리밍 — 토큰 청크 (스트리밍 본질상 구조화 미적용) |
| `POST /chat/vision` | ChatModel + `UserMessage.media(...)` + `BeanOutputConverter<VisionAnswer>` 수동 적용 | multipart 이미지 → 구조화 응답 (`{ description, detectedCategory, detectedColor, detectedFit, ... }`) |

추가로 매 채팅에서 **`QuestionAnswerAdvisor`** 가 자동으로 VectorStore 를 검색해 `rag/*.md` (사이즈 가이드 · FAQ · 관리 가이드) 의 관련 청크를 LLM 컨텍스트에 끼워 넣습니다. 따라서 *"키 175 / 70kg 인데 L 사이즈 맞아?"* 같은 질문도 도구 없이 자체 문서로 답변합니다.

### 보안 적용 범위

JWT 의 의무 사용 경로는 **외부 LLM / MCP 클라이언트 전용**입니다.

| 경로 | 인증 | 권한 검사 | 비고 |
|---|---|---|---|
| `POST /auth/login` | permitAll | — | 외부 클라이언트가 토큰 발급받는 엔드포인트 |
| `GET /sse`, `POST /mcp/**` | **Bearer JWT 필수** | `@RequireScope` + Spring AOP `ToolGuardAspect` | 외부 LLM 호스트 (Claude Desktop / MCP 클라이언트) 진입점 |
| `POST /chat`, `/chat/stream`, `/chat/vision` | permitAll | 없음 | 브라우저 UI 채팅 — 인증 없이 동작 |
| 정적 리소스 / `/actuator/health` | permitAll | — | |

UI 의 로그인 폼은 발급된 JWT 를 받기 위한 도우미 UI 로 유지되지만, UI 채팅 자체는 인증 없이 작동합니다.

### Structured Output — 모든 채팅 응답을 LLM 호출 결과로 구조화

모든 `/chat`·`/chat/vision` 응답은 `BeanOutputConverter` 로 JSON Schema 가 LLM 프롬프트에 자동 주입되어
LLM 이 정해진 스키마에 맞춰 응답을 채워주는 형태입니다.

#### `POST /chat` 응답 예시

요청
```bash
curl -sS -X POST localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"conversationId":"u1","message":"재고 있는 블랙 옷 추천해줘"}'
```

응답 (`ChatAnswer`)
```json
{
  "conversationId": "u1",
  "result": {
    "answer": "재고 있는 블랙 옷으로는 P-1002 오버핏 그래픽 반팔 티셔츠(44개, 29,000원), P-1007 구스다운 푸퍼 패딩(15개, 199,000원), P-1011 와이드 코튼 슬랙스(22개, 72,000원) 가 있어요.",
    "intent": "SEARCH",
    "mentionedProductIds": ["P-1002", "P-1007", "P-1011"],
    "mentionedOrderIds": [],
    "suggestedFollowUps": [
      "P-1002 의 사이즈별 재고 알려줘",
      "5만원 이하 블랙 옷만 보여줘"
    ]
  }
}
```

#### `POST /chat/vision` 응답 예시

요청
```bash
curl -sS -X POST localhost:8080/chat/vision \
  -F 'image=@jacket.jpg' \
  -F 'prompt=이 사진 속 옷을 분석해줘'
```

응답 (`VisionAnswer`)
```json
{
  "conversationId": "vision",
  "result": {
    "description": "검정색 코튼 자켓이에요. 오버핏 실루엣이고 가을용으로 적합해 보입니다.",
    "detectedCategory": "자켓",
    "detectedColor": "블랙",
    "detectedFit": "오버핏",
    "detectedMaterial": "코튼",
    "detectedSeason": "가을",
    "keywords": ["오버핏", "코튼", "캐주얼"]
  }
}
```

UI 는 `result.answer` (또는 `result.description`) 를 채팅 메시지 본문으로 렌더링하고, 그 아래에 메타데이터(intent · 언급 ID · 후속 질문 등) 를 작은 글씨로 함께 표시합니다.

#### Stream 은 비구조화 (예외)

`POST /chat/stream` 만 토큰 청크 텍스트를 그대로 흘려보냅니다 — SSE 스트리밍 특성상 응답을 객체로 묶을 수 없기 때문입니다. UI 의 "스트리밍 응답" 토글을 켜면 이 경로로 가고, 끄면 `/chat` 의 구조화 응답을 받습니다.

### 어노테이션 기반 스코프 검사 (`@RequireScope` + Spring AOP)

도구 메서드는 어노테이션만 부착하면 스코프 검사·감사 로그가 자동 적용됩니다.

```kotlin
@Tool(description = "...")
@RequireScope("catalog:read")            // ← 메타데이터만 선언
fun searchProducts(...): List<ProductSummary> {
    // 비즈니스 로직만 — 보일러플레이트 없음
    return productRepository.search(...)
}
```

동작 흐름:

1. `@Component` 빈 중 `@RequireScope` 메서드가 있으면 Spring 이 자동으로 **CGLIB 프록시** 생성
2. [`ToolGuardAspect`](src/main/kotlin/com/biuea/springai/security/ToolGuardAspect.kt) 가 `@Around("@annotation(requireScope)")` 포인트컷으로 메서드 호출을 가로챔
3. advice 가 호출 진입부에서 **SecurityContext 권한 검사** (`SCOPE_<name>`) + [`ToolAuditLogger`](src/main/kotlin/com/biuea/springai/audit/ToolAuditLogger.kt) 기록 후 `joinPoint.proceed()`
4. 스코프 부족 시 `AccessDeniedException` → LLM 응답에 자연어로 반영

#### MCP 경로에서도 AOP 가 동작하는 이유

이전엔 "MCP `MethodToolCallback` 가 리플렉션으로 호출하니 AOP 우회" 라고 추정했으나, 실제로는:
- `MethodToolCallback.toolObject` 가 **CGLIB 프록시 빈을 그대로 받음**
- `Method.invoke(proxyTarget, args)` 호출 시 프록시 클래스의 메서드가 실행 → advisor chain 통과
- 결과: 인앱 ChatClient 경로(`/chat`) 와 MCP 경로(`/sse`) 양쪽에서 동일하게 advice 트리거

검증 — MCP 클라이언트로 `searchProducts` 호출 시 audit 로그가 찍히는지로 확인 가능:
```text
INFO audit.tool : {"subject":"shopper-llm","tool":"searchProducts",...,"outcome":"success"}
WARN audit.tool : {"subject":"catalog-only-llm","tool":"getOrderStatus",...,"outcome":"denied","reason":"missing required scope: order:read"}
```

UI 채팅(`/chat` 계열) 은 SecurityFilterChain 에서 permitAll 이라 SecurityContext 가 anonymous. AOP advice 도 같이 작동하면 `authentication.authorities` 조회 시점에 NPE 가 나므로, UI 채팅에서는 도구 호출 자체가 일어나지 않거나(스코프 검사 미적용 도구로 한정) 사용자가 토큰을 함께 보내야 동작.

### 외부 MCP 서버 연동 (독립 프로젝트)

외부 MCP 서버는 별도 디렉토리 [`../mcp-external-server/`](../mcp-external-server/README.md) 로 분리되어 있으며 자체 Dockerfile · pyproject.toml 을 가진다.

```bash
# A. docker-compose 로 함께 (권장)
cd /Users/biuea/feature/flag_project
docker compose up -d mcp-external-server

# B. 또는 로컬 uv
cd mcp-external-server && uv run --with mcp python server.py

# Spring AI 앱을 연결 모드로 띄우기
cd spring-ai-practice
EXTERNAL_MCP_ENABLED=true EXTERNAL_MCP_URL=http://localhost:9090 \
  ./gradlew bootRun
```

부팅 로그에 `External MCP server 도구 2개 노출됨` + `Registered tools: 16` 이 보이면 성공. UI 에서 *"5만원이면 미국 달러로 얼마야?"* 같은 질문 시 LLM 이 외부 `convert_krw` 도구를 자동 호출합니다. 우리 앱은 **MCP gateway** 역할도 겸해 외부 도구가 우리 `/sse` 엔드포인트로도 노출됩니다 (외부 LLM 호스트가 우리 앱 하나만 보면 외부 + 내부 도구를 모두 사용 가능).

#### 클라이언트 패턴 — deprecation 정리

`SyncMcpToolCallbackProvider` 의 생성자는 1.1.x 부터 deprecated. 본 PoC 는 [McpClientConfig.kt](src/main/kotlin/com/biuea/springai/config/McpClientConfig.kt) 에서 권장 builder 패턴을 사용합니다.

```kotlin
SyncMcpToolCallbackProvider.builder()
    .mcpClients(client)
    .build()
    .toolCallbacks
```

### 이미지 생성 (`ImageModel`)

`OPENAI_API_KEY` 환경변수를 설정하면 LLM 이 *"미니멀 화이트 셔츠 디자인 시안 만들어줘"* 같은 요청 시 [ImageGenerationTool](src/main/kotlin/com/biuea/springai/tool/ImageGenerationTool.kt) 의 `generateProductImage` 도구를 자동 호출합니다 (DALL-E 3). 키가 없으면 빈이 등록되지 않고 LLM 응답에 "API 키가 없어 사용할 수 없다" 가 자연어로 반영됩니다. 스코프는 `catalog:write` (운영자급).

```bash
OPENAI_API_KEY=sk-... ./gradlew bootRun
```

UI 상단의 스코프 배지가 토큰의 보유 권한을 보여줍니다 (보유: 파란색, 미보유: 회색 + 취소선). 상세 흐름·다이어그램은 [`docs/architecture.md`](docs/architecture.md).

### UI 시연 스크린샷

| 단계 | 화면 |
|---|---|
| 로그인 | [docs/screenshots/01-login-view.png](docs/screenshots/01-login-view.png) |
| shopper-llm 채팅 — 블랙 옷 추천 정상 응답 | [docs/screenshots/03-chat-response-shopper.png](docs/screenshots/03-chat-response-shopper.png) |
| catalog-only-llm 채팅 — 주문 상태 요청 시 **권한 부족 안내** | [docs/screenshots/05-chat-response-catalog-only.png](docs/screenshots/05-chat-response-catalog-only.png) |

전체 5장은 [docs/screenshots/](docs/screenshots/), 재현 스크립트는 [ui-test/test-ui-flow.py](ui-test/test-ui-flow.py) — `uv run --with playwright python ui-test/test-ui-flow.py` 한 줄로 재실행됩니다.

## 엔드포인트

| 경로 | 메서드 | 인증 | 설명 |
|---|---|---|---|
| `GET /` | GET | permitAll | UI (static/index.html) |
| `POST /auth/login` | POST | permitAll | clientId/Secret → JWT 발급 |
| `POST /chat` | POST | permitAll | 자연어 → LLM(도구 동적 호출) → 구조화된 `ChatAnswer` JSON 응답 |
| `GET /sse` | GET (MCP) | Bearer JWT | MCP SSE 핸드셰이크 (외부 LLM 호스트) |
| `POST /mcp/message?sessionId=...` | POST (MCP) | Bearer JWT | MCP JSONRPC 메시지 |
| `GET /actuator/health` | GET | permitAll | 헬스체크 |

## 보안 PoC 7종

| 항목 | 구현 | 동작 |
|---|---|---|
| JWT 인증 (HS256) | [security/JwtService.kt](src/main/kotlin/com/biuea/springai/security/JwtService.kt) | Bearer 헤더 누락/오류 시 401 |
| 스코프 인가 | `@RequireScope("scope")` 어노테이션 + Spring AOP [security/ToolGuardAspect.kt](src/main/kotlin/com/biuea/springai/security/ToolGuardAspect.kt) (`@Around` advice) | 부족 시 403 + audit `denied` |
| Rate limit | Resilience4j [security/RateLimitFilter.kt](src/main/kotlin/com/biuea/springai/security/RateLimitFilter.kt) | 한도 초과 시 429 + `Retry-After` |
| 감사 로그 | [audit/ToolAuditLogger.kt](src/main/kotlin/com/biuea/springai/audit/ToolAuditLogger.kt) → `audit.tool` logger | subject·tool·args·outcome·latencyMs |
| 입력 검증 | [tool/ToolInputValidator.kt](src/main/kotlin/com/biuea/springai/tool/ToolInputValidator.kt) | 위반 시 400 + audit `error` |
| TLS | [application-tls.yml](src/main/resources/application-tls.yml) + `keystore.p12` (자체서명 RSA 2048) | `--spring.profiles.active=tls` |
| 시크릿 외부화 | `${JWT_SECRET}`, `${SHOPPER_LLM_SECRET}`, `${CATALOG_ONLY_SECRET}`, `${KEYSTORE_PASSWORD}` | 미설정 시 PoC 기본값 |

### SecurityContext 전파

Spring AI MCP server-webmvc 는 도구 호출을 Reactor `boundedElastic` worker 로 dispatch 하므로, servlet thread 의 ThreadLocal SecurityContext 가 자동 전파되지 않습니다. [SpringAiPracticeApplication.kt](src/main/kotlin/com/biuea/springai/SpringAiPracticeApplication.kt) 에서 `ContextRegistry.registerThreadLocalAccessor("security.context", ...)` + `Hooks.enableAutomaticContextPropagation()` 으로 Reactor automatic context propagation 을 활성화합니다.

## MCP 서버 (외부 LLM 호스트 연결)

`CatalogTools` 의 `@Tool` 3종이 Spring AI ChatClient(인앱) 와 MCP 서버(외부) 양쪽에 **같은 빈으로** 노출됩니다.

### 외부 LLM 호스트 연결

```bash
# Claude Code
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"shopper-llm","clientSecret":"dev-secret-1"}' | jq -r .accessToken)

claude mcp add --transport sse clothing-ecommerce http://localhost:8080/sse \
  --header "Authorization: Bearer $TOKEN"
```

### Python MCP 클라이언트 (학습용 데모)

```bash
# 전체 스코프
uv run --with mcp --with httpx mcp-client-demo/client.py

# catalog 전용 — getOrderStatus 호출이 "missing required scope: order:read" 로 거부됨
MCP_CLIENT_ID=catalog-only-llm MCP_CLIENT_SECRET=dev-secret-2 \
  uv run --with mcp --with httpx mcp-client-demo/client.py
```

자세한 구성은 [`mcp-client-demo/README.md`](mcp-client-demo/README.md) 참고.

### Claude Desktop 연결

```json
{
  "mcpServers": {
    "clothing-ecommerce": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/sse",
               "--header", "Authorization: Bearer <JWT>"]
    }
  }
}
```

## 테스트

### 자동 테스트 — 32/32 PASS

`./gradlew test` 한 번에 다음을 모두 실행합니다.

| 클래스 | 건수 | 검증 |
|---|---|---|
| `SpringAiPracticeApplicationTests` | 1 | 컨텍스트 로드 + MCP 도구 11개 등록 + 보안 빈 그래프 |
| `CatalogToolsTest` | 22 | 도구 11종 비즈니스 로직 — 조회·쓰기 동작 + 입력 검증 + 재고 차감/복원 (스코프 검사는 데코레이터로 분리) |
| AOP advice 동작 검증 | (수동) | MCP 클라이언트로 도구 호출 시 audit `success` / `denied` 로그 확인 |
| `SecurityIntegrationTest` | 8 | JWT 발급/거부, /sse 인증, 정적 리소스 permitAll, **/chat 토큰 없이 permitAll 확인** |
| `RateLimitIntegrationTest` | 1 | 한도 초과 시 429 + `Retry-After` |

### UI 자동 검증 (Playwright)

브라우저로 실제 LLM 흐름까지 확인합니다.

```bash
# 1. Ollama 와 모델 준비
ollama serve & ollama pull llama3.2:3b

# 2. 서버 기동
./gradlew bootRun &
until curl -sf http://localhost:8080/actuator/health; do sleep 1; done

# 3. UI 검증 (스크린샷 5장 → docs/screenshots/)
uv run --with playwright python ui-test/test-ui-flow.py
```

검증되는 시나리오:

1. UI 로드 (인증 불필요)
2. shopper-llm 로그인 → 채팅창 전환 + 스코프 배지
3. "재고 있는 블랙 옷 추천해줘" → LLM 이 `searchProducts` 호출 → P-1002·P-1007·P-1011 추천
4. 로그아웃 → catalog-only-llm 재로그인 (`order:read` 배지 회색 + 취소선)
5. "주문 ORD-1001 상태 알려줘" → LLM 이 "권한이 부족하여 주문 status를 확인하지 못합니다" 자연어 응답

상세 표·실제 응답·재현 명령은 [`docs/test-results.md`](docs/test-results.md).

## 주의

- **Boot 4.x 로 올리지 마세요** — Spring AI 1.0.x 와 바이너리 비호환입니다.
- 작은 로컬 Ollama 모델은 도구 콜링 신뢰도가 낮습니다. UI 채팅에서 도구가 잘 안 호출되면 더 큰 모델(`llama3.1:70b`)이나 외부 LLM 호스트로 시도하세요.
- 이 PoC 는 학습용입니다. 기본 자격증명(`dev-secret-1` 등)은 환경변수로 반드시 교체 후 노출하세요.
- 운영에서는 자체서명 인증서 대신 정식 인증서·OAuth2·Vault 로 교체하세요.
