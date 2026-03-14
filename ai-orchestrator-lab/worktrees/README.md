# Worktrees

병렬 작업용 git worktree를 이 디렉토리 아래에 생성합니다.

중요:

- 모든 worktree의 기준 루트는 `ai-orchestrator-lab` 입니다.
- 기존 `wiki` 같은 외부 프로젝트를 가져와서 구현 기준으로 삼지 않습니다.
- `fe`, `be`, `devops` 내부 실제 제품 코드를 기준으로 lane별 브랜치를 분기합니다.

권장 예시:

```bash
git worktree add ai-orchestrator-lab/worktrees/fe-feature feature/fe-feature
git worktree add ai-orchestrator-lab/worktrees/be-analyze feature/be-analyze
git worktree add ai-orchestrator-lab/worktrees/devops-ci feature/devops-ci
```
