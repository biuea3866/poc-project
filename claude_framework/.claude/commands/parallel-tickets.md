---
description: 03-tickets.md의 티켓을 git worktree + agent team으로 병렬 구현. 팀장=opus4.7, 팀원=sonnet4.6
argument-hint: <path to 03-tickets.md> [max-parallel=4]
---

# Parallel Ticket Execution

03-tickets.md의 티켓을 **종속성 없는 것들부터 병렬로** 구현한다. 각 티켓은 별도 git worktree + 별도 에이전트 인스턴스에서 격리 실행.

**입력**
- 티켓 파일: $1
- 동시 실행 최대치: ${2:-4}

## 팀 구성 (모델 분할)

| 역할 | 에이전트 | 모델 | 책임 |
|---|---|---|---|
| **팀장(Lead)** | `pipeline-runner` | **opus** (Opus 4.7) | 티켓 큐 관리, 의존성 해결, 팀원 모니터링, 충돌 조정, 최종 머지 판단 |
| **BE 팀원(Worker)** | `be-implementer` | **sonnet** (Sonnet 4.6) | 개별 티켓 TDD 구현 |
| **FE 팀원(Worker)** | `fe-implementer` | **sonnet** (Sonnet 4.6) | 개별 티켓 TDD 구현 |
| **리뷰어** | `pr-reviewer` | **sonnet** | 팀원 PR 자동 리뷰 |

## 실행 절차 (팀장 시점)

### 1. 티켓 파싱
- `$1` 읽어서 YAML 티켓 블록 파싱 (id, deps, labels, estimate)
- 종속성 그래프 구축 → topological sort

### 2. Wave 계산
```
Wave 0: deps 없음              (병렬 가능)
Wave 1: Wave 0만 deps          (병렬 가능)
Wave 2: Wave 0/1만 deps        (병렬 가능)
...
```
동일 wave 안에서 최대 `${2:-4}`개까지 병렬 실행.

### 3. Worktree 생성 (각 티켓당)
```bash
BRANCH="feature/<ticket-id>-<slug>"
WORKTREE="claude_framework/worktrees/<ticket-id>"
git -C <repo> worktree add -b "$BRANCH" "$WORKTREE" main
```

### 4. Worker 에이전트 병렬 스폰
각 티켓에 대해 `Agent` 툴 호출 — **한 메시지에 여러 Agent 호출을 묶어 병렬화**:

```
Agent(
  subagent_type="be-implementer",      # labels에 따라 be/fe 선택
  model="sonnet",                       # Sonnet 4.6 강제
  isolation="worktree",                 # 격리 worktree
  name="worker-<ticket-id>",            # SendMessage로 중간 지시 가능
  description="<ticket-id> 구현",
  prompt=<<<
    티켓: <id> — <title>
    Acceptance Criteria: ...
    TC: [TC-01, TC-02]

    `tdd-loop` 스킬을 따라 Red→Green→Refactor로 구현하라.
    feature 브랜치 위에서만 작업하고, 커밋은 수행하되 push는 금지.
    완료 시 변경 파일 목록과 테스트 결과를 반환하라.
  >>>,
  run_in_background=true
)
```

### 5. 팀장 모니터링
- 팀원이 background 완료 알림을 보내면 결과 수집
- 실패/차단 상황 발생 시:
  - 단순 문제는 `SendMessage`로 팀원에게 수정 지시
  - 설계 이슈는 `project-analyst` 호출해 재설계
  - 하네스 룰 위반은 팀원에게 재작업 지시
- 동일 파일 충돌 위험 감지 시 wave 재편성

### 6. 팀원 완료 → PR 생성 + 리뷰
각 완료 티켓마다:
```
- worktree에서 push
- gh pr create
- Agent(subagent_type="pr-reviewer", model="sonnet", ...)로 자동 리뷰
- FE/DevOps는 pr-reviewer가 approve 시 자동 머지
- BE는 사람 대기 (comment만)
```

### 7. Wave 완료 → 다음 Wave로
- 현재 wave의 모든 티켓이 머지(또는 승인 대기)되면 다음 wave 시작
- `git worktree remove` 로 완료된 worktree 정리

### 8. 최종 보고 (팀장)
```
## Execution Summary
- 총 티켓: N
- 완료: X (머지 Y / 사람대기 Z)
- 실패: W (원인별)
- 총 소요 시간: ...
- Wave별 소요: W0=.., W1=..

## Per-ticket
| id | assignee(agent) | status | branch | PR | notes |
```

## 안전장치

### 병렬 제한
- 동시 worktree 개수 ≤ `${2:-4}`
- 동일 파일을 건드리는 티켓은 **같은 wave에 넣지 않음** (팀장이 사전 체크)

### 에러 격리
- 한 worker 실패가 다른 worker에 전파되지 않도록 `run_in_background=true`로 독립 실행
- 실패한 worktree는 `git worktree remove --force` + 브랜치 유지(디버깅용)

### 하네스 보장
- 각 worktree에도 `.claude/settings.json` 훅이 상속됨 (프로젝트 루트 기준)
- Worker가 금지 패턴을 쓰면 PreToolUse 훅이 차단 → 재작업

### 모델 강제
- 팀장: **opus** (복잡한 의사결정)
- 팀원/리뷰어: **sonnet** (속도/비용)
- 코드 내 `Agent({model: "opus"})` / `Agent({model: "sonnet"})`로 명시 전달

## 시작
`$1`의 티켓 파일을 파싱하고 Wave 0부터 실행 시작해줘. 최종 보고는 위 "Execution Summary" 포맷으로.