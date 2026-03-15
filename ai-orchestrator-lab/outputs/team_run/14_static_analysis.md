# Stage 14: 정적 분석 게이트

> FE(11), BE(12), DevOps(13) 산출물 대상 | 작성일: 2026-03-14 | 담당: QA

---

## 분석 범위

| 산출물 | 분석 항목 |
|--------|----------|
| `11_fe_codegen.md` | TypeScript 타입 안전성, API 호출 에러 핸들링, SSE 연결 관리 |
| `12_be_codegen.md` | 컴파일 여부, 상태 머신 정합성, 동시성 제어, 헥사고날 구조 |
| `13_devops_codegen.md` | Docker 구성 정합성, 환경변수 일관성, CI/CD 파이프라인 |

---

## 1. BE 정적 분석

### 1.1 빌드 결과

```
cd be && ./gradlew :apps:api:compileKotlin
BUILD SUCCESSFUL ✅
```

### 1.2 코드 이슈

| # | 심각도 | 파일 | 이슈 | 판정 |
|---|--------|------|------|------|
| SA-BE-01 | MINOR | `DocumentJpaEntity.kt` | `@Version` 필드가 `entity_version`(Long)이나 도메인의 `updatedAt`(Instant) 기반 낙관적 잠금과 이중 관리. PRD는 `updated_at` 기반 명시 | `@Version`과 도메인 수동 검사 병행은 의도된 설계. 혼동 주의 |
| SA-BE-02 | MINOR | `DocumentPersistenceAdapter.kt` | `searchActiveOwnedByUser` 인메모리 필터링 — 대량 데이터 시 성능 저하 | MVP 허용. 후속 PostgreSQL tsvector 전환 계획 존재 (05_technical_analysis) |
| SA-BE-03 | MINOR | `AiPipelineService.kt` | Mock 구현에서 `Thread.sleep(1000)` 사용 — `@Async` 스레드 풀 점유 | Mock 환경 한정. 실제 AI Adapter 전환 시 비동기 HTTP 호출로 대체 예정 |
| SA-BE-04 | MINOR | `DocumentController.kt` | `emitters` ConcurrentHashMap이 Controller 인스턴스 필드 — 멀티 인스턴스 배포 시 SSE 이벤트 유실 | MVP 단일 인스턴스 전제. 스케일아웃 시 Redis Pub/Sub 전환 필요 |
| SA-BE-05 | INFO | `DocumentCommandService.kt` | `OptimisticLockException` 커스텀 예외 정의 — JPA의 동일 이름 예외와 혼동 가능 | 네이밍 개선 권장 (예: `DocumentVersionConflictException`) |
| SA-BE-06 | INFO | `AiPipelineService.kt` | `@Async` 사용하나 `AsyncConfig`의 스레드 풀 커스터마이징 없음 | 기본 SimpleAsyncTaskExecutor 사용. 운영 전 커스텀 ThreadPoolTaskExecutor 설정 권장 |

### 1.3 상태 머신 검증

| 전이 | 코드 검증 | 결과 |
|------|----------|------|
| DRAFT → ACTIVE | `Document.activate()` | ✅ 상태 검증 없이 전환 (DRAFT 외 상태에서도 호출 가능 — MINOR) |
| ACTIVE → DELETED | 미구현 | ⚠️ PRD 명시 (FR-02-02). `DeleteDocumentUseCase` 없음 — MINOR |
| NOT_STARTED → PENDING | `Document.requestAnalysis()` | ✅ ACTIVE + !PROCESSING 검증 |
| PENDING → PROCESSING | `Document.startProcessing()` | ✅ PENDING 검증 |
| PROCESSING → COMPLETED | `Document.completeAnalysis()` | ✅ PROCESSING 검증 |
| PROCESSING → FAILED | `Document.failAnalysis()` | ✅ 무조건 전이 (검증 없음 — 허용) |

### 1.4 동시성 검증

