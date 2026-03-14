# Stage 05: 기술 분석

> PRD v2.1 + 요구사항 분석 기준 | 작성일: 2026-03-14

---

## 1. 기술 스택 평가

### Backend

| 기술 | 버전 | 적합성 | 비고 |
|------|------|--------|------|
| Kotlin | 1.9+ | ✅ 최적 | 코루틴 기반 비동기, null safety, data class로 도메인 표현력 우수 |
| Spring Boot | 3.x | ✅ 최적 | 헥사고날 아키텍처와 자연스럽게 결합, SseEmitter 내장 |
| Spring Data JPA | 3.x | ✅ 적합 | Entity-도메인 분리는 수동 매핑 필요하나 패턴 확립됨 |
| PostgreSQL | 15+ | ✅ 적합 | Full-text search(tsvector), JSONB 지원, 벡터 확장(pgvector) 가능 |
| Gradle (Kotlin DSL) | 8.x | ✅ 적합 | 멀티 모듈 빌드 관리 |

### Frontend

| 기술 | 버전 | 적합성 | 비고 |
|------|------|--------|------|
| Next.js (App Router) | 14+ | ✅ 적합 | SSR/CSR 유연, API Route 활용 가능 |
| TypeScript | 5.x | ✅ 필수 | 타입 안전성 |
| React Query (TanStack) | 5.x | ✅ 적합 | 서버 상태 캐싱, SSE와 조합 용이 |
| Tailwind CSS | 3.x | ✅ 적합 | 빠른 UI 구성 |

### DevOps / Infra

| 기술 | 버전 | 적합성 | 비고 |
|------|------|--------|------|
| Docker Compose | 2.x | ✅ 적합 | 로컬 개발 + Pinpoint 환경 통합 |
| Pinpoint | 2.5.4 | ✅ 적합 | Java 에이전트 기반 APM, HBase 의존 |

---

## 2. 통합 포인트

### 2.1 AI 서비스 연동 (외부 의존성)

```
[BE Application Service]
    │
    ├─→ AiPort (Port 인터페이스)
    │     ├─→ SummaryPort.summarize(content) → String
    │     ├─→ TaggerPort.extractTags(content) → List<Tag>
    │     └─→ EmbeddingPort.embed(content) → Vector
    │
    └─→ [Adapter 구현]
          ├─→ OpenAiSummaryAdapter (GPT-4)
          ├─→ OpenAiTaggerAdapter (GPT-4)
          └─→ OpenAiEmbeddingAdapter (text-embedding-3-small)
```

- **리스크**: AI 서비스 모델 미확정 (모호성 A-09)
- **완화**: Port 인터페이스로 추상화하여 모델/서비스 교체 가능 구조 확보
- **Mock 전략**: `MockSummaryAdapter` 등으로 AI 없이 파이프라인 흐름 검증 가능

### 2.2 SSE 연동

```
[Client] ←──SSE──── [SseEmitter] ←── [ApplicationEventPublisher]
                                           ↑
                                    [AiPipelineService]
                                    (각 단계 완료 시 이벤트 발행)
```

- Spring `SseEmitter`로 구현, 클라이언트당 1개 연결
- 이벤트 타입: `AI_STATUS_CHANGED`, `STAGE_COMPLETED`, `PIPELINE_COMPLETED`, `PIPELINE_FAILED`
- 연결 타임아웃: 5분 (재연결은 클라이언트 책임)

### 2.3 Pinpoint 연동

```
[Spring Boot API] ──JVM Agent──→ [Pinpoint Collector:9994] ──→ [HBase] ←── [Pinpoint Web:28080]
```

- BE 앱에 `-javaagent` JVM 옵션으로 Pinpoint Agent 부착
- 추적 대상: Controller → Service → Repository (Spring 자동 계측)
- 외부 HTTP 호출 (AI 서비스) 자동 추적

---

## 3. 기술적 고려사항

### 3.1 비동기 파이프라인 설계

| 선택지 | 장점 | 단점 | 결론 |
|--------|------|------|------|
| `ApplicationEventPublisher` + `@Async` | 간단, 외부 의존 없음, MVP 적합 | 서버 재시작 시 유실 | **MVP 채택** |
| Message Queue (RabbitMQ/Kafka) | 메시지 보장, 확장성 | 인프라 복잡도 증가 | 후속 고려 |
| Kotlin Coroutines | 경량, 취소 지원 | Spring 통합 주의 필요 | 내부 구현에 활용 |

### 3.2 검색 구현 전략

| 선택지 | 장점 | 단점 | 결론 |
|--------|------|------|------|
| PostgreSQL Full-text (tsvector) | 추가 인프라 불필요, 한국어 지원 가능 | 대규모 시 한계 | **MVP 채택** |
| Elasticsearch | 고성능 전문 검색 | 인프라 복잡도 | 후속 고려 |
| pgvector + 시맨틱 검색 | embedding 활용 | 정확도 튜닝 필요 | 후속 고려 |

### 3.3 낙관적 잠금 구현

```kotlin
// Domain
data class Document(
    val id: UUID,
    val updatedAt: Instant,  // 버전 역할
    ...
)

// JPA Entity
@Entity
class DocumentJpaEntity(
    @Version
    val updatedAt: Instant,  // JPA @Version으로 자동 관리
    ...
)
```

- `@Version` + `updated_at` 필드로 JPA 레벨에서 자동 충돌 감지
- `OptimisticLockException` 발생 시 409 Conflict 응답

### 3.4 상태 머신 안전성

```
[Document Status]
DRAFT ──(activate)──→ ACTIVE ──(delete)──→ DELETED
  │                                           ↑
  └──────────(delete)─────────────────────────┘

[AI Status]  (ACTIVE 문서에서만 전이)
NOT_STARTED ──(analyze)──→ PENDING ──(pick up)──→ PROCESSING
                                                      │
                                              ┌───────┴───────┐
                                              ↓               ↓
                                          COMPLETED        FAILED
```

- 상태 전이 규칙은 도메인 POJO에서 `fun activate()`, `fun requestAnalysis()` 등 메서드로 강제
- 잘못된 전이 시 `IllegalStateException` → 400 Bad Request

---

## 4. 외부 의존성 정리

| 의존성 | 용도 | 대체 가능 여부 | 리스크 수준 |
|--------|------|--------------|------------|
| AI API (OpenAI 등) | 요약/태깅/임베딩 | Port 추상화로 교체 가능 | 중간 |
| PostgreSQL | 데이터 저장 + 검색 | H2 (테스트) | 낮음 |
| Pinpoint | APM 모니터링 | 선택적 (에이전트 미부착 시 동작 영향 없음) | 낮음 |

---

## 5. 리스크 요약

| # | 리스크 | 영향도 | 완화 전략 |
|---|--------|--------|----------|
| R-01 | AI 서비스 모델/API 미확정 | 높음 | Port 인터페이스 + Mock Adapter |
| R-02 | 인증 방식 미확정 | 높음 | JWT 기본 구현, 교체 가능 구조 |
| R-03 | 비동기 파이프라인 메시지 유실 | 중간 | MVP에서 허용, MQ 전환 준비 |
| R-04 | SSE 연결 안정성 | 중간 | 클라이언트 재연결 + 상태 폴링 fallback |
| R-05 | Full-text search 한국어 형태소 | 낮음 | pg_bigm 확장 또는 후속 ES 도입 |
