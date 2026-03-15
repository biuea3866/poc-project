# AI Wiki Roadmap

---

## 에이전트 작업 시작 가이드

> 작업을 시작하기 전 이 섹션을 반드시 읽어라.

### 프로젝트 한 줄 목표
> **사용자는 기록하고, 정리는 AI가 한다.**
> 문서를 저장하면 AI가 자동으로 요약·태깅·임베딩을 수행하고, 사용자는 그 상태를 실시간으로 확인할 수 있는 계층형 지식 위키.

### 기술 스택
| 영역 | 스택 |
|---|---|
| 백엔드 | Spring Boot (Kotlin), MySQL 8.0, PostgreSQL 16 + pgvector, Apache Kafka (KRaft) |
| 프론트엔드 | Next.js (App Router), TypeScript, Tailwind CSS, React Query |
| 인프라 | Docker Compose (로컬 개발 환경) |

### 작업 전 필독 파일
1. **`CLAUDE.md`** — 브랜치/워크트리 전략, 로그 작성 규칙, 코딩 컨벤션
2. **`requirement.md`** — 전체 요구사항, ERD, API 명세
3. **`logs/{현재 브랜치명}.md`** — 이전 에이전트가 남긴 작업 이력 (반드시 이어서 작업)

### 현재 구현 상태 (2026-03-15 기준)
| 영역 | 상태 | 비고 |
|---|---|---|
| 인프라 (Docker Compose) | ✅ 완료 | MySQL, PostgreSQL, Kafka, Redpanda Console |
| Auth API | ✅ 완료 | 회원가입/로그인/로그아웃/토큰 재발급(Refresh), 통합 시나리오 테스트 |
| Document API | ✅ 완료 | wiki-api 모듈, 11개 엔드포인트, Querydsl 검색 |
| AI 파이프라인 | ✅ 완료 | wiki-worker 모듈, Kafka 순차 파이프라인 (SUMMARY→TAGGER→EMBEDDING) |
| SSE 엔드포인트 | ✅ 완료 | SseEmitterManager, 30초 Heartbeat, COMPLETED/FAILED 자동 종료 |
| 관측성 (Observability) | ✅ 완료 | Actuator, OpenTelemetry, Prometheus, Loki, Grafana 대시보드 |
| CI/CD | ✅ 완료 | GitHub Actions (be-ci, docker-build, jira-sync) |
| Tag API | ✅ 완료 | 태그 타입 목록 조회 (NAW-105) |
| FE Auth | ✅ 완료 | 로그인/회원가입 페이지 |
| FE 사이드바/에디터 | ✅ 완료 | 계층형 트리, Markdown 에디터, 검색/Trash 연동 (NAW-106) |
| FE 문서 상세 | ✅ 완료 | AI 상태 분기, SSE 연동, 히스토리, 휴지통, 복구 |
| FE 검색 | ✅ 완료 | LIKE 검색 연동 (NAW-106) |

### 작업 우선순위
```
1순위 (Now/MVP)  → 아래 "Now" 체크리스트 참고
2순위 (Next)     → RAG 검색, 임베딩 관리, Refresh 토큰 고도화
3순위 (Later)    → 댓글, 권한/공유, 협업 고도화
```

---

## Now (MVP)

> 핵심 목표: 사용자가 문서를 작성하면 AI가 비동기로 요약·태깅하고, 그 상태를 실시간으로 확인할 수 있는 기본 위키

### 기획 확정 사항

| 항목 | 결정 내용 |
|---|---|
| 문서 상태 모델 | `status = DRAFT\|ACTIVE\|DELETED`, `ai_status = PENDING\|PROCESSING\|COMPLETED\|FAILED` 분리 |
| 버전 관리 | 사용자 수정 시마다 revision 생성. 포함 필드: title, content, tags, summary |
| 태그/요약 | 전역 태그 + 매핑 테이블, document_summary는 1:1 |
| 삭제 정책 | 소프트 삭제, 하위 문서 cascade, Trash에서 복구 가능 (부모 삭제 시 루트로 복구) |
| AI 파이프라인 | 순차 실행: SUMMARY → TAGGER → EMBEDDING. 실패 시 3회 재시도 후 FAILED |
| SSE 규격 | event+data JSON 포맷, 30초 Heartbeat, 클라이언트 최대 3회 재연결 |
| 검색 (MVP) | 제목/내용 LIKE 검색만 제공. RAG는 Next 마일스톤 |
| Refresh 토큰 | 기본 구현 (7일 만료, Redis/DB 저장) |