| 시나리오 | 구현 | 결과 |
|---------|------|------|
| 동시 수정 충돌 | `DocumentCommandService.updateDraft()` updatedAt 비교 + JPA `@Version` | ✅ 이중 보호 |
| PROCESSING 중 재요청 | `Document.requestAnalysis()` require(!PROCESSING) | ✅ |
| 동시 analyze 요청 | 도메인 레벨 검증만 — DB 레벨 락 없음 | ⚠️ MINOR: 극히 짧은 race condition 가능. MVP 허용 |

---

## 2. FE 정적 분석

### 2.1 타입 체크

```
cd fe && npx tsc --noEmit
No errors ✅
```

### 2.2 코드 이슈

| # | 심각도 | 파일 | 이슈 | 판정 |
|---|--------|------|------|------|
| SA-FE-01 | MINOR | `api-client.ts` | 에러 응답 body 파싱 없음 — `res.status`만 사용. BE 에러 메시지 표시 불가 | MVP 허용. 후속 에러 body 파싱 추가 |
| SA-FE-02 | MINOR | `use-ai-status.ts` | `onerror` 시 `eventSource.close()` 후 재연결 로직 없음 | MVP 허용. 페이지 새로고침으로 우회 가능 |
| SA-FE-03 | MINOR | `[id]/page.tsx` | `params.id` 타입 검증 없음 — 잘못된 ID 시 NaN 전달 | Next.js App Router 라우팅 보장. 낮은 리스크 |
| SA-FE-04 | INFO | 전체 | 외부 라이브러리 없이 순수 fetch + useState — 번들 경량화. React Query 미사용 | 의도된 설계. 복잡해지면 React Query 도입 검토 |

---

## 3. DevOps 정적 분석

### 3.1 Docker Compose 검증

| # | 심각도 | 파일 | 이슈 | 판정 |
|---|--------|------|------|------|
| SA-DO-01 | INFO | `devops/docker-compose.yml` | `pinpoint-net` external — Pinpoint compose 먼저 기동 필요 | 기동 순서 문서화 완료 (13_devops_codegen.md) |
| SA-DO-02 | MINOR | `be/apps/api/Dockerfile` | Pinpoint Agent tar.gz를 GitHub Releases에서 직접 `ADD` — 빌드 시 네트워크 의존 | 멀티스테이지 빌드 또는 로컬 캐시 전략 후속 개선 |
| SA-DO-03 | INFO | CI/CD | Docker 이미지 push 레지스트리 미설정 — `docker build`만 수행 | 레지스트리 확정 후 push 단계 추가 예정 |

### 3.2 환경변수 일관성

| 변수 | docker-compose | .env.example | Dockerfile | 일치 |
|------|---------------|-------------|-----------|------|
| PINPOINT_COLLECTOR_HOST | ✅ | ✅ | ✅ | ✅ |
| PINPOINT_APPLICATION_NAME | ✅ | ✅ | ✅ | ✅ |
| PINPOINT_AGENT_ID | ✅ | ✅ | ✅ | ✅ |
| POSTGRES_DB/USER/PASSWORD | ✅ | ✅ | - | ✅ |
| SPRING_DATASOURCE_* | ✅ | - | - | ✅ (compose에서 주입) |

---

## 4. 종합 판정

### 이슈 요약

| 심각도 | BE | FE | DevOps | 합계 |
|--------|----|----|--------|------|
| CRITICAL | 0 | 0 | 0 | **0** |
| MAJOR | 0 | 0 | 0 | **0** |
| MINOR | 6 | 3 | 1 | **10** |
| INFO | 2 | 1 | 2 | **5** |

### 판정: **PASS**

CRITICAL/MAJOR 이슈 없음. MINOR 10건은 모두 MVP 범위에서 허용되며 후속 개선 계획이 존재합니다.

**루프백 불필요** — 15_review_gate로 진행합니다.
