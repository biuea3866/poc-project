# AI Wiki Project — Agent Context

## Project Overview
AI 기반 계층형 지식 위키. 사용자는 기록하고, 정리는 AI가 수행.

## Tech Stack
- **Backend:** Spring Boot (Kotlin), MySQL 8.0, PostgreSQL 16 + pgvector, Apache Kafka (KRaft)
- **Frontend:** Next.js (App Router), TypeScript, Tailwind CSS, React Query
- **Infra:** Docker Compose

## Key Documents
- `requirement.md` — 요구사항, ERD, API 명세, FE/DevOps 스펙 전체
- `logs/` — 피처별 작업 이력 (아래 규칙 참고)

## Git Worktree Workflow
이 프로젝트는 **git worktree** 기반 병렬 개발 구조를 사용합니다.

```
flag_project/                              ← main (관리 허브, 직접 개발 X)
flag_project/wiki/worktrees/feat-auth/     ← feat/auth 브랜치
flag_project/wiki/worktrees/feat-fe/       ← feat/fe 브랜치
flag_project/wiki/worktrees/feat-ai-worker/← feat/ai-worker 브랜치
```

### Worktree 명령어 (wiki/ 디렉토리에서 실행)
```bash
make -C wiki worktree-add FEAT=auth          # 워크트리 생성
make -C wiki worktree-remove FEAT=auth       # 워크트리 제거
make -C wiki worktree-list                   # 전체 목록 확인
```

## Work Log Rules (필수)

### 작업 시작 전
1. `wiki/logs/` 디렉토리에서 **현재 브랜치명에 해당하는 로그 파일**을 읽어라.
   - 예: `feat/auth` 브랜치 → `wiki/logs/feat-auth.md`
2. 이전 에이전트의 마지막 기록을 확인하고 이어서 작업하라.

### 작업 완료 후
1. 해당 로그 파일에 아래 형식으로 **append** 기록하라.
2. 절대로 이전 기록을 삭제하거나 수정하지 마라.

### 로그 형식
```markdown
---
### YYYY-MM-DD HH:MM
- **Agent:** Claude | Codex | Gemini
- **Task:** 수행한 작업 요약
- **Changes:** 변경된 파일 목록
- **Decisions:** 주요 결정 사항과 이유
- **Issues:** 발생한 이슈 (없으면 생략)
- **Next:** 다음 에이전트가 이어서 해야 할 작업
---
```

## Conventions
- API endpoint는 복수형 사용: `/api/v1/documents`
- Soft delete: `deleted_at` nullable 컬럼 사용
- 브랜치 네이밍: `feat/{feature-name}`
- 커밋 메시지: 한글 허용, 명령형으로 작성
