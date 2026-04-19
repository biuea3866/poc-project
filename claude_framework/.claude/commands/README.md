# .claude/commands — Slash Commands

사용자가 `/<name> <args>`로 호출하는 얇은 UX 레이어. 본문은 파이프라인/에이전트/스킬을 호출하는 프롬프트이며, 절차/로직을 여기에 중복 작성하지 않는다.

## Command 카탈로그

| Command | 용도 | 내부 호출 |
|---|---|---|
| `/analyze-prd <link>` | PRD 파이프라인 실행 | pipeline-runner + `prd/` PIPELINE |
| `/plan-project <prd-path>` | 설계+TDD+티켓 파이프라인 | pipeline-runner + `project-analysis/` PIPELINE |
| `/split-tickets <feature-dir>` | 티켓 분해 단독 실행 | ticket-splitter + ticket-breakdown |
| `/tdd-implement <ticket> [be\|fe]` | 단일 티켓 TDD 구현 | be-implementer 또는 fe-implementer + tdd-loop |
| `/review-pr <num>` | PR 리뷰 | pr-reviewer + pr-review-checklist |
| `/audit-harness [path]` | 하네스 룰 전수 감사 | harness-auditor + harness-audit |
| **`/parallel-tickets <tickets.md> [max=4]`** | **티켓 병렬 구현 (worktree + agent team)** | **팀장=opus, 팀원=sonnet** |
| **`/init [--scan] [--classify-repos] [--stack=..]`** | **플러그인 설치 후 현재 프로젝트에 이식** | **convention-detective + repo-classifier** |

## 참조 방향 (재확인)

```
Command ──호출──> Pipeline ──참조──> Agent ──참조──> Skill
   └──호출──> Agent (파이프라인 밖 독립 액션)
```

**Pipeline이 Command를 참조하지 않는다.** Command는 UX 레이어이고, 내부 로직은 Pipeline/Agent/Skill이 책임.

## /parallel-tickets 특징

- **모델 분할**: 팀장=Opus 4.7(의사결정/조정), 팀원=Sonnet 4.6(실제 구현), 리뷰어=Sonnet 4.6(PR 리뷰)
- **격리**: 각 티켓은 `isolation: worktree`로 별도 체크아웃에서 실행
- **Wave 기반 스케줄링**: 종속성 없는 티켓부터 동시 실행, 최대 N개 병렬
- **동일 파일 충돌 방지**: 팀장이 wave 편성 시 파일 레벨 충돌 검사
- **하네스 룰 보장**: 각 worktree에서도 `.claude/settings.json` 훅 적용
- **실패 격리**: background 실행으로 한 worker 실패가 다른 worker에 전파 안 됨

## Command 파일 규약

```markdown
---
description: 한 줄 설명 (슬래시 자동완성에 표시됨)
argument-hint: <arg1> [optional-arg2]
---

<프롬프트 본문 — $1, $2, $ARGUMENTS 치환>
```

추가 가능 옵션:
- `model: opus | sonnet | haiku` — 이 커맨드 실행 시 메인 세션 모델 강제
- `allowed-tools: Read, Edit, Bash, Agent` — 실행 도구 제한