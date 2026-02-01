# Work Logs

피처 브랜치별 작업 이력을 기록하는 디렉토리입니다.

## 규칙
- 파일명은 브랜치명과 동일: `feat/auth` → `feat-auth.md`
- 각 에이전트는 **자기 브랜치의 로그 파일만** 수정
- 기존 내용 삭제/수정 금지, **append only**
- 머지 시 충돌 방지를 위해 파일을 분리하는 구조

## 로그 엔트리 형식

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
