# 티켓 수명주기 규칙

## 브랜치 규칙

- 모든 구현은 티켓 단위 브랜치로 진행합니다.
- 브랜치 이름에는 Jira 이슈 키를 포함합니다.
- 예시:
  - `feature/naw-5-fe-run-dashboard`
  - `feature/naw-6-be-workflow-api`

## Jira 상태 규칙

- 브랜치 생성만으로는 상태를 바꾸지 않습니다.
- PR이 열리면 Jira를 `진행 중`으로 전환합니다.
- PR이 머지되면 Jira를 `완료`로 전환합니다.
- PR이 닫히고 머지되지 않으면 필요 시 `해야 할 일`로 되돌립니다.

## 명령 예시

```bash
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli sync-jira-pr --prd input/prd.md --issue-key NAW-5 --pr-state OPEN
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli sync-jira-pr --prd input/prd.md --issue-key NAW-5 --pr-state MERGED
```
