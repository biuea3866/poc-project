# AI Wiki v1.0 MVP — 최종 운영 릴리즈 문서

> 작성일: 2026-03-14 | 버전: v1.0 MVP | 프로젝트: AI Wiki (NAW)

---

## 1. 릴리즈 개요

| 항목 | 내용 |
|------|------|
| 제품명 | AI Wiki |
| 버전 | v1.0 MVP |
| 릴리즈 날짜 | 2026-03-14 |
| 아키텍처 | 헥사고날 아키텍처 (Port/Adapter 패턴) |
| BE 기술 스택 | Kotlin, Spring Boot 3, JPA, H2 (MVP) → PostgreSQL (운영) |
| FE 기술 스택 | Next.js 14 (App Router), TypeScript 5 |
| 모니터링 | Pinpoint 2.5.4 (HBase + Collector + Web) |
| CI/CD | GitHub Actions (BE/FE 분리 워크플로우) |

---

## 2. 아키텍처 요약

### 2.1 헥사고날 아키텍처 구조

```
apps/api (Spring Boot)     ← Inbound Adapter (Controller, SSE, Config)
    ↓
core/application           ← UseCase (CQRS: CommandService + QueryService)
    ↓
core/domain                ← 순수 도메인 POJO (외부 의존성 ZERO)
    ↑
adapters/*                 ← Outbound Adapter (persistence-jpa, ai, event)
```

### 2.2 의존성 규칙

- `core/domain`: 다른 모듈을 참조하지 않음
- `core/application`: `core/domain`만 참조
- `adapters/*`: `core/domain`의 Port 인터페이스를 구현
- `apps/api`: `core/application`과 `adapters/*`를 DI로 조립

### 2.3 DB 마이그레이션 계획

| 단계 | 환경 | DB | 비고 |
|------|------|-----|------|
| MVP (현재) | 개발 | H2 (In-Memory) | `ddl-auto: create-drop` |
| M1 완료 후 | 스테이징 | PostgreSQL 15 | Docker Compose 구성 완료 |
| 운영 | 프로덕션 | PostgreSQL 15 | Flyway 마이그레이션 적용 예정 |

---

## 3. 구현 완료 기능 목록

### 3.1 BE (Backend) — 10개 티켓 완료

| 티켓 ID | 기능 | 상태 |
|---------|------|------|
| NAW-BE-001 | Document 도메인 POJO + 상태 머신 (DRAFT/ACTIVE/DELETED, AI 상태 전이) | ✅ 완료 |
| NAW-BE-002 | Outbound Port 인터페이스 5개 (DocumentRepository, SummaryPort, TaggerPort, EmbeddingPort, DocumentRevisionRepository) | ✅ 완료 |
| NAW-BE-003 | JPA Entity + Repository + Mapper (낙관적 잠금 @Version 포함) | ✅ 완료 |
| NAW-BE-004 | 문서 CRUD UseCase (생성/조회/수정, 낙관적 잠금 충돌 감지) | ✅ 완료 |
| NAW-BE-005 | AI 파이프라인 비동기 실행 (@Async, summary→tagger→embedding) | ✅ 완료 |
| NAW-BE-006 | Mock AI Adapter 3종 (Summary, Tagger, Embedding) | ✅ 완료 |
| NAW-BE-007 | SSE 엔드포인트 (SseEmitter, 실시간 AI 상태 스트리밍) | ✅ 완료 |
| NAW-BE-008 | REST Controller + Security 설정 (JWT 준비, MVP permitAll) | ✅ 완료 |
| NAW-BE-009 | 문서 검색 (제목/본문/태그, 인메모리 필터링 → 후속 tsvector 전환) | ✅ 완료 |
| NAW-BE-010 | Revision 자동 생성 (ACTIVE 문서 수정 시) | ✅ 완료 |

### 3.2 FE (Frontend) — 5개 티켓 완료

| 티켓 ID | 기능 | 상태 |
|---------|------|------|
| NAW-FE-001 | API Client + TypeScript 타입 정의 (fetch 기반, 6개 API 함수) | ✅ 완료 |
| NAW-FE-002 | 문서 목록/상세 페이지 (Markdown 렌더링, 상태 뱃지) | ✅ 완료 |
| NAW-FE-003 | 문서 생성/수정 폼 (DRAFT 생성, 낙관적 잠금 충돌 알림) | ✅ 완료 |
| NAW-FE-004 | 상태 전환 + AI 분석 요청 + SSE 실시간 상태 표시 | ✅ 완료 |
| NAW-FE-005 | 검색 UI + 태그 필터 (debounce 300ms) | ✅ 완료 |

### 3.3 DevOps — 4개 티켓 완료

