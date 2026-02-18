# AI Wiki Project — Agent Context

## Project Overview
AI 기반 계층형 지식 위키. 사용자는 기록하고, 정리는 AI가 수행.

## Tech Stack
- **Backend:** Spring Boot (Kotlin), MySQL 8.0, PostgreSQL 16 + pgvector, Apache Kafka (KRaft)
- **Frontend:** Next.js (App Router), TypeScript, Tailwind CSS, React Query
- **Infra:** Docker Compose

## Key Documents
- `ROADMAP.md` — 전체 진행 상태, 구현 체크리스트, 작업 우선순위
- `requirement.md` — 요구사항, ERD, API 명세, FE/DevOps 스펙 전체
- `logs/` — 피처별 작업 이력 (아래 규칙 참고)

---

## 작업 규칙 (필수)

### 1. 모든 작업은 워크트리에서 수행한다
- **main 브랜치에서 직접 개발하지 않는다.**
- 작업 시작 전 반드시 해당 피처의 worktree로 이동하여 작업한다.
- worktree가 없으면 아래 명령어로 생성한다.

```bash
# wiki/ 디렉토리에서 실행
make worktree-add FEAT=document-api   # 워크트리 생성 → worktrees/feat-document-api/
make worktree-remove FEAT=document-api # 워크트리 제거
make worktree-list                     # 전체 목록 확인
```

```
flag_project/wiki/                              ← main (관리 허브, 직접 개발 X)
flag_project/wiki/worktrees/feat-document-api/  ← feat/document-api 브랜치
flag_project/wiki/worktrees/feat-ai-worker/     ← feat/ai-worker 브랜치
flag_project/wiki/worktrees/feat-fe/            ← feat/fe 브랜치
```

### 2. 작업 시작 전
1. `ROADMAP.md`를 읽고 현재 진행 상태와 작업 우선순위를 파악한다.
2. `requirement.md`에서 구현할 기능의 요구사항·ERD·API 명세를 확인한다.
3. `logs/{현재 브랜치명}.md`를 읽고 이전 에이전트의 작업을 이어받는다.
   - 예: `feat/document-api` 브랜치 → `logs/feat-document-api.md`
   - 로그 파일이 없으면 새로 생성한다.

### 3. 작업 중 요구사항·ERD 변경 시
- 구현하다가 요구사항이나 ERD를 변경해야 할 경우, **`requirement.md`를 즉시 수정**한다.
- API 명세가 바뀌면 `requirement.md`의 API 명세 섹션도 함께 수정한다.
- 변경 내용은 작업 완료 후 로그의 **Decisions** 항목에 이유와 함께 기록한다.

### 4. 작업 완료 후
1. `logs/{브랜치명}.md`에 아래 형식으로 **append** 기록한다.
2. 절대로 이전 기록을 삭제하거나 수정하지 않는다.
3. `ROADMAP.md`의 구현 체크리스트에서 완료된 항목을 `[x]`로 업데이트한다.

```markdown
---
### YYYY-MM-DD HH:MM
- **Agent:** Claude | Codex | Gemini
- **Task:** 수행한 작업 요약
- **Changes:** 변경된 파일 목록
- **Decisions:** 주요 결정 사항과 이유 (요구사항·ERD 변경 포함)
- **Issues:** 발생한 이슈 (없으면 생략)
- **Next:** 다음 에이전트가 이어서 해야 할 작업
---
```

---

## Conventions
- API endpoint는 복수형 사용: `/api/v1/documents`
- Soft delete: `deleted_at` nullable 컬럼 사용
- 브랜치 네이밍: `feat/{feature-name}`
- 커밋 메시지: 한글 허용, 명령형으로 작성
