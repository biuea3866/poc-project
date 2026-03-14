# Stage 02: 요구사항 분석

> PRD v2.1 기준 | 분석일: 2026-03-14

---

## 1. 기능 요구사항 (Functional Requirements)

### FR-01: 문서 CRUD

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-01-01 | Markdown 문서를 생성할 수 있다 (DRAFT 상태로 생성) | P0 | |
| FR-01-02 | 문서를 조회할 수 있다 (단건/목록) | P0 | |
| FR-01-03 | 문서를 수정할 수 있다 | P0 | ACTIVE 문서 수정 시 revision 생성 |
| FR-01-04 | 문서를 삭제할 수 있다 (DELETED 상태 전환) | P0 | |
| FR-01-05 | 계층형 문서 구조를 지원한다 (부모-자식 관계) | P0 | |

### FR-02: 문서 상태 관리

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-02-01 | DRAFT → ACTIVE 상태 전환이 가능하다 | P0 | |
| FR-02-02 | 문서 → DELETED 상태 전환이 가능하다 | P0 | |
| FR-02-03 | ACTIVE 전환 시 AI 파이프라인이 비동기로 트리거된다 | P0 | |
| FR-02-04 | PROCESSING 중 재요청 시 409 Conflict를 반환한다 | P0 | 동시성 제어 |
| FR-02-05 | `updated_at` 기반 낙관적 잠금을 적용한다 | P0 | |

### FR-03: AI 파이프라인

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-03-01 | summary 단계: 문서 본문 요약을 생성한다 | P0 | |
| FR-03-02 | tagger 단계: 자동 태그를 추출한다 | P0 | |
| FR-03-03 | embedding 단계: 벡터 임베딩을 생성한다 | P0 | |
| FR-03-04 | 파이프라인은 summary → tagger → embedding 순서로 실행된다 | P0 | 순차 실행 |
| FR-03-05 | AI 상태를 NOT_STARTED → PENDING → PROCESSING → COMPLETED/FAILED로 관리한다 | P0 | |
| FR-03-06 | analyze 요청은 ACTIVE 문서에서만 허용된다 | P0 | |

### FR-04: SSE (Server-Sent Events)

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-04-01 | AI 파이프라인 처리 상태를 SSE로 실시간 전달한다 | P0 | |
| FR-04-02 | 각 단계(summary/tagger/embedding)의 시작/완료/실패 이벤트를 전송한다 | P1 | 모호성 A-18 참고 |

### FR-05: 검색

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-05-01 | 제목 기준 검색이 가능하다 | P0 | |
| FR-05-02 | 본문 기준 검색이 가능하다 | P0 | |
| FR-05-03 | 태그 기준 검색이 가능하다 | P0 | |
| FR-05-04 | 검색 대상은 본인의 ACTIVE 문서로 한정한다 | P0 | |

### FR-06: Revision

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| FR-06-01 | ACTIVE 문서 수정 시 revision을 생성한다 | P0 | |
| FR-06-02 | revision 이력을 조회할 수 있다 | P1 | 모호성 A-11 참고 |

---

## 2. 비기능 요구사항 (Non-Functional Requirements)

### NFR-01: 아키텍처

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-01-01 | 헥사고날 아키텍처를 따른다 | P0 | Port/Adapter 패턴 |
| NFR-01-02 | JPA Entity와 도메인 POJO를 분리한다 | P0 | |
| NFR-01-03 | 객체지향, 클린 코드, 디자인 패턴 원칙을 준수한다 | P0 | |

### NFR-02: 성능

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-02-01 | AI 파이프라인은 비동기로 실행되어 사용자 요청을 블로킹하지 않는다 | P0 | |
| NFR-02-02 | 검색 응답 시간은 합리적 수준을 유지한다 | P1 | 구체적 SLA 미정 |

### NFR-03: 동시성/일관성

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-03-01 | 낙관적 잠금으로 동시 수정 충돌을 방지한다 | P0 | `updated_at` 기반 |
| NFR-03-02 | PROCESSING 중 중복 요청을 방지한다 (409 Conflict) | P0 | |