| 티켓 ID | 기능 | 상태 |
|---------|------|------|
| NAW-DO-001 | 개발 환경 Docker Compose (PostgreSQL 15 + API) | ✅ 완료 |
| NAW-DO-002 | Pinpoint 모니터링 환경 (HBase + Collector + Web) | ✅ 완료 |
| NAW-DO-003 | API Dockerfile + Pinpoint Agent 통합 | ✅ 완료 |
| NAW-DO-004 | CI/CD 파이프라인 (GitHub Actions, BE/FE 분리) | ✅ 완료 |

### 3.4 Cross-Lane — 1개 티켓 완료

| 티켓 ID | 기능 | 상태 |
|---------|------|------|
| NAW-CL-001 | Pinpoint 추적 범위 + 운영 체크리스트 | ✅ 완료 |

---

## 4. API 엔드포인트 목록

| Method | Endpoint | 설명 | 비고 |
|--------|----------|------|------|
| POST | `/api/v1/documents` | 문서 생성 (DRAFT) | `CreateDraftRequest` |
| GET | `/api/v1/documents/{id}` | 문서 단건 조회 | |
| GET | `/api/v1/documents` | 문서 목록/검색 조회 | `?userId=&q=` |
| PATCH | `/api/v1/documents/{id}` | 문서 수정 | 낙관적 잠금 (`expectedUpdatedAt`) |
| POST | `/api/v1/documents/{id}/activate` | DRAFT → ACTIVE 전환 | `?userId=` |
| POST | `/api/v1/documents/{id}/analyze` | AI 분석 요청 | ACTIVE만 허용, PROCESSING 시 409 |
| GET | `/api/v1/documents/{id}/ai-status/stream` | SSE AI 상태 스트림 | `text/event-stream` |
| GET | `/api/v1/documents/health` | 헬스체크 | |

---

## 5. 배포 절차

### 5.1 사전 준비

```bash
# 환경 변수 설정
cp devops/.env.example devops/.env
cp devops/pinpoint/.env.example devops/pinpoint/.env
```

### 5.2 기동 순서

```bash
# Step 1: Pinpoint 모니터링 환경 기동
cd devops/pinpoint && docker compose up -d

# Step 2: 개발 환경 기동 (PostgreSQL + API)
cd devops && docker compose up -d

# Step 3: FE 개발 서버 기동
cd fe && npm ci && npm run dev
```

### 5.3 검증

| 항목 | URL | 기대 결과 |
|------|-----|----------|
| API Health | `http://localhost:8080/api/v1/documents/health` | `{"service":"ai-wiki-api"}` |
| H2 Console | `http://localhost:8080/h2-console` | DB 콘솔 접속 |
| Pinpoint Web | `http://localhost:28080` | APM 대시보드 |
| FE | `http://localhost:3000` | AI Wiki 홈페이지 |

### 5.4 환경 변수 목록

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `POSTGRES_DB` | `aiwiki` | PostgreSQL DB명 |
| `POSTGRES_USER` | `aiwiki` | PostgreSQL 사용자 |
| `POSTGRES_PASSWORD` | `aiwiki` | PostgreSQL 비밀번호 |
| `API_PORT` | `8080` | API 서버 포트 |
| `PINPOINT_COLLECTOR_HOST` | `pinpoint-collector` | Pinpoint Collector 호스트 |
| `PINPOINT_APPLICATION_NAME` | `ai-wiki-api` | Pinpoint 애플리케이션명 |
| `PINPOINT_AGENT_ID` | `api-001` | Pinpoint 에이전트 ID |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | FE → BE API 주소 |

---

## 6. 알려진 이슈 및 후속 작업

### 6.1 Stage 15 리뷰 게이트 MINOR 이슈 (7건)

| # | 이슈 | 심각도 | 후속 계획 |
|---|------|--------|----------|
| 1 | SecurityConfig `permitAll()` — 인증 미적용 | MINOR | M1 완료 후 JWT 인증 전환 |
| 2 | userId 클라이언트 전달 방식 | MINOR | JWT 토큰 기반 추출로 전환 |
| 3 | 단건 조회/수정/삭제 시 소유권 미검증 | MINOR | 인증 도입 시 함께 적용 |
| 4 | `Document.activate()` 상태 검증 미흡 (DRAFT 외 호출 가능) | MINOR | NAW-BE-001 후속 패치 |
| 5 | DeleteDocumentUseCase 미구현 (DELETED 전이) | MINOR | NAW-BE-004 범위 후속 구현 |
| 6 | Markdown XSS sanitize 미적용 (FE) | MINOR | NAW-FE-002 후속 |
| 7 | `application.yml` H2 설정 — 운영 환경 분리 필요 | MINOR | 프로파일별 yml 분리 |

### 6.2 Stage 14 정적 분석 추가 이슈 (8건)