### 백엔드 구현 목록

- [x] **Auth API** _(NAW-105)_
  - [x] 회원가입 / 로그인 / 로그아웃
  - [x] Refresh 토큰 재발급 (`POST /api/v1/auth/refresh`)
  - [x] Testcontainers 기반 엔드포인트 통합 시나리오 테스트 (`signup/login/refresh/logout/delete`)

- [x] **Document API**
  - [x] 문서 생성 (`POST /api/v1/documents`) — DRAFT 상태로 생성
  - [x] 문서 발행 (`POST /api/v1/documents/{id}/publish`) — ACTIVE 전환 및 Kafka 이벤트 발행
  - [x] 문서 목록/트리 조회 (`GET /api/v1/documents`)
  - [x] 문서 상세 조회 (`GET /api/v1/documents/{id}`) — 요약·태그 포함
  - [x] 문서 수정 (`PUT /api/v1/documents/{id}`) — revision 아카이빙
  - [x] 문서 삭제 (`DELETE /api/v1/documents/{id}`) — cascade soft delete
  - [x] 문서 복구 (`POST /api/v1/documents/{id}/restore`)
  - [x] Trash 조회 (`GET /api/v1/documents/trash`)
  - [x] 버전 목록 조회 (`GET /api/v1/documents/{id}/revisions`)
  - [x] 태그 목록 조회 (`GET /api/v1/documents/{id}/tags`)

- [x] **AI 파이프라인**
  - [x] 문서 발행 시 `event.document` 이벤트 발행 (Kafka Producer, PublishDocumentFacade)
  - [x] SUMMARY 에이전트 (wiki-worker, `event.document` 컨슘 → LLM 요약 → `queue.ai.tagging` 발행)
  - [x] TAGGER 에이전트 (wiki-worker, `queue.ai.tagging` 컨슘 → LLM 태깅 → `queue.ai.embedding` 발행)
  - [x] EMBEDDING 에이전트 (wiki-worker, `queue.ai.embedding` 컨슘 → 벡터화 → PostgreSQL upsert → COMPLETED)
  - [x] `ai_status` 상태 전이 관리 (PENDING → PROCESSING → COMPLETED / FAILED)
  - [x] 실패 처리: FixedBackOff 3회 재시도 후 `event.ai.failed` 발행 → FAILED 전이
  - [x] 수동 재분석 (`POST /api/v1/documents/{id}/analyze`)

- [x] **SSE / 상태 조회**
  - [x] AI 상태 Polling (`GET /api/v1/documents/{id}/ai-status`)
  - [x] AI 상태 SSE 스트림 (`GET /api/v1/documents/{id}/ai-status/stream`) — SseEmitterManager (30초 Heartbeat)

- [x] **검색**
  - [x] 제목/내용 LIKE 검색 (`GET /api/v1/search/integrated`) — Querydsl
  - [ ] 웹 검색 연동 (`GET /api/v1/search/web`)

- [x] **태그 API** _(NAW-105)_
  - [x] 태그 타입 목록 조회 (`GET /api/v1/tags/types`)

- [x] **AI 로그**
  - [x] 에이전트 로그 조회 (`GET /api/v1/ai/logs`)

### 프론트엔드 구현 목록

- [x] Auth (로그인 / 회원가입)
- [x] 사이드바 계층형 문서 트리 (API 연동) _(NAW-106)_
- [x] Markdown 에디터 (작성 / 수정 / DRAFT→ACTIVE 발행) _(NAW-106)_
- [x] 문서 상세 페이지 — AI 상태별 UI 분기 (스켈레톤 / 요약·태그 노출 / 재시도 버튼) _(NAW-106)_
- [x] SSE 기반 AI 상태 실시간 연동 (`useAiStatus`)
- [x] 버전 히스토리 페이지
- [x] Trash(휴지통) 페이지
- [x] Trash에서 복구 버튼 연동 _(NAW-106)_
- [x] 검색 페이지 (LIKE 검색 결과) _(NAW-106)_

---

## Next 마일스톤 (Sprint 2026-03-17 ~ 2026-03-28)

> 핵심 목표: MVP 안정성 강화 + AI 검색 고도화. Outbox 패턴으로 이벤트 신뢰성을 확보하고, RAG 벡터 검색으로 지식 탐색 경험을 업그레이드한다.

