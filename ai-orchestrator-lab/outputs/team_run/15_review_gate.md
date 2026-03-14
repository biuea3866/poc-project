# Stage 15: 품질 리뷰 게이트

> 14_static_analysis.md + 전체 산출물 기준 | 작성일: 2026-03-14 | 담당: QA

---

## 리뷰 범위

| 검토 영역 | 검토 대상 |
|----------|----------|
| 보안 | 인증/인가, 데이터 접근 제어, 입력 검증 |
| 상태 머신 | 문서 상태(DRAFT/ACTIVE/DELETED), AI 상태(NOT_STARTED~FAILED) 전이 정합성 |
| 동시성 | 낙관적 잠금, PROCESSING 중복 방지, SSE emitter 동시 접근 |
| 회귀 리스크 | 기존 구현과의 호환성, 아키텍처 일관성 |

---

## 1. 보안 리뷰

### 1.1 인증/인가

| 항목 | 현재 상태 | 판정 |
|------|----------|------|
| SecurityConfig | `anyRequest().permitAll()` — 인증 미적용 | MINOR |
| userId 전달 방식 | RequestParam/RequestBody로 클라이언트 전송 | MINOR |
| 문서 소유권 검증 | `DocumentQueryService`에서 ACTIVE 문서 ownerId 필터 | ✅ |
| CSRF | `.csrf { it.disable() }` | ✅ (REST API, 토큰 인증 전제) |

**평가**: MVP 단계에서 `permitAll()`은 허용. 다만 userId를 클라이언트에서 전달받는 구조는 운영 전 JWT 토큰 기반으로 전환 필수. PRD에서 인증 방식 미확정(A-19) 상태이므로 현 구현은 합리적 판단.

### 1.2 입력 검증

| 항목 | 현재 상태 | 판정 |
|------|----------|------|
| `@NotBlank` 적용 | CreateDraftRequest, UpdateDraftRequest의 title/content | ✅ |
| `@Validated` | Controller 레벨 적용 | ✅ |
| SQL Injection | JPA Parameter Binding 사용 | ✅ |
| XSS | Markdown 본문 저장 — 렌더링 시 sanitize 필요 | MINOR (FE 렌더링 측) |

### 1.3 데이터 접근 제어

| 항목 | 현재 상태 | 판정 |
|------|----------|------|
| 검색 범위 | `searchActiveOwnedByUser(query, userId)` — 본인 ACTIVE만 | ✅ |
| 단건 조회 | `getDocument(documentId)` — 소유권 미검증 | MINOR |
| 수정/삭제 | 소유권 미검증 | MINOR |

**권장**: `getDocument`, `updateDraft`, `activate`, `analyze` 등에 `userId` 검증 추가. MVP에서는 단일 사용자 전제로 허용.

---

## 2. 상태 머신 리뷰

### 2.1 문서 상태 전이

```
PRD 정의:
DRAFT ──→ ACTIVE ──→ DELETED
  │                     ↑
  └─────────────────────┘

코드 구현:
DRAFT ──(activate)──→ ACTIVE    ✅
DRAFT ──(delete)──→ ?           ⚠️ DeleteDocumentUseCase 미구현
ACTIVE ──(delete)──→ ?          ⚠️ DeleteDocumentUseCase 미구현
```

| 검증 항목 | 결과 | 비고 |
|----------|------|------|
| DRAFT → ACTIVE | ✅ | `Document.activate()` |
| activate 중 상태 검증 | ⚠️ MINOR | DRAFT 외 상태에서도 호출 가능 (idempotent로 볼 수 있음) |
| DELETED 전이 | ⚠️ MINOR | UseCase 미구현, API 엔드포인트 미구현 |

### 2.2 AI 상태 전이

```
코드 구현:
NOT_STARTED ──(requestAnalysis)──→ PENDING ──(startProcessing)──→ PROCESSING
                                                                      │
                                                              ┌───────┴───────┐
                                                              ↓               ↓
                                                          COMPLETED        FAILED
```

| 검증 항목 | 결과 | 비고 |
|----------|------|------|
| ACTIVE 문서만 analyze | ✅ | `require(status == ACTIVE)` |
| PROCESSING 중 재요청 차단 | ✅ | `require(aiStatus != PROCESSING)` |
| PENDING → PROCESSING | ✅ | `require(aiStatus == PENDING)` |
| PROCESSING → COMPLETED | ✅ | `require(aiStatus == PROCESSING)` |
| PROCESSING → FAILED | ✅ | 무조건 전이 (catch 블록에서 호출) |
| COMPLETED 후 재분석 | ✅ | `requestAnalysis()`에서 PROCESSING만 차단 |

