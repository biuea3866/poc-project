# AI Wiki BE Technical Design Document

> 작성일: 2026-03-14 | 버전: 1.0 | 담당: be-developer

---

## 1. 아키텍처 개요

### 1.1 헥사고날 아키텍처 (Ports & Adapters)

AI Wiki 백엔드는 **헥사고날 아키텍처**를 채택하여 도메인 로직을 외부 인프라로부터 격리합니다.

```
┌─────────────────────────────────────────────────┐
│                   apps/api                       │
│  (Inbound Adapter: REST Controller, SSE)         │
├─────────────────────────────────────────────────┤
│                core/application                  │
│  (UseCase, Service, Port 호출)                    │
├─────────────────────────────────────────────────┤
│                 core/domain                      │
│  (Entity POJO, 상태 머신, Port 인터페이스)          │
├─────────────────────────────────────────────────┤
│              adapters/                           │
│  persistence-jpa (Outbound: JPA Repository)      │
│  ai (Outbound: Mock AI Adapters)                 │
└─────────────────────────────────────────────────┘
```

**의존 방향**: `apps/api` → `core/application` → `core/domain` ← `adapters/*`

### 1.2 멀티모듈 구조

```
ai-wiki-backend/
├── apps/
│   └── api/                    # Spring Boot 메인 애플리케이션
├── core/
│   ├── domain/                 # 도메인 모델, Port 인터페이스
│   └── application/            # UseCase, Service
└── adapters/
    ├── persistence-jpa/        # JPA 영속성 어댑터
    └── ai/                     # Mock AI 어댑터 (Summary, Tagger, Embedding)
```

### 1.3 기술 스택

| 항목 | 기술 |
|------|------|
| 언어 | Kotlin 2.2 |
| 프레임워크 | Spring Boot 3.3.3 |
| 빌드 | Gradle Kotlin DSL |
| JDK | 21 |
| DB (개발) | H2 In-Memory |
| DB (운영) | PostgreSQL 15 |
| ORM | Spring Data JPA + Hibernate |
| 비동기 | Spring @Async |
| 실시간 | SSE (SseEmitter) |
| 인증 | Spring Security (개발: permitAll) |
| 모니터링 | Pinpoint Agent |

---

## 2. 도메인 모델

### 2.1 Document (핵심 Aggregate)

```kotlin
class Document(
    val id: Long?,
    var title: String,
    var content: String,
    var status: DocumentStatus,      // DRAFT → ACTIVE → DELETED
    var aiStatus: AiStatus,          // NOT_STARTED → PENDING → PROCESSING → COMPLETED/FAILED
    var tags: List<String>,
    var summary: String?,
    val createdBy: Long,
    var updatedBy: Long,
    val createdAt: Instant?,
    var updatedAt: Instant?,
)
```

### 2.2 DocumentStatus (문서 상태)

```
DRAFT ──activate()──→ ACTIVE ──delete()──→ DELETED
```

| 상태 | 설명 |
|------|------|
| DRAFT | 초안, 수정 가능 |
| ACTIVE | 게시 상태, AI 분석 가능, 수정 시 Revision 생성 |
| DELETED | 논리 삭제 |

### 2.3 AiStatus (AI 처리 상태)

```
NOT_STARTED ──requestAnalysis()──→ PENDING ──startProcessing()──→ PROCESSING
                                                                      │
                                              completeAnalysis() ←────┤
                                              failAnalysis()     ←────┘
                                                    │                  │
                                                COMPLETED          FAILED
```

| 상태 | 설명 |
|------|------|
| NOT_STARTED | AI 미요청 |
| PENDING | 분석 요청됨, 대기 중 |
| PROCESSING | AI 파이프라인 실행 중 (재요청 불가) |
| COMPLETED | 처리 완료, summary/tags 생성됨 |
| FAILED | 처리 실패 |

### 2.4 DocumentRevision

```kotlin
class DocumentRevision(
    val id: Long?,
    val documentId: Long,
    val title: String,
    val content: String,
    val status: DocumentStatus,
    val createdBy: Long,
    val createdAt: Instant?,
)
```

ACTIVE 문서 수정 시 자동으로 Revision이 생성됩니다.

---

## 3. API 명세

### 3.1 문서 CRUD

| Method | Path | 설명 | 요청 Body |
|--------|------|------|----------|
| POST | `/api/v1/documents` | 문서 초안 생성 (DRAFT) | `{ title, content, tags, userId }` |
| GET | `/api/v1/documents` | 본인 ACTIVE 문서 검색 | `?userId=&q=` |
| GET | `/api/v1/documents/{id}` | 문서 상세 조회 | - |
| PATCH | `/api/v1/documents/{id}` | 문서 수정 (낙관적 잠금) | `{ title, content, tags, userId, expectedUpdatedAt }` |

