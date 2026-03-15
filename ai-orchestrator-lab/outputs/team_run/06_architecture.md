# Stage 06: 시스템 아키텍처 설계

> PRD v2.1 + 기술 분석 기준 | 작성일: 2026-03-14

---

## 1. 아키텍처 원칙

- **헥사고날 아키텍처**: 도메인 로직을 외부 기술(DB, AI, HTTP)로부터 격리
- **단방향 의존성**: `apps/api` → `core/application` → `core/domain` ← `adapters/*`
- **JPA Entity ≠ 도메인 POJO**: 영속성 관심사를 도메인에서 완전 분리
- **Port/Adapter 패턴**: 외부 연동(AI, DB, 이벤트)은 Port 인터페이스 + Adapter 구현

---

## 2. 멀티 모듈 구조

```
ai-wiki-backend/
├── apps/
│   └── api/                          # Spring Boot 진입점, HTTP 어댑터
│       ├── controller/               # REST Controller (Inbound Adapter)
│       ├── sse/                       # SSE Emitter 관리
│       ├── config/                    # Security, Web, Async 설정
│       └── AiWikiApiApplication.kt
│
├── core/
│   ├── domain/                        # 순수 도메인 (외부 의존성 ZERO)
│   │   ├── document/
│   │   │   ├── Document.kt           # 도메인 POJO + 상태 전이 로직
│   │   │   ├── DocumentStatus.kt     # DRAFT | ACTIVE | DELETED
│   │   │   ├── AiStatus.kt           # NOT_STARTED | PENDING | PROCESSING | COMPLETED | FAILED
│   │   │   └── DocumentRevision.kt   # Revision 도메인
│   │   ├── tag/
│   │   │   └── Tag.kt                # 태그 도메인
│   │   └── port/                      # Outbound Port 인터페이스
│   │       ├── DocumentRepository.kt  # 영속성 포트
│   │       ├── SummaryPort.kt         # AI 요약 포트
│   │       ├── TaggerPort.kt          # AI 태깅 포트
│   │       ├── EmbeddingPort.kt       # AI 임베딩 포트
│   │       └── DocumentEventPublisher.kt  # 이벤트 발행 포트
│   │
│   └── application/                   # 유스케이스 (Inbound Port 구현)
│       ├── document/
│       │   ├── CreateDocumentUseCase.kt
│       │   ├── UpdateDocumentUseCase.kt
│       │   ├── DeleteDocumentUseCase.kt
│       │   ├── ChangeDocumentStatusUseCase.kt
│       │   ├── AnalyzeDocumentUseCase.kt
│       │   └── SearchDocumentUseCase.kt
│       └── pipeline/
│           └── AiPipelineService.kt   # summary → tagger → embedding 오케스트레이션
│
└── adapters/
    ├── persistence-jpa/               # JPA 영속성 어댑터
    │   ├── entity/
    │   │   ├── DocumentJpaEntity.kt   # JPA Entity (@Version 포함)
    │   │   ├── TagJpaEntity.kt
    │   │   └── DocumentRevisionJpaEntity.kt
    │   ├── repository/
    │   │   └── DocumentJpaRepository.kt  # Spring Data JPA
    │   └── mapper/
    │       └── DocumentMapper.kt      # Entity ↔ Domain 변환
    │
    ├── ai/                            # AI 서비스 어댑터 (Outbound)
    │   ├── openai/
    │   │   ├── OpenAiSummaryAdapter.kt
    │   │   ├── OpenAiTaggerAdapter.kt
    │   │   └── OpenAiEmbeddingAdapter.kt
    │   └── mock/
    │       ├── MockSummaryAdapter.kt
    │       ├── MockTaggerAdapter.kt
    │       └── MockEmbeddingAdapter.kt
    │
    └── event/                         # 이벤트 어댑터
        └── SpringEventPublisherAdapter.kt
```

---

## 3. 의존성 방향