### N-1. 아웃박스 패턴 (Transactional Outbox)

**목표:** Kafka 발행 신뢰성 보장 (at-least-once delivery, 장애 시 이벤트 유실 방지)

**구조:**
1. Kafka 메시지 발행 시 비동기 스레드에서 시도, ErrorHandler 기본 3회 재시도
2. 재시도 모두 실패 시 `outbox` 테이블에 미발행 이벤트 적재 (status = PENDING)
3. 컨슈머가 이벤트 처리 성공 시 outbox 로우에 성공 표시 (status = SUCCESS)
4. 실패 시 스케줄러(자동 재처리) 또는 어드민 API를 통한 수동 재처리 지원

**인수 기준 (Acceptance Criteria):**
- [x] Kafka 발행 실패 시 outbox 테이블에 이벤트가 정확히 기록된다
- [x] 스케줄러가 PENDING 상태의 outbox 이벤트를 1분 주기로 재발행한다
- [x] 재발행 성공 시 outbox 상태가 SUCCESS로 전이된다
- [x] 재시도 5회 초과 시 DEAD_LETTER 상태로 전이되고 알림이 발생한다
- [x] 어드민 API로 실패 이벤트 조회 및 수동 재처리가 가능하다
- [x] 기존 AI 파이프라인 동작에 영향을 주지 않는다 (하위 호환)

**구현 체크리스트:**
- [x] `outbox_event` 테이블 스키마 (id, aggregate_type, aggregate_id, topic, payload, status, retry_count, max_retries, created_at, processed_at, error_message) — `docker/mysql/init.sql` 추가
- [x] OutboxEvent 엔티티 & Repository
- [x] Kafka 발행 실패 시 outbox 적재 로직 (OutboxKafkaPublisher 래퍼)
- [x] PublishDocumentFacade — DocumentApiController publish/analyze에서 OutboxKafkaPublisher 경유
- [x] 컨슈머 성공/실패 시 outbox 상태 업데이트 (NAW-119)
- [x] 스케줄러: 미처리 outbox 주기적 재발행 (`@Scheduled`, 1분 간격)
- [x] DEAD_LETTER 전이 로직 (retry_count >= max_retries)
- [x] 어드민 API: `GET /api/admin/outbox` (목록, 필터: status), `POST /api/admin/outbox/{id}/retry` (수동 재처리)
- [x] 단위 테스트: OutboxEventTest, OutboxServiceTest, OutboxKafkaPublisherTest, OutboxSchedulerTest, PublishDocumentFacadeTest
- [ ] 통합 테스트: Kafka 장애 시뮬레이션 → outbox 적재 → 스케줄러 재발행 검증 (Testcontainers)

### N-2. RAG 벡터 검색 도입

**목표:** 기존 LIKE 검색에 벡터 유사도 기반 시맨틱 검색을 추가하여 지식 탐색 품질 향상

**사용자 스토리:**
- 사용자가 "머신러닝 모델 배포 방법"을 검색하면, 정확히 해당 키워드가 없더라도 "ML 모델 서빙", "모델 인퍼런스 파이프라인" 등 의미적으로 유사한 문서가 검색된다
- 검색 결과에서 키워드 매칭 결과와 시맨틱 매칭 결과가 탭으로 구분되어 표시된다
- 각 검색 결과에 유사도 점수가 표시되어 관련성을 파악할 수 있다

