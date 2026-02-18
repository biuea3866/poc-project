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

### 현재 구현 상태 (2026-02-18 기준)
| 영역 | 상태 | 비고 |
|---|---|---|
| 인프라 (Docker Compose) | ✅ 완료 | MySQL, PostgreSQL, Kafka, Redpanda Console |
| Auth API | ✅ 완료 | 회원가입/로그인/로그아웃. Refresh 토큰 미구현 |
| Document API | ❌ 미구현 | Facade·엔티티만 존재, 컨트롤러 없음 |
| AI 파이프라인 | ❌ 미구현 | Kafka Producer/Consumer, 에이전트 없음 |
| SSE 엔드포인트 | ❌ 미구현 | FE 훅(`useAiStatus`)은 준비됨 |
| FE Auth | ✅ 완료 | 로그인/회원가입 페이지 |
| FE 문서 상세 | ⚠️ 부분 완료 | SSE 연동·히스토리·휴지통 페이지 있음. 에디터·트리 미구현 |
| FE 검색 | ⚠️ 페이지만 존재 | API 미연동 |

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

- [ ] **Auth API**
  - [x] 회원가입 / 로그인 / 로그아웃
  - [ ] Refresh 토큰 재발급 (`POST /api/v1/auth/refresh`)

- [ ] **Document API**
  - [ ] 문서 생성 (`POST /api/v1/documents`) — DRAFT 상태로 생성. 발행 시 ACTIVE 전환 및 Kafka 이벤트 발행
  - [ ] 문서 목록/트리 조회 (`GET /api/v1/documents`)
  - [ ] 문서 상세 조회 (`GET /api/v1/documents/{id}`) — 요약·태그 포함
  - [ ] 문서 수정 (`PUT /api/v1/documents/{id}`) — revision 아카이빙
  - [ ] 문서 삭제 (`DELETE /api/v1/documents/{id}`) — cascade soft delete
  - [ ] 문서 복구 (`POST /api/v1/documents/{id}/restore`)
  - [ ] Trash 조회 (`GET /api/v1/documents/trash`)
  - [ ] 버전 목록 조회 (`GET /api/v1/documents/{id}/revisions`)
  - [ ] 태그 목록 조회 (`GET /api/v1/documents/{id}/tags`)

- [ ] **AI 파이프라인**
  - [ ] 문서 발행 시 `document-created` 이벤트 발행 (Kafka Producer)
  - [ ] SUMMARY 에이전트 (Kafka Consumer → LLM 요약 → `ai-summary-finished` 발행)
  - [ ] TAGGER 에이전트 (Kafka Consumer → LLM 태깅 → `ai-tagging-finished` 발행)
  - [ ] EMBEDDING 에이전트 (Kafka Consumer → 벡터화 → PostgreSQL 저장 → `ai-embedding-finished` 발행)
  - [ ] `ai_status` 상태 전이 관리 (PENDING → PROCESSING → COMPLETED / FAILED)
  - [ ] 실패 처리: 3회 재시도 후 `ai-processing-failed` 발행
  - [ ] 수동 재분석 (`POST /api/v1/documents/{id}/analyze`)

- [ ] **SSE / 상태 조회**
  - [ ] AI 상태 Polling (`GET /api/v1/documents/{id}/ai-status`)
  - [ ] AI 상태 SSE 스트림 (`GET /api/v1/documents/{id}/ai-status/stream`)

- [ ] **검색**
  - [ ] 제목/내용 LIKE 검색 (`GET /api/v1/search/integrated`)
  - [ ] 웹 검색 연동 (`GET /api/v1/search/web`)

- [ ] **태그 API**
  - [ ] 태그 타입 목록 조회 (`GET /api/v1/tags/types`)

- [ ] **AI 로그**
  - [ ] 에이전트 로그 조회 (`GET /api/v1/ai/logs`)

### 프론트엔드 구현 목록

- [x] Auth (로그인 / 회원가입)
- [ ] 사이드바 계층형 문서 트리 (API 연동)
- [ ] Markdown 에디터 (작성 / 수정 / DRAFT→ACTIVE 발행)
- [ ] 문서 상세 페이지 — AI 상태별 UI 분기 (스켈레톤 / 요약·태그 노출 / 재시도 버튼)
- [x] SSE 기반 AI 상태 실시간 연동 (`useAiStatus`)
- [x] 버전 히스토리 페이지
- [x] Trash(휴지통) 페이지
- [ ] Trash에서 복구 버튼 연동
- [ ] 검색 페이지 (LIKE 검색 결과 / 웹 검색 탭)

---

## Next (1~2 마일스톤)

- **RAG 벡터 검색 도입**
  - 통합 검색에 벡터 유사도 결과 섹션 추가
  - 검색 랭킹/스코어링 정책 수립
- **임베딩 관리 전략**
  - 모델 버전/차원 관리, 재인덱싱 정책
- **Refresh 토큰 고도화**
  - 회전(rotation) 및 단일 사용(one-time) 정책
- **운영/보안**
  - 로그 보관 기간, 감사 추적, 비용 모니터링

---

## Later (확장 로드맵)

### 댓글

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

### 권한/공유 모델
- 팀/조직/ACL/공유 링크

### 협업 고도화
- 변경 diff, 알림/웹훅

### 멀티 에이전트 확장
- 정책 기반 에이전트 플러그인화