```
┌─────────────────────────────────────────────────────────┐
│                      apps/api                           │
│  (Controller, SSE, Config)                              │
│  Inbound Adapter                                        │
└────────────────────────┬────────────────────────────────┘
                         │ depends on
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  core/application                        │
│  (UseCase, PipelineService)                              │
│  Inbound Port 구현                                       │
└────────────────────────┬────────────────────────────────┘
                         │ depends on
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    core/domain                           │
│  (Document, Tag, Status, Port interfaces)                │
│  순수 도메인 — 외부 의존성 ZERO                            │
└─────────────────────────────────────────────────────────┘
                         ▲
                         │ implements (Port)
┌─────────────────────────────────────────────────────────┐
│                      adapters/*                          │
│  persistence-jpa  │  ai  │  event                        │
│  Outbound Adapter                                        │
└─────────────────────────────────────────────────────────┘
```

**강제 규칙**:
- `core/domain`은 다른 모듈을 참조하지 않는다
- `core/application`은 `core/domain`만 참조한다
- `adapters/*`는 `core/domain`의 Port 인터페이스를 구현한다
- `apps/api`는 `core/application`과 `adapters/*`를 조립한다 (DI)

---

## 4. 데이터 흐름

### 4.1 문서 생성 흐름

```
Client POST /api/v1/documents
    → DocumentController.create()
    → CreateDocumentUseCase.execute(command)
    → Document.createDraft(title, content, ownerId)     [Domain]
    → DocumentRepository.save(document)                  [Port]
    → DocumentJpaRepository.save(entity)                 [Adapter]
    → 201 Created
```

### 4.2 AI 파이프라인 실행 흐름

```
Client POST /api/v1/documents/{id}/analyze
    → DocumentController.analyze()
    → AnalyzeDocumentUseCase.execute(documentId)
        → document.requestAnalysis()                    [Domain: ACTIVE 검증, AI 상태 전이]
        → DocumentRepository.save(document)              [aiStatus = PENDING]
        → DocumentEventPublisher.publish(AnalyzeRequested)
    → 202 Accepted

    [Async Event Handler]
    → AiPipelineService.execute(documentId)
        → document.startProcessing()                    [aiStatus = PROCESSING]
        → SummaryPort.summarize(content)                 [Step 1]
        → publish(StagCompleted("summary"))
        → TaggerPort.extractTags(content)                [Step 2]
        → publish(StageCompleted("tagger"))
        → EmbeddingPort.embed(content)                   [Step 3]
        → publish(StageCompleted("embedding"))
        → document.completeAnalysis(summary, tags, embedding)
        → DocumentRepository.save(document)              [aiStatus = COMPLETED]
        → publish(PipelineCompleted)

    [On Failure]
        → document.failAnalysis()                       [aiStatus = FAILED]
        → publish(PipelineFailed)
```

### 4.3 SSE 이벤트 흐름

```
Client GET /api/v1/documents/{id}/events
    → SseController.subscribe(documentId)
    → SseEmitterManager.register(documentId, emitter)

    [Event Listener]
    → @EventListener(StageCompleted)
    → SseEmitterManager.send(documentId, event)
    → Client receives: { "type": "STAGE_COMPLETED", "stage": "summary", "ts": "..." }
```

### 4.4 검색 흐름

```
Client GET /api/v1/documents/search?q=keyword&tag=java
    → DocumentController.search(query, tag, ownerId)
    → SearchDocumentUseCase.execute(criteria)
    → DocumentRepository.search(criteria)               [Port]
    → JPA: WHERE owner_id = ? AND status = 'ACTIVE'
            AND (title ILIKE ? OR content_tsvector @@ ?)
            AND id IN (SELECT document_id FROM tags WHERE name = ?)
    → List<Document>
```

---

## 5. ERD (핵심 엔티티)

```
┌─────────────────────────┐       ┌──────────────────────┐
│       documents          │       │        tags           │
├─────────────────────────┤       ├──────────────────────┤
│ id          UUID    PK   │──┐   │ id        UUID   PK   │
│ title       VARCHAR      │  │   │ name      VARCHAR     │
│ content     TEXT          │  └──→│ document_id UUID  FK  │
│ status      VARCHAR      │      └──────────────────────┘
│ ai_status   VARCHAR      │
│ parent_id   UUID    FK   │──→ self (계층 구조)
│ owner_id    UUID    FK   │
│ summary     TEXT         │      ┌──────────────────────────┐
│ embedding   VECTOR       │      │   document_revisions      │
│ revision    INTEGER      │      ├──────────────────────────┤
│ created_at  TIMESTAMP    │  ┌──→│ id             UUID  PK   │
│ updated_at  TIMESTAMP    │──┘   │ document_id    UUID  FK   │
│ deleted_at  TIMESTAMP    │      │ revision_number INTEGER   │
└─────────────────────────┘      │ content        TEXT       │
                                  │ created_at     TIMESTAMP  │
                                  │ created_by     UUID       │
                                  └──────────────────────────┘
```