| # | 이슈 | 심각도 | 후속 계획 |
|---|------|--------|----------|
| 1 | `@Version` + `updatedAt` 이중 낙관적 잠금 관리 | MINOR | 혼동 주의, 문서화 |
| 2 | 검색 인메모리 필터링 — 대량 데이터 성능 저하 | MINOR | PostgreSQL tsvector 전환 |
| 3 | Mock `Thread.sleep(1000)` — 스레드 풀 점유 | MINOR | 실제 AI Adapter 전환 시 해소 |
| 4 | SSE emitter Controller 인스턴스 필드 — 멀티 인스턴스 유실 | MINOR | Redis Pub/Sub 전환 |
| 5 | FE 에러 응답 body 파싱 없음 | MINOR | 후속 에러 핸들링 개선 |
| 6 | SSE 재연결 로직 없음 | MINOR | NAW-FE-004 후속 |
| 7 | Dockerfile Pinpoint Agent 네트워크 의존 빌드 | MINOR | 멀티스테이지 빌드 개선 |
| 8 | Docker 이미지 push 레지스트리 미설정 | INFO | 레지스트리 확정 후 추가 |

---

## 7. 마일스톤 계획 요약

### 7.1 전체 로드맵

| 마일스톤 | 이름 | 범위 | 핵심 기능 |
|---------|------|------|----------|
| **M1** | Foundation + Auth | MVP | 회원 체계, 문서 CRUD, 상태 관리, JWT 인증, OAuth2 |
| **M2** | AI Pipeline + SSE | MVP | AI 비동기 파이프라인, SSE 실시간 스트리밍, Revision |
| **M3** | Search + Views + Ops | MVP | 전문 검색, 조회수/랭킹, Pinpoint APM |
| **M4** | Collaboration | Post-MVP | 댓글/대댓글, 팀 단위 Wiki, 권한 관리 (OWNER/EDITOR/VIEWER) |
| **M5** | Analytics | Post-MVP | 문서/사용자/팀 통계, 대시보드 |

### 7.2 마일스톤 의존성

```
M1 ──→ M2 ──→ M3
 │               │
 └───────────────┼──→ M4 ──→ M5
                 │
                 └──→ M5
```

### 7.3 신규 기능 (5건, Stage 03에서 추가)

| 기능 | 요구사항 ID | 마일스톤 |
|------|-----------|----------|
| 회원가입/로그인/OAuth2/프로필 | FR-07-01~04 | M1 |
| 댓글 CRUD/대댓글/멘션 | FR-08-01~03 | M4 |
| 조회수 자동 증가/인기 랭킹 | FR-09-01~02 | M3 |
| 팀 생성/관리/공유 문서/권한 | FR-10-01~04 | M4 |
| 통계 집계/대시보드 | FR-11-01~04 | M5 |

---

## 8. 팀 산출물 인덱스

| Stage | 파일 | 설명 |
|-------|------|------|
| 01 | `outputs/team_run/01_ambiguity.md` | PRD 모호성 분석 (23건: 높음 9, 중간 10, 낮음 4) |
| 02 | `outputs/team_run/02_requirements_analysis.md` | 기능/비기능 요구사항 분석 (FR 6그룹, NFR 6그룹) |
| 03 | `outputs/team_run/03_roadmap_update.md` | 릴리즈 로드맵 (M1~M5, 신규 5기능 추가) |
| 06 | `outputs/team_run/06_architecture.md` | 헥사고날 아키텍처 설계 (멀티모듈, ERD, Pinpoint) |
| 08 | `outputs/team_run/08_ticket_breakdown.md` | 티켓 분해 (BE 10, FE 5, DevOps 4, Cross-Lane 1 = 20건) |
| 11 | `outputs/team_run/11_fe_codegen.md` | FE 코드 생성 (Next.js 14, 5개 티켓 구현) |
| 12 | `outputs/team_run/12_be_codegen.md` | BE 코드 생성 (Kotlin Spring Boot, 10개 티켓 구현) |
| 13 | `outputs/team_run/13_devops_codegen.md` | DevOps 구현 (Docker Compose, Pinpoint, CI/CD) |
| 14 | `outputs/team_run/14_static_analysis.md` | 정적 분석 (CRITICAL 0, MAJOR 0, MINOR 10, INFO 5 → PASS) |
| 15 | `outputs/team_run/15_review_gate.md` | 품질 리뷰 게이트 (MINOR 7건 → APPROVE) |
| 16 | `outputs/team_run/16_docs.md` | 최종 운영 릴리즈 문서 (본 문서) |

---

## 9. 품질 게이트 결과 요약

| 게이트 | 판정 | CRITICAL | MAJOR | MINOR |
|--------|------|----------|-------|-------|
| Stage 14 정적 분석 | **PASS** | 0 | 0 | 10 |
| Stage 15 품질 리뷰 | **APPROVE** | 0 | 0 | 7 |

**최종 판정**: 모든 CRITICAL/MAJOR 이슈 없음. MVP 릴리즈 승인.

---

*Generated by AI Orchestrator Lab — Stage 16 Final Delivery Docs*