### NFR-04: 운영/모니터링

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-04-01 | Pinpoint 에이전트 연동 가능 구조를 제공한다 | P0 | BE 담당 |
| NFR-04-02 | Pinpoint 수집/조회 환경을 구성한다 | P0 | DevOps 담당 |
| NFR-04-03 | 추적 대상, 배포 방식, 환경 변수, 운영 체크리스트를 BE/DevOps 협업으로 정의한다 | P0 | |

### NFR-05: 보안

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-05-01 | 사용자 인증/인가를 적용한다 | P0 | 방식 미확정 (모호성 A-19) |
| NFR-05-02 | 문서 접근은 소유자로 제한한다 | P0 | |

### NFR-06: 코드 품질

| ID | 요구사항 | 우선순위 | 비고 |
|----|---------|----------|------|
| NFR-06-01 | 보안, 상태 머신, 동시성에 대한 리뷰를 수행한다 | P0 | 최종 단계 |
| NFR-06-02 | 구현 티켓은 독립 PR 단위로 분해한다 | P0 | |

---

## 3. 도메인 모델 (초안)

### 핵심 엔티티

```
Document
├── id: UUID
├── title: String
├── content: String (Markdown)
├── status: DocumentStatus (DRAFT | ACTIVE | DELETED)
├── aiStatus: AiStatus (NOT_STARTED | PENDING | PROCESSING | COMPLETED | FAILED)
├── parentId: UUID (nullable, 계층 구조)
├── ownerId: UUID
├── summary: String (nullable, AI 생성)
├── tags: List<Tag> (AI 생성)
├── embedding: Vector (AI 생성)
├── revision: Integer
├── createdAt: Timestamp
├── updatedAt: Timestamp
└── deletedAt: Timestamp (nullable)

Tag
├── id: UUID
├── name: String
└── documentId: UUID

DocumentRevision
├── id: UUID
├── documentId: UUID
├── revisionNumber: Integer
├── content: String
├── createdAt: Timestamp
└── createdBy: UUID
```

### 상태 전이 다이어그램

```
[Document Status]
DRAFT ──→ ACTIVE ──→ DELETED
              │
              └──→ (수정 시 revision 생성)

[AI Status]
NOT_STARTED ──→ PENDING ──→ PROCESSING ──→ COMPLETED
                    │            │
                    └────────────└──→ FAILED
```

---

## 4. API 엔드포인트 (초안)

| Method | Endpoint | 설명 | 비고 |
|--------|----------|------|------|
| POST | `/api/v1/documents` | 문서 생성 (DRAFT) | |
| GET | `/api/v1/documents/{id}` | 문서 단건 조회 | |
| GET | `/api/v1/documents` | 문서 목록 조회 | 본인 문서만 |
| PUT | `/api/v1/documents/{id}` | 문서 수정 | 낙관적 잠금 적용 |
| DELETE | `/api/v1/documents/{id}` | 문서 삭제 (DELETED 전환) | |
| PATCH | `/api/v1/documents/{id}/status` | 문서 상태 전환 | DRAFT→ACTIVE |
| POST | `/api/v1/documents/{id}/analyze` | AI 분석 요청 | ACTIVE만 허용, PROCESSING 시 409 |
| GET | `/api/v1/documents/{id}/events` | SSE 이벤트 스트림 | AI 처리 상태 |
| GET | `/api/v1/documents/search` | 문서 검색 | 제목/본문/태그 |
| GET | `/api/v1/documents/{id}/revisions` | revision 이력 조회 | P1 |

---

## 5. 요구사항-모호성 매핑

아래 요구사항은 [01_ambiguity.md](./01_ambiguity.md)의 모호성 항목이 해소된 후 확정되어야 한다.

| 요구사항 ID | 관련 모호성 | 확정 필요 사항 |
|------------|-----------|---------------|
| FR-02-01 | A-01 | 전환 조건 확정 |
| FR-02-02 | A-02, A-03 | 삭제 전환 규칙 확정 |
| FR-03-05 | A-06, A-07 | 실패/부분실패 복구 전략 |
| FR-03-03 | A-08 | embedding 대상 범위 |
| FR-03-01~03 | A-09 | AI 서비스/모델 선정 |
| FR-05-01~03 | A-13 | 검색 방식 확정 |
| FR-06-01 | A-10, A-12 | revision 생성 조건 및 AI 재실행 정책 |
| NFR-05-01 | A-19 | 인증 방식 확정 |