**API 스펙 (초안):**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/search/semantic` | 벡터 유사도 기반 시맨틱 검색. 쿼리 파라미터: `q`, `threshold`(기본 0.7), `page`, `size` |
| GET | `/api/v1/search/integrated` | 기존 LIKE + 시맨틱 통합 검색. 쿼리 파라미터: `q`, `mode`(keyword/semantic/hybrid), `page`, `size` |

**응답 스펙 (시맨틱 검색):**
```json
{
  "items": [
    {
      "documentId": 1,
      "title": "...",
      "snippet": "... 매칭된 chunk 텍스트 ...",
      "similarity": 0.89,
      "tags": ["AI", "배포"]
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 42
}
```

**구현 체크리스트:**
- [x] pgvector 코사인 유사도 검색 쿼리 구현 (PostgreSQL Repository)
- [x] 시맨틱 검색 API 엔드포인트 (`GET /api/v1/search/semantic`)
- [x] 통합 검색 API에 `mode` 파라미터 추가 (keyword/semantic/hybrid)
- [x] Hybrid 검색 랭킹: RRF (Reciprocal Rank Fusion) 알고리즘 적용
- [x] 검색 결과 snippet 생성 (매칭 chunk에서 주변 텍스트 추출)
- [ ] FE: 검색 결과 페이지에 시맨틱 탭 추가
- [ ] FE: 유사도 점수 시각화 (프로그레스 바 또는 백분율)
- [ ] FE: 검색 모드 전환 UI (키워드 / 시맨틱 / 하이브리드)

### N-3. Refresh 토큰 고도화

**목표:** Refresh 토큰 보안 강화 — 토큰 탈취 시 피해 최소화

**회전(Rotation) 정책:**
- Refresh 토큰 사용 시 기존 토큰을 무효화하고 새 Refresh 토큰을 함께 발급 (단일 사용)
- 이미 사용된(무효화된) Refresh 토큰으로 재발급 요청 시 → 해당 사용자의 모든 Refresh 토큰 무효화 (토큰 탈취 감지)
- Refresh 토큰 Family 개념 도입: 같은 로그인 세션에서 발급된 토큰 체인을 추적

**구현 체크리스트:**
- [ ] `refresh_token` 테이블 스키마 (id, token_hash, user_id, family_id, is_revoked, expires_at, created_at)
- [ ] 토큰 발급 시 family_id 부여 및 체인 추적
- [ ] 토큰 사용 시 회전: 기존 토큰 revoke + 새 토큰 발급
- [ ] 탈취 감지: revoked 토큰 재사용 시 같은 family 전체 무효화
- [ ] FE: 자동 재발급 시 새 Refresh 토큰 저장 로직 업데이트
- [ ] 통합 테스트: 정상 회전, 탈취 감지, 만료 시나리오

### N-4. 운영/보안 강화

**구현 체크리스트:**
- [x] Datadog 유사 운영 화면 기준 SLO/알람 정의 (Grafana) — NAW-126: wiki-slo.json 강화(P50/P95/P99, SSE, 에러율 SLO 라인), WikiWorkerDown/AnthropicCallSpiked/OpenAiCallSpiked 알람 추가
- [x] 로그 보관 정책 수립 (30일 hot, 90일 cold) — NAW-126: loki-config.yml retention_period 720h, compactor retention_enabled: true
- [x] AI 파이프라인 비용 모니터링 대시보드 (LLM API 호출 횟수/토큰 사용량) — NAW-127: wiki-ai-cost.json 대시보드 + AiMetricsService (wiki_ai_anthropic_calls_total, wiki_ai_openai_calls_total)
- [ ] 감사 로그(Audit Trail) 기본 구현 — 로그인/문서 변경/삭제 이력

### 스프린트 역할 배분

| 기능 | 담당 | 예상 티켓 수 | 우선순위 |
|---|---|---|---|
| N-1. Outbox 패턴 | BE | 4 | P0 (안정성) |
| N-2. RAG 벡터 검색 — BE | BE | 4 | P0 (핵심 가치) |
| N-2. RAG 벡터 검색 — FE | FE | 3 | P1 |
| N-3. Refresh 토큰 고도화 | BE + FE | 3 | P1 (보안) |
| N-4. SLO/알람/모니터링 | DevOps | 2 | P2 |
| N-4. 감사 로그 | BE | 1 | P2 |
| 디자인 핸드오프 | Design | - | P1 |

---

## Sprint 3 (2026-04-01 ~ 2026-04-14)

> 핵심 목표: "쓰고 싶은 위키"로 진화 — 사용자 리텐션 확보 + 협업 기반 구축

### 스프린트 역할 배분

| 기능 | 담당 | 우선순위 | 비고 |
|---|---|---|---|
| S3-1. 댓글/토론 | BE + FE | P0 | 인게이지먼트 핵심 |
| S3-2. 알림 시스템 | BE + FE | P0 | 재방문 유도 |
| S3-3. 버전 Diff 비교 | FE | P1 | BE revision 이미 완료 |
| S3-4. 문서 공유/권한 관리 | BE + FE | P1 | 팀 협업 진입점 |
| S3-5. 대시보드/활동 피드 | BE + FE | P2 | 인기 문서, 최근 활동 |
| S3-6. AI 고도화 — 연관 문서 추천 | BE + FE | P2 | RAG 완료 후 확장 |
| S3-7. 내보내기 (PDF / Markdown) | BE + FE | P2 | B2B 요구사항 |

---

### S3-1. 댓글/토론

> ACTIVE 상태의 문서에 댓글·대댓글 작성 가능. 1단계 대댓글만 지원.

**백엔드 구현 목록**
- [ ] Comment 엔티티 & Repository
- [ ] 댓글 목록 조회 (`GET /api/v1/documents/{id}/comments`) — 대댓글 포함 트리 구조 반환
- [ ] 댓글 작성 (`POST /api/v1/documents/{id}/comments`) — `parentId` 전달 시 대댓글
- [ ] 댓글 수정 (`PUT /api/v1/comments/{commentId}`) — 작성자 본인만
- [ ] 댓글 삭제 (`DELETE /api/v1/comments/{commentId}`) — 소프트 삭제, 작성자 본인만
- [ ] 삭제된 댓글 플레이스홀더 처리 (대댓글이 남아 있으면 "삭제된 댓글입니다." 표시)

**프론트엔드 구현 목록**
- [ ] 문서 상세 페이지 — 댓글 목록 렌더링
- [ ] 댓글 작성 폼
- [ ] 대댓글 UI (1단계 인덴트)
- [ ] 삭제된 댓글 플레이스홀더 표시
- [ ] 수정/삭제 버튼 — 본인 댓글에만 노출

---

### S3-2. 알림 시스템

> 내 문서 변경, 댓글, AI 완료 이벤트를 인앱 알림으로 수신

**백엔드 구현 목록**
- [ ] Notification 엔티티 & Repository (type, target_user_id, ref_id, is_read, created_at)
- [ ] 알림 생성 이벤트: 댓글 작성 / 내 문서 수정 / AI 완료 / AI 실패
- [ ] 알림 목록 조회 (`GET /api/v1/notifications`) — 페이지네이션, 읽음 필터
- [ ] 알림 읽음 처리 (`PATCH /api/v1/notifications/{id}/read`)
- [ ] 전체 읽음 처리 (`PATCH /api/v1/notifications/read-all`)
- [ ] 미읽음 카운트 (`GET /api/v1/notifications/unread-count`)
- [ ] SSE 기반 실시간 알림 스트림 (`GET /api/v1/notifications/stream`)

**프론트엔드 구현 목록**
- [ ] 헤더 알림 벨 아이콘 + 미읽음 뱃지
- [ ] 알림 드롭다운 목록 (최근 20건)
- [ ] 알림 클릭 시 해당 문서/댓글로 이동
- [ ] SSE 연결로 실시간 알림 수신

---

### S3-3. 버전 Diff 비교

> 이미 구현된 revision 데이터를 활용한 변경 이력 시각화

**프론트엔드 구현 목록**
- [ ] 버전 히스토리 페이지 — 두 revision 선택 후 diff 비교 뷰
- [ ] 변경된 줄 하이라이트 (추가: 초록, 삭제: 빨강)
- [ ] 특정 revision으로 롤백 버튼 (`POST /api/v1/documents/{id}/rollback`)

**백엔드 구현 목록**
- [ ] 두 revision 간 diff 계산 API (`GET /api/v1/documents/{id}/revisions/diff?from={v1}&to={v2}`)
- [ ] 특정 revision으로 롤백 (`POST /api/v1/documents/{id}/rollback`) — 새 revision으로 생성

---

### S3-4. 문서 공유/권한 관리

> 문서 단위 공개 범위 설정 + 공유 링크 생성

**기획 확정 사항**

| 공개 범위 | 설명 |
|---|---|
| PRIVATE | 작성자 본인만 |
| WORKSPACE | 로그인 사용자 전체 |
| PUBLIC | 비로그인 포함 누구나 (공유 링크) |

**백엔드 구현 목록**
- [ ] Document에 `visibility` 컬럼 추가 (PRIVATE / WORKSPACE / PUBLIC)
- [ ] 공유 링크 생성 (`POST /api/v1/documents/{id}/share`) — 토큰 기반 URL 발급
- [ ] 공유 링크 조회 (`GET /api/v1/share/{token}`) — 인증 불필요
- [ ] 권한 체크 미들웨어 (SecurityConfig 업데이트)

**프론트엔드 구현 목록**
- [ ] 문서 설정 패널 — 공개 범위 선택 드롭다운
- [ ] 공유 링크 복사 버튼
- [ ] PUBLIC 문서 비로그인 뷰 (읽기 전용)

---

### S3-5. 대시보드/활동 피드

> 팀의 지식 활동을 한눈에 파악

**백엔드 구현 목록**
- [ ] 활동 피드 API (`GET /api/v1/activity`) — 최근 문서 생성/수정/댓글, 페이지네이션
- [ ] 통계 API (`GET /api/v1/stats`) — 전체 문서 수, 활성 사용자, 인기 태그 Top5, 최근 7일 문서 생성 추이

**프론트엔드 구현 목록**
- [ ] 대시보드 홈 페이지 (`/dashboard`) — 활동 피드 + 통계 카드
- [ ] 인기 문서 Top5 위젯
- [ ] 최근 7일 활동 그래프 (Recharts)

---

### S3-6. AI 고도화 — 연관 문서 추천

> RAG 완료 후 확장: 현재 보고 있는 문서와 의미적으로 유사한 문서 추천

**백엔드 구현 목록**
- [ ] 연관 문서 추천 API (`GET /api/v1/documents/{id}/related`) — pgvector 코사인 유사도, 상위 5건
- [ ] 추천 결과 캐싱 (Caffeine, TTL 10분)

**프론트엔드 구현 목록**
- [ ] 문서 상세 사이드바 — "연관 문서" 섹션 (최대 5건)
- [ ] 연관 문서 클릭 시 해당 문서로 이동

---

### S3-7. 내보내기 (PDF / Markdown)

> 문서 외부 공유 및 백업

**백엔드 구현 목록**
- [ ] Markdown 내보내기 (`GET /api/v1/documents/{id}/export?format=markdown`) — 원본 Markdown 파일 다운로드
- [ ] PDF 내보내기 (`GET /api/v1/documents/{id}/export?format=pdf`) — Flying Saucer 또는 Playwright 활용

**프론트엔드 구현 목록**
- [ ] 문서 상세 — 내보내기 드롭다운 버튼 (Markdown / PDF)

---

## Phase 4: 유저 프로필 & 설정 (User Profile & Settings)

### 기능 목표
사용자가 자신의 프로필을 관리하고, 앱 사용 환경을 개인화할 수 있도록 한다.

### 유저 스토리
- 사용자는 자신의 이름, 이메일, 프로필 이미지를 설정할 수 있다.
- 사용자는 비밀번호를 변경할 수 있다.
- 사용자는 알림 설정(AI 처리 완료 알림, 문서 공유 알림 등)을 켜고 끌 수 있다.
- 사용자는 에디터 기본 설정(기본 뷰 모드, 자동저장 간격 등)을 변경할 수 있다.
- 사용자는 계정 탈퇴를 할 수 있다.

### 구현 항목
#### BE
- [ ] `GET /api/v1/users/me` — 내 프로필 조회
- [ ] `PUT /api/v1/users/me` — 프로필 수정 (이름, 프로필 이미지 URL)
- [ ] `PUT /api/v1/users/me/password` — 비밀번호 변경
- [ ] `GET /api/v1/users/me/settings` — 설정 조회
- [ ] `PUT /api/v1/users/me/settings` — 설정 저장
- [ ] `DELETE /api/v1/users/me` — 계정 탈퇴 (soft delete)
- [ ] UserSettings 엔티티/테이블 설계

#### FE
- [ ] 프로필 페이지 (`/profile`) — 이름, 이메일, 프로필 이미지
- [ ] 설정 페이지 (`/settings`) — 알림, 에디터, 계정 관리 탭
- [ ] AppShell에 프로필 드롭다운 메뉴 (현재는 로그아웃만)
- [ ] 비밀번호 변경 모달

### Jira 티켓
- NAW-122: [Epic] 유저 프로필 & 설정
- NAW-123: [BE] 유저 프로필 API
- NAW-124: [BE] 유저 설정 API
- NAW-125: [FE] 프로필 & 설정 페이지

---

## Later (확장 로드맵)

### 권한/공유 고도화
- 팀/조직 단위 ACL, 문서 편집 초대

### 협업 고도화
- 실시간 공동 편집 (CRDT 기반), 멘션(@user)

### 멀티 에이전트 확장
- 정책 기반 에이전트 플러그인화