---

## 6. 컴포넌트 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (Next.js)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ Doc List │ │ Doc Edit │ │  Search  │ │  SSE Status View │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬─────────┘   │
└───────┼────────────┼────────────┼─────────────────┼─────────────┘
        │ REST       │ REST       │ REST            │ SSE
        ▼            ▼            ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     apps/api (Spring Boot)                       │
│  ┌───────────────────┐  ┌─────────────┐  ┌──────────────────┐  │
│  │ DocumentController│  │SseController│  │  SecurityConfig  │  │
│  └─────────┬─────────┘  └──────┬──────┘  └──────────────────┘  │
└────────────┼────────────────────┼───────────────────────────────┘
             ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                   core/application                               │
│  ┌────────────────────┐  ┌───────────────────────────────────┐  │
│  │  Document UseCases │  │     AiPipelineService             │  │
│  │  (CRUD, Status,    │  │  (summary → tagger → embedding)  │  │
│  │   Analyze, Search) │  └──────────────┬────────────────────┘  │
│  └────────┬───────────┘                 │                       │
└───────────┼─────────────────────────────┼───────────────────────┘
            ▼                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      core/domain                                 │
│  ┌──────────┐  ┌────────────┐  ┌─────────────────────────────┐  │
│  │ Document │  │ Tag        │  │ Port Interfaces              │  │
│  │ (POJO)   │  │ (POJO)     │  │ DocumentRepo, SummaryPort,  │  │
│  │ + 상태   │  │            │  │ TaggerPort, EmbeddingPort,  │  │
│  │   전이   │  │            │  │ EventPublisher              │  │
│  └──────────┘  └────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
            ▲                             ▲
            │ implements                  │ implements
┌───────────┴─────────────┐  ┌────────────┴──────────────────────┐
│ adapters/persistence-jpa│  │ adapters/ai + adapters/event      │
│ ┌─────────────────────┐ │  │ ┌────────────┐ ┌───────────────┐ │
│ │ DocumentJpaEntity   │ │  │ │ OpenAi*    │ │ SpringEvent   │ │
│ │ DocumentJpaRepo     │ │  │ │ Adapters   │ │ Publisher     │ │
│ │ DocumentMapper      │ │  │ └────────────┘ └───────────────┘ │
│ └─────────────────────┘ │  └──────────────────────────────────┘
└─────────────────────────┘
```

---

## 7. Pinpoint 모니터링 아키텍처

```
┌──────────────────┐     ┌────────────────────┐     ┌──────────────┐
│  Spring Boot API │     │ Pinpoint Collector  │     │ Pinpoint Web │
│  + Pinpoint      │────→│  (gRPC :9994)       │     │  (:28080)    │
│    Agent (JVM)   │     │                    │     │              │
└──────────────────┘     └────────┬───────────┘     └──────┬───────┘
                                  │                        │
                                  ▼                        │
                         ┌────────────────────┐            │
                         │      HBase          │←───────────┘
                         │  (:16010, :9090)    │
                         └────────────────────┘
```

**추적 대상**:
- HTTP Controller 엔드포인트 (자동 계측)
- Service 레이어 메서드 (Spring AOP 자동)
- 외부 HTTP 호출 — AI API (RestTemplate/WebClient 자동 계측)
- JPA/JDBC 쿼리 (자동 계측)

---

## 8. Cross-Lane 협업 항목

| 항목 | BE 담당 | DevOps 담당 |
|------|---------|------------|
| Pinpoint Agent 설정 | JVM 옵션, application.yml | Dockerfile, 환경변수 |
| Pinpoint 서버 환경 | - | Docker Compose (HBase, Collector, Web) |
| 추적 대상 정의 | 메서드/레이어 범위 선정 | 대시보드 구성 |
| 운영 체크리스트 | 헬스체크, 로그 레벨 | 배포 스크립트, 모니터링 알림 |
