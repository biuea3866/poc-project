# Feedback Loop Proposals

`process-reviewer` 에이전트가 메타-피드백 루프 트리거 충족 시 생성하는 제안 파일 보관소.

## 디렉토리 구조

```
proposals/
├── README.md                   # 이 파일
├── <YYYYMMDD>-<topic>.md       # status: proposed (현재 검토 대기)
├── archived/                   # 보류된 제안
└── closed/                     # 기각된 제안 (closed_reason 기록)
```

## 제안 파일 형식

frontmatter 필수:
```yaml
---
trigger: pr_senior_fail | retry_failure | rule_blocked | qa_followup
session_id: <세션 ID>
pr_url: <관련 PR URL, 있으면>
created_at: <ISO 8601>
risk: low | med | high
status: proposed | archived | closed
closed_reason: <기각 사유, status=closed 일 때만>
---
```

## 처리 흐름

1. process-reviewer 가 `<YYYYMMDD>-<topic>.md` 생성 (status=proposed)
2. main-orchestrator 또는 사람이 검토:
   - **승인** → feedback-loop-guardian 이 `refactor/feedback/<date>-<topic>` 브랜치로 PR 생성 → 사람 머지
   - **보류** → `archived/` 로 이동, status=archived
   - **기각** → `closed/` 로 이동, status=closed + closed_reason
3. nightly feedback-loop-guardian 이 머지된 제안의 효과 측정 (재발 빈도)

## 절대 금지

- `.claude/`, `.analysis/`, `agents/`, `skills/`, `commands/` 의 `.md` 파일을 자동 수정
- 비교 기준 (PRD/ADR/harness-rules.json) 없이 LLM 의견만으로 제안
- 자동 PR 머지 (항상 사람 승인)
- 일일 5개 초과 발행 (process-reviewer 가 자체 종료해야 함)

## 참고

- 워크플로우: `.analysis/feedback-loop/PIPELINE.md`
- 생성 책임: `agents/process-reviewer.md`
- 효과 측정: `agents/feedback-loop-guardian.md`
- 상위 설계: `REFACTOR.md` §4 9~11단계
