# Stage 07: 병렬 Worktree 계획

> 06_architecture.md 기준 | 작성일: 2026-03-14

---

## 1. 병렬 작업 전략 개요

FE, BE, DevOps 3개 lane을 독립 git worktree에서 병렬 개발한다.
각 lane은 서로 블로킹 없이 작업하며, 인터페이스(API 스펙, 환경변수)를 먼저 합의한 뒤 구현에 진입한다.

---

## 2. Lane별 Worktree 정의

### 2.1 BE Lane

| 항목 | 값 |
|------|-----|
| 브랜치명 | `feat/be-ai-wiki-api` |
| Worktree 경로 | `worktrees/be-ai-wiki-api/` |
| 작업 범위 | core/domain, core/application, adapters/persistence-jpa, adapters/ai, apps/api |
| 담당 | BE Engineer |

**작업 순서**:
1. `core/domain` — Document, Tag, DocumentRevision POJO + 상태 머신 + Port 인터페이스
2. `adapters/persistence-jpa` — JPA Entity, Mapper, Repository 구현
3. `core/application` — UseCase 구현 (CRUD, Status, Analyze, Search)
4. `adapters/ai` — AI Adapter (Mock 우선, OpenAI 후속)
5. `apps/api` — Controller, SSE, Security 설정
6. 통합 테스트

### 2.2 FE Lane

| 항목 | 값 |
|------|-----|
| 브랜치명 | `feat/fe-ai-wiki-ui` |
| Worktree 경로 | `worktrees/fe-ai-wiki-ui/` |
| 작업 범위 | fe/ (Next.js App Router) |
| 담당 | FE Engineer |

**작업 순서**:
1. API Client 정의 (BE API 스펙 기반, Mock 응답으로 선행 개발)
2. 문서 목록/상세 페이지
3. 문서 생성/수정 폼 (Markdown 에디터)
4. 상태 전환 UI (DRAFT → ACTIVE)
5. AI 분석 요청 + SSE 상태 표시
6. 검색 UI + 태그 필터

### 2.3 DevOps Lane

| 항목 | 값 |
|------|-----|
| 브랜치명 | `feat/devops-infra` |
| Worktree 경로 | `worktrees/devops-infra/` |
| 작업 범위 | devops/ (Docker Compose, CI/CD, Pinpoint) |
| 담당 | DevOps Engineer |

**작업 순서**:
1. 개발 환경 Docker Compose (PostgreSQL, API)
2. Pinpoint 환경 구성 (HBase, Collector, Web)
3. BE Dockerfile + Pinpoint Agent 연동
4. CI/CD 파이프라인 (빌드, 테스트, 이미지 빌드)
5. 환경변수 관리 (.env, secrets)

---

## 3. 병합 순서

```
Phase 1: 기반 구조 (독립 병합 가능)
  ├─ feat/be-ai-wiki-api   ──→ main  (core/domain + adapters + apps/api)
  ├─ feat/devops-infra      ──→ main  (Docker Compose + Pinpoint)
  └─ feat/fe-ai-wiki-ui     ──→ main  (FE 앱)

Phase 2: 통합 검증
  └─ main에서 FE ↔ BE ↔ DevOps 통합 테스트

Phase 3: Cross-lane 조정
  └─ Pinpoint Agent ↔ Collector 연동 검증 (BE + DevOps 협업)
```

**병합 규칙**:
- BE를 먼저 병합 (API 스펙이 기준)
- DevOps를 두 번째로 병합 (인프라 환경 준비)
- FE를 마지막 병합 (API 연동 최종 확인)
- 충돌 최소화를 위해 각 lane은 자신의 디렉토리만 수정

---

## 4. 인터페이스 선합의 항목

병렬 작업 전 아래 항목을 먼저 확정하여 lane 간 블로킹을 방지한다.

### 4.1 API 스펙 (BE → FE)

