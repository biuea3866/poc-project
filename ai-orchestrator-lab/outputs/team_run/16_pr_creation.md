# Stage 16: Pull Request 생성

> Stage 15 APPROVE 기준 | 작성일: 2026-03-14 | 담당: orchestrator-dev (tech_lead)

---

## PR 생성 결과

| Lane | PR 번호 | URL | 상태 |
|------|---------|-----|------|
| BE | #18 | https://github.com/biuea3866/poc-project/pull/18 | Open |
| FE | #19 | https://github.com/biuea3866/poc-project/pull/19 | Open |
| DevOps | #20 | https://github.com/biuea3866/poc-project/pull/20 | Open |

---

## 브랜치 구조

| Lane | 브랜치 | Base |
|------|--------|------|
| BE | `feat/ai-wiki-be` | `main` |
| FE | `feat/ai-wiki-fe` | `main` |
| DevOps | `feat/ai-wiki-devops` | `main` |
| 공통 기반 | `feat/ai-wiki-base` | `main` |

---

## PR 본문 요약

### BE (#18)
- Document 도메인 POJO + 상태 머신 (DRAFT/ACTIVE/DELETED, AI NOT_STARTED~FAILED)
- 헥사고날 아키텍처: domain → application → persistence-jpa → api
- JPA Entity/Domain POJO 분리, 낙관적 잠금(@Version updatedAt)
- Async AI 파이프라인 + SSE 실시간 상태 스트리밍
- DELETE 엔드포인트 구현 (Stage 15 MINOR 이슈 해결)

### FE (#19)
- Next.js 14 App Router + TypeScript + Tailwind
- 문서 목록/상세/생성 페이지
- SSE ai-status 이벤트 수신 (addEventListener 방식)
- 올바른 API 경로(/api/v1/documents) + DELETE 버튼

### DevOps (#20)
- Docker Compose: PostgreSQL 15 + API + Pinpoint 2.5.4
- GitHub Actions: BE CI (JDK 21 + Gradle) + FE CI (Node 20 + tsc)
- 네트워크 분리: aiwiki-net / pinpoint-net

---

## Jira 티켓 상태

아래 티켓은 PR 생성과 함께 **In Progress** 상태로 전환됩니다.

| 티켓 ID | 제목 | Lane | PR |
|---------|------|------|----|
| NAW-BE-001 | Document 도메인 POJO + 상태 머신 | BE | #18 |
| NAW-BE-002 | Port 인터페이스 정의 | BE | #18 |
| NAW-BE-003 | JPA Entity + 영속성 어댑터 | BE | #18 |
| NAW-BE-004 | 문서 CRUD UseCase | BE | #18 |
| NAW-BE-005 | AI 파이프라인 Async | BE | #18 |
| NAW-BE-006 | SSE 실시간 스트리밍 | BE | #18 |
| NAW-FE-001 | 문서 목록 페이지 | FE | #19 |
| NAW-FE-002 | 문서 상세/편집 페이지 | FE | #19 |
| NAW-FE-003 | 문서 생성 페이지 | FE | #19 |
| NAW-FE-004 | SSE AI 상태 실시간 UI | FE | #19 |
| NAW-DO-001 | 개발 환경 Docker Compose | DevOps | #20 |
| NAW-DO-002 | Pinpoint 모니터링 환경 | DevOps | #20 |
| NAW-DO-003 | Dockerfile + Pinpoint Agent | DevOps | #20 |
| NAW-DO-004 | CI/CD 파이프라인 | DevOps | #20 |

---

## 다음 단계

1. 사용자가 PR #18, #19, #20을 리뷰합니다
2. 머지 완료 후 → Stage 17 (Post-Merge Docs) 수행
3. Stage 17에서 Jira 티켓을 Done으로 전환

> ⚠️ PR 머지는 사용자가 직접 수행합니다. 자동 머지 금지.
