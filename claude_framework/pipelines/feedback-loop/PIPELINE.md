# Feedback Loop Pipeline

메타-피드백 루프 운영 절차. 코드 산출물 리뷰(pr-senior-review)와 별개로 **참조한 command/skill/rule 자체** 가 잘못돼 실패를 유발했는지 점검한다.

## 진입점

- 자동: `Stop` / `SubagentStop` 훅이 트리거 조건 충족 시 발화
- 수동: `/audit-feedback-loop` 또는 `claude_framework:audit-feedback-loop` 호출

## 트리거 조건 (정확히 5가지)

1. 직전 작업이 PR Senior Gate (`pr-senior-review.yml`) 에서 fail
2. 자동 재시도 N회 중 1회 이상 실패 (default N=3)
3. 룰 위반이 PreToolUse 훅(`harness-check.py`) 에서 차단
4. QA 보고서(`docs/qa/*.md`) 에 "후속 티켓" 항목이 신규 추가됨
5. **claim-without-action 감지** — 다음 패턴 중 하나 이상 (운영 사고 재발 방지):
   - 완료/통과/검증 단언 후 600초 내 동일 주제 도구 호출 없음
   - 사용자의 "했어?" / "확인했어?" / "정말?" 질문 직후 그제서야 도구 호출이 발생
   - "전 레포 적용" / "리팩토링 완료" 단언이 `pipelines/COMPLETION-RULE.md` 의 강제 산출물 없이 나옴

위 5개 외에는 발화하지 않는다 (비용 보호).

## 단계

### 1. 트리거 검증
- Stop/SubagentStop 훅이 `stop_hook_active` 환경 변수 확인 (재귀 차단)
- `stop_hook_active=1` 이면 즉시 종료
- 일일 발화 카운트 확인 (`docs/feedback-loop/proposals/<오늘날짜>-*.md` 5개 이상이면 종료)

### 2. process-reviewer 스폰
- 모델: sonnet
- SubagentStop 비활성 모드 (옵션 `disable_subagent_stop=true`)
- 입력:
  - 트리거 정보 (`{trigger, session_id, pr_url?}`)
  - 작업 로그 (참조 command/skill/rule 경로 포함)
  - 산출물 (PR diff, QA 문서, 빌드 로그)

### 3. 분석 (process-reviewer 내부)
- 참조 체인 추출: command → pipeline → agent → skill → rule
- 비교 기준 (PRD/ADR/harness-rules.json) 과 대조
- 가설 → 검증 → 권장 수정

### 4. 제안 파일 생성
- 위치: `docs/feedback-loop/proposals/<YYYYMMDD>-<topic-slug>.md`
- frontmatter 강제 (trigger / session_id / risk / status=proposed)
- diff 형식 권장 수정 포함
- **자동 .md 수정 금지** — 제안 파일만

### 5. 사람·main-orchestrator 검토
다음 중 하나로 처리:
- **승인**: feedback-loop-guardian 이 `refactor/feedback/<date>-<topic>` 브랜치 생성 → 권장 수정 적용 → PR 생성 → 사람 머지
- **보류**: `docs/feedback-loop/proposals/archived/` 로 이동
- **기각**: `docs/feedback-loop/proposals/closed/` 로 이동, frontmatter 에 `closed_reason` 추가

### 6. 효과 측정 (nightly)
- feedback-loop-guardian 이 03:30 KST 에 발화
- 머지된 제안 PR 추적 → 같은 trigger 재발 빈도 측정
- 결과: `effective` / `partial` / `ineffective` 표시
- `ineffective` 가 2회 연속이면 추가 분석 필요 (Issue 자동 발행)

### 7. 자기 보정
- 효과 미흡 → 새 제안 생성 (다른 가설로)
- 일일 상한·재시도 상한·비용 상한 항상 적용

## 산출물 위치

| 산출물 | 경로 |
|--------|------|
| 제안 파일 | `docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md` |
| 보류 제안 | `docs/feedback-loop/proposals/archived/` |
| 기각 제안 | `docs/feedback-loop/proposals/closed/` |
| 건강 보고서 | `pipelines/feedback-loop/<YYYY-MM-DD>-health.md` |
| 머지된 변경 | `refactor/feedback/<date>-<topic>` 브랜치 PR |

## 안전 장치

- `stop_hook_active` 가드 (재귀 차단)
- 일일 발화 상한 5회
- process-reviewer 의 `.md` 직접 수정 금지 (정책 + 코드 양쪽 강제)
- 비교 기준 없는 LLM 의견만으로 제안 금지
- 자동 PR 머지 금지 (항상 사람 승인)
- `git_upstream_guard` 와 결합해 보호 브랜치 사고 방지

## 책임 매트릭스

| 단계 | 책임자 |
|------|--------|
| 트리거 검증 | Stop/SubagentStop 훅 (settings.json) |
| 분석·제안 | `agents/process-reviewer.md` |
| PR 생성 | `agents/feedback-loop-guardian.md` |
| 효과 측정 | `agents/feedback-loop-guardian.md` (nightly) |
| 머지 결정 | 사람 |

## 참고

- 상위 설계: `REFACTOR.md` §4 9~11단계, §5.2
- 출력물 리뷰(별도 축): `pipelines/pr-review/PIPELINE.md`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`pipelines/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