| 엔드포인트 | Method | 요청/응답 형식 | 비고 |
|-----------|--------|--------------|------|
| `/api/v1/documents` | POST | `{title, content, parentId?}` → `Document` | DRAFT 생성 |
| `/api/v1/documents/{id}` | GET | → `Document` | 단건 조회 |
| `/api/v1/documents` | GET | `?page&size` → `Page<Document>` | 목록 (본인만) |
| `/api/v1/documents/{id}` | PUT | `{title, content, updatedAt}` → `Document` | 낙관적 잠금 |
| `/api/v1/documents/{id}` | DELETE | → 204 | DELETED 전환 |
| `/api/v1/documents/{id}/status` | PATCH | `{status: "ACTIVE"}` → `Document` | 상태 전환 |
| `/api/v1/documents/{id}/analyze` | POST | → 202 Accepted | ACTIVE만, 409 if PROCESSING |
| `/api/v1/documents/{id}/events` | GET | SSE stream | AI 상태 이벤트 |
| `/api/v1/documents/search` | GET | `?q&tag` → `Page<Document>` | 전문 검색 |

### 4.2 SSE 이벤트 형식 (BE → FE)

```json
{"type": "AI_STATUS_CHANGED", "aiStatus": "PROCESSING", "ts": "..."}
{"type": "STAGE_COMPLETED", "stage": "summary", "ts": "..."}
{"type": "STAGE_COMPLETED", "stage": "tagger", "ts": "..."}
{"type": "STAGE_COMPLETED", "stage": "embedding", "ts": "..."}
{"type": "PIPELINE_COMPLETED", "ts": "..."}
{"type": "PIPELINE_FAILED", "error": "...", "ts": "..."}
```

### 4.3 환경변수 (DevOps → BE)

| 변수명 | 용도 | 기본값 |
|--------|------|--------|
| `SPRING_DATASOURCE_URL` | PostgreSQL 연결 | `jdbc:postgresql://localhost:5432/aiwiki` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | `aiwiki` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `aiwiki` |
| `OPENAI_API_KEY` | AI 서비스 키 | - |
| `PINPOINT_COLLECTOR_HOST` | Pinpoint Collector | `pinpoint-collector` |
| `PINPOINT_APPLICATION_NAME` | Pinpoint 앱 이름 | `ai-wiki-api` |
| `PINPOINT_AGENT_ID` | Pinpoint 에이전트 ID | `api-001` |

---

## 5. Cross-Lane 협업 항목

### 5.1 Pinpoint 모니터링 (BE + DevOps 공동)

| 작업 | BE 담당 | DevOps 담당 | 상태 |
|------|---------|------------|------|
| Pinpoint Agent JVM 옵션 정의 | ✅ JAVA_TOOL_OPTIONS 설정 | Dockerfile에 반영 | 완료 |
| Pinpoint 서버 환경 | - | Docker Compose 구성 | 완료 |
| 추적 대상 레이어 합의 | Controller + Service + 외부 호출 | 대시보드 확인 | 대기 |
| 운영 체크리스트 | 헬스체크 엔드포인트 | 알림 설정 | 대기 |

### 5.2 AI 서비스 연동 (BE + DevOps)

| 작업 | BE 담당 | DevOps 담당 |
|------|---------|------------|
| Port 인터페이스 정의 | SummaryPort, TaggerPort, EmbeddingPort | - |
| Mock Adapter | MockSummaryAdapter 등 | - |
| API Key 관리 | application.yml 참조 | .env / secrets 관리 |

---

## 6. 위험 요소 및 완화

| 리스크 | 영향 | 완화 |
|--------|------|------|
| BE API 스펙 변경 시 FE 재작업 | 중간 | OpenAPI 스펙 공유, FE는 Mock 기반 선행 |
| 환경변수 불일치 | 낮음 | `.env.example` 공유, PR 리뷰 시 확인 |
| Pinpoint 연동 지연 | 낮음 | Agent 미부착 시 앱 동작에 영향 없음 |
| 병합 충돌 | 낮음 | lane별 디렉토리 분리로 충돌 최소화 |
