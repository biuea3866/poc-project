---
description: 메타-피드백 루프 수동 트리거 — 직전 작업 분석 후 docs/feedback-loop/proposals/ 에 제안 파일 생성
argument-hint: <trigger 사유 (선택)>
---

`process-reviewer` 에이전트를 수동으로 호출해 메타-피드백 트리거에 대응시켜줘.

자동 발화는 Stop/SubagentStop 훅이 담당하지만, 사용자가 직접 호출해야 하는 상황이 있다:
- 활성화 안 된 환경에서 점검
- 트리거 조건은 충족됐지만 일일 상한에 걸려 자동 발화 못 한 케이스
- 특정 PR/세션을 명시적으로 분석하고 싶을 때

**Trigger 사유**: $ARGUMENTS (선택, 자동 검출 우선)

## 단계

### 1. 트리거 검증
- 다음 중 하나라도 충족되는지 확인:
  - PR Senior Gate fail 이력
  - 자동 재시도 N회 중 1회 이상 실패
  - PreToolUse 훅 차단 이력
  - QA 보고서 신규 후속 티켓
- 어느 것도 충족 안 되면 즉시 종료 (메시지 출력)

### 2. 일일 상한 확인
- `ls docs/feedback-loop/proposals/$(date +%Y%m%d)-*.md | wc -l`
- 5개 이상이면 즉시 종료

### 3. process-reviewer 스폰
- 모델: sonnet
- SubagentStop 비활성 (재귀 차단)
- 입력:
  - 트리거 정보 ($ARGUMENTS 또는 자동 검출)
  - 작업 로그 (참조 command/skill/rule 경로)
  - 산출물 (PR diff / QA 문서 / 빌드 로그)

### 4. 분석 → 제안 파일 생성
process-reviewer 가 다음 형식으로 `docs/feedback-loop/proposals/<YYYYMMDD>-<topic>.md` 작성:

```markdown
---
trigger: <trigger>
session_id: <ID>
pr_url: <URL, 있으면>
created_at: <ISO 8601>
risk: low | med | high
status: proposed
---

# 제안: <한 줄 요약>

## 트리거 배경
## 참조 체인
## 실패 원인 분석
## 권장 수정 (diff 형식)
## 영향 범위
## 검증 계획
```

### 5. 보고
사용자에게 생성된 제안 파일 경로 + 핵심 권장 수정 1~2 줄 요약 보고.

## 절대 금지

- `.md` / `harness-rules.json` 직접 수정
- 비교 기준 (PRD/ADR/harness-rules.json) 없이 LLM 의견만으로 제안
- 일일 상한 초과 발화

## 후속

- 사람이 제안 파일 검토 → 승인 시 `feedback-loop-guardian` 에이전트가 `refactor/feedback/<date>` PR 생성
- 보류/기각은 `archived/`/`closed/` 로 이동

## 참고

- 워크플로우: `commands/audit-feedback-loop.md`
- 에이전트: `agents/process-reviewer.md`
- 안전 장치: `REFACTOR.md` §7 안티패턴