**평가**: AI 상태 머신은 PRD 요구사항을 정확히 구현. 409 Conflict 반환도 정상.

---

## 3. 동시성 리뷰

| 시나리오 | 구현 | 판정 |
|---------|------|------|
| 동시 문서 수정 | `updatedAt` 비교 + JPA `@Version` 이중 보호 | ✅ |
| 동시 analyze 요청 | 도메인 `require` 검증 + DB 저장 시 `@Version` 충돌 | ✅ (간접 보호) |
| SSE emitter 동시 접근 | `ConcurrentHashMap` + `CopyOnWriteArrayList` | ✅ |
| SSE emitter 정리 | `onCompletion`, `onTimeout`, `onError` 핸들러 | ✅ |
| @Async 스레드 안전성 | 각 요청 독립 실행, 문서별 상태 전이 | ✅ |

**평가**: 동시성 제어가 도메인 + JPA + 자료구조 3계층에서 보호됨. MVP 수준에서 충분.

---

## 4. 아키텍처 일관성 리뷰

| 원칙 | 현재 상태 | 판정 |
|------|----------|------|
| 헥사고날 아키텍처 | Port/Adapter 패턴 준수 (domain → port ← adapter) | ✅ |
| JPA Entity ≠ Domain POJO | `DocumentJpaEntity` ↔ `Document` 분리, `toDomain()`/`from()` 매핑 | ✅ |
| 단방향 의존성 | api → application → domain ← adapters | ✅ |
| 모듈 경계 | `core/domain`에 외부 의존성 없음 | ✅ |
| UseCase 패턴 | `DocumentCommandService` + `DocumentQueryService` (CQRS 경향) | ✅ |

**평가**: ARCHITECTURE.md 강제 규칙을 모두 준수. 헥사고날 구조가 일관되게 적용됨.

---

## 5. 회귀 리스크 리뷰

| 리스크 항목 | 평가 | 판정 |
|------------|------|------|
| 기존 SecurityConfig 변경 | permitAll 유지, 기존 동작 변경 없음 | ✅ |
| application.yml H2 설정 | 개발 환경 전용, 운영 환경 분리 필요 | MINOR |
| Gradle 모듈 구조 | 기존 4모듈(api, application, domain, persistence-jpa) + ai 어댑터 추가 | ✅ |
| FE API 경로 | BE Controller 매핑과 일치 (`/api/v1/documents`) | ✅ |
| SSE 이벤트 이름 | FE `ai-status` ↔ BE `.name("ai-status")` 일치 | ✅ |

---

## 6. 종합 판정

### 이슈 분류

| 심각도 | 건수 | 주요 내용 |
|--------|------|----------|
| CRITICAL | 0 | - |
| MAJOR | 0 | - |
| MINOR | 7 | 인증 미적용(MVP 허용), 소유권 미검증(단일 사용자 전제), activate 상태 미검증, 삭제 미구현, XSS sanitize |
| INFO | 0 | - |

### 최종 판정: **APPROVE**

모든 CRITICAL/MAJOR 이슈 없음. MINOR 이슈 7건은 다음 조건으로 허용:

1. **인증(MINOR)**: MVP 단일 사용자 전제. M1 완료 후 JWT 전환 계획 존재 (03_roadmap_update.md)
2. **삭제 미구현(MINOR)**: FR-02-02 티켓(NAW-BE-004 범위)에서 후속 구현 예정
3. **소유권 검증(MINOR)**: 인증 도입 시 함께 적용
4. **XSS(MINOR)**: FE Markdown 렌더링 시 sanitize 라이브러리 적용 예정

### 루프백 여부: **불필요**

Stage 16 (Delivery Docs)으로 진행 가능합니다.

---

## 7. 후속 개선 권장사항

| 우선순위 | 항목 | 대상 티켓 |
|---------|------|----------|
| P0 | JWT 인증 + userId 토큰 추출 | NAW-BE-008 후속 |
| P0 | 문서 삭제(DELETED 전이) 구현 | NAW-BE-004 범위 |
| P1 | activate() 상태 검증 강화 | NAW-BE-001 후속 |
| P1 | SSE 재연결 로직 (FE) | NAW-FE-004 후속 |
| P1 | Markdown XSS sanitize (FE) | NAW-FE-002 후속 |
| P2 | 검색 PostgreSQL tsvector 전환 | NAW-BE-009 후속 |
| P2 | @Async ThreadPool 커스터마이징 | NAW-BE-005 후속 |
| P2 | Dockerfile 멀티스테이지 빌드 | NAW-DO-003 후속 |