### 3.2 상태 전환

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/documents/{id}/activate` | DRAFT → ACTIVE 전환 |
| POST | `/api/v1/documents/{id}/analyze` | AI 분석 요청 (ACTIVE만 가능) |

### 3.3 실시간 이벤트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/documents/{id}/ai-status/stream` | SSE로 AI 상태 실시간 수신 |

### 3.4 응답 형식

```json
{
  "id": 1,
  "title": "문서 제목",
  "status": "ACTIVE",
  "aiStatus": "COMPLETED",
  "excerpt": "본문 앞 100자...",
  "tags": ["tag1", "tag2"],
  "updatedAt": "2026-03-14T06:00:00Z"
}
```

### 3.5 에러 응답

| HTTP Status | 상황 |
|-------------|------|
| 400 | 잘못된 상태 전이 (DRAFT에서 analyze 등) |
| 403 | 타인 문서 접근 |
| 409 | 낙관적 잠금 충돌 / PROCESSING 중 재요청 |
| 404 | 문서 미존재 |

---

## 4. AI 파이프라인 설계

### 4.1 흐름

```
[Client] POST /analyze
    │
    ▼
[DocumentController] → DocumentCommandService.requestAnalysis()
    │                   → aiStatus = PENDING, save
    ▼
[AiPipelineService.processDocument()] ← @Async 비동기 실행
    │
    ├─ 1. startProcessing() → aiStatus = PROCESSING
    │     → ApplicationEventPublisher.publishEvent(PROCESSING)
    │
    ├─ 2. Thread.sleep(1000) — AI stub 시뮬레이션
    │
    ├─ 3. completeAnalysis(summary, tags) → aiStatus = COMPLETED
    │     → ApplicationEventPublisher.publishEvent(COMPLETED)
    │
    └─ (예외 시) failAnalysis() → aiStatus = FAILED
          → ApplicationEventPublisher.publishEvent(FAILED)
```

### 4.2 이벤트 기반 SSE 연동

```
[AiPipelineService]
    │ publishEvent(AiStatusChangedEvent)
    ▼
[DocumentController.@EventListener]
    │ onAiStatusChanged()
    ▼
[SseEmitter.send()] → Client에 실시간 push
```

- **SseEmitter 관리**: `ConcurrentHashMap<documentId, CopyOnWriteArrayList<SseEmitter>>`
- **Timeout**: 60초
- **자동 정리**: onCompletion, onTimeout, onError 콜백에서 emitter 제거
- **종료 조건**: COMPLETED 또는 FAILED 이벤트 수신 시 `emitter.complete()` 호출

### 4.3 향후 확장 (Mock → 실제 AI)

현재는 `AiPipelineService`에서 stub으로 처리하지만, 향후:

1. `SummaryPort`, `TaggerPort`, `EmbeddingPort` 인터페이스 구현
2. Mock Adapter → 실제 AI API 호출 Adapter로 교체
3. `@Async` → MQ 기반 비동기 처리로 전환 가능

---

## 5. ERD

### 5.1 documents 테이블

```sql
CREATE TABLE documents (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title           VARCHAR(255)    NOT NULL,
    content         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    ai_status       VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED',
    summary         TEXT,
    created_by      BIGINT          NOT NULL,
    updated_by      BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entity_version  BIGINT          NOT NULL DEFAULT 0
);
```

### 5.2 document_tags 테이블

```sql
CREATE TABLE document_tags (
    document_id     BIGINT          NOT NULL REFERENCES documents(id),
    tag_name        VARCHAR(100)    NOT NULL,
    PRIMARY KEY (document_id, tag_name)
);
```

### 5.3 document_revisions 테이블

```sql
CREATE TABLE document_revisions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id     BIGINT          NOT NULL REFERENCES documents(id),
    title           VARCHAR(255)    NOT NULL,
    content         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    created_by      BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 5.4 관계

```
documents (1) ──── (*) document_tags
documents (1) ──── (*) document_revisions
```

---

## 6. 보안 설계

### 6.1 개발 환경

- Spring Security: 모든 요청 `permitAll()`
- CSRF 비활성화
- H2 Console 접근을 위한 frameOptions 비활성화

### 6.2 운영 환경 (향후)

- JWT 기반 인증
- `@AuthenticationPrincipal`로 userId 주입
- 본인 문서만 접근 가능하도록 서비스 레이어에서 ownerId 검증

---

## 7. 설정

### 7.1 application.yml (개발)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:aiwiki;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

---

## 8. 모듈 의존성

```
apps/api
  ├── core/application
  ├── core/domain
  └── adapters/persistence-jpa

core/application
  └── core/domain

adapters/persistence-jpa
  └── core/domain

adapters/ai
  └── core/domain
```
