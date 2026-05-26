#!/usr/bin/env bash
# PreToolUse Bash(git push:*) 훅.
# bash에서 PR/브랜치 게이팅을 모두 처리하고, 리뷰가 필요한 경우만 claude -p 호출.
# 출력: stdout에 hookSpecificOutput JSON 한 줄.
# 트레이스: /tmp/claude-push-review-trace.log (append)
set -u

TRACE_LOG=/tmp/claude-push-review-trace.log
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$TRACE_LOG"; }

# 발동 기록 (필수 — emit보다 먼저)
log "hook fired pid=$$ pwd=$(pwd) repo=$(git rev-parse --show-toplevel 2>/dev/null) branch=$(git branch --show-current 2>/dev/null)"

emit_allow() {
  local reason=$1
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow","permissionDecisionReason":%s}}\n' "$(printf '%s' "$reason" | jq -Rs .)"
}

emit_deny() {
  local reason=$1
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":%s}}\n' "$(printf '%s' "$reason" | jq -Rs .)"
}

# 훅은 harness CWD(워크스페이스 루트)에서 실행되므로, push 중인 실제 레포를 직접 찾아야 한다.
# tool_input에서 command를 받을 수 없으니, `pwd`가 워크스페이스 루트면 스킵하는 게 안전.
# 하지만 여기서는 일단 현재 git repo 기준으로 진행.
REPO=$(git rev-parse --show-toplevel 2>/dev/null)
BRANCH=$(git branch --show-current 2>/dev/null)

if [ -z "$REPO" ] || [ -z "$BRANCH" ]; then
  log "skip (no repo or branch) repo=$REPO branch=$BRANCH"
  emit_allow "git 정보 없음 — 리뷰 스킵"
  exit 0
fi

case "$BRANCH" in
  dev|main|master)
    log "skip (base branch) branch=$BRANCH repo=$REPO"
    emit_allow "base 브랜치($BRANCH) — 리뷰 스킵"
    exit 0
    ;;
esac

cd "$REPO" || { log "cd fail: $REPO"; emit_allow "cd 실패 — 리뷰 스킵"; exit 0; }

PR_DATA=$(gh pr view "$BRANCH" --json state,number,baseRefName 2>/dev/null)
if [ -z "$PR_DATA" ]; then
  log "skip (no PR) branch=$BRANCH repo=$REPO"
  emit_allow "PR 미생성 — 리뷰 스킵"
  exit 0
fi

PR_STATE=$(echo "$PR_DATA" | jq -r .state)
PR_NUMBER=$(echo "$PR_DATA" | jq -r .number)
BASE=$(echo "$PR_DATA" | jq -r .baseRefName)

if [ "$PR_STATE" != "OPEN" ]; then
  log "skip (PR #$PR_NUMBER $PR_STATE)"
  emit_allow "PR #$PR_NUMBER ${PR_STATE} — 리뷰 스킵"
  exit 0
fi

git fetch origin --quiet 2>/dev/null

CHANGED_FILES=$(git diff "origin/$BASE...HEAD" --name-only 2>/dev/null)
COMMITS=$(git log "origin/$BASE..HEAD" --oneline 2>/dev/null)
DIFF=$(git diff "origin/$BASE...HEAD" 2>/dev/null)

if [ -z "$DIFF" ]; then
  log "skip (empty diff) PR #$PR_NUMBER"
  emit_allow "PR #$PR_NUMBER 변경 없음 — 리뷰 스킵"
  exit 0
fi

DIFF_BYTES=$(printf '%s' "$DIFF" | wc -c | tr -d ' ')
MAX_DIFF_BYTES=500000
if [ "$DIFF_BYTES" -gt "$MAX_DIFF_BYTES" ]; then
  log "skip (diff too large: $DIFF_BYTES) PR #$PR_NUMBER"
  emit_allow "PR #$PR_NUMBER diff 과대($DIFF_BYTES bytes) — 리뷰 스킵"
  exit 0
fi

log "invoke claude -p for PR #$PR_NUMBER branch=$BRANCH repo=$REPO diff=$DIFF_BYTES"

PROMPT=$(cat <<PROMPT_EOF
PR #$PR_NUMBER 코드 리뷰. base=$BASE, branch=$BRANCH, repo=$REPO.

## 규칙 로드 (필수)
- 워크스페이스 루트 CLAUDE.md 읽기
- 변경 레포의 CLAUDE.md가 있으면 읽기 (prefix: $REPO)
- .claude/harness-rules.json이 있으면 규칙 로드

## 리뷰 기준
**🔴 Must Fix (deny)**
- NPE/무한루프/런타임 에러, 데이터 정합성 파괴, 보안 이슈, 크로스 서비스 breaking change
- CLAUDE.md 강제 규칙 위반 (OutputPort에 인프라 기술명, @Transactional in Repository, LocalDateTime, Entity 생성자 기본값, Repository 메서드 prefix, @Repository class에 JPAQueryFactory 등)
- 1:1 이관 티켓의 AS-IS 누락
- Anemic Domain/Aggregate 경계 침범, 불변 Entity에 setter 등 OOP/DDD 심각 위반
- !! 남용, lateinit var 남용 등 Kotlin 심각 오용

**🟡 Should Fix (allow, 사유에 요약)**
- 테스트 누락, N+1/full scan, 에러 핸들링 부족
- Kotlin 함수형 미적용, var 남용, 확장 함수 미사용
- SRP 위반, 함수 >30줄, 인자 >3개, 매직 넘버, DRY 위반
- Strategy/Factory/Builder 등 패턴 적용 기회

**🟢 무시**: 네이밍 취향, 포매팅

## 출력 (반드시 아래 JSON 한 줄, 그 외 절대 출력 금지)
차단:
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"🔴 [PR #$PR_NUMBER] <파일:라인 - 이슈> / ..."}}

통과:
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow","permissionDecisionReason":"PR #$PR_NUMBER 리뷰 통과. Should Fix: <요약>"}}

---
변경 파일:
$CHANGED_FILES

커밋:
$COMMITS

DIFF:
\`\`\`diff
$DIFF
\`\`\`
PROMPT_EOF
)

RESULT=$(printf '%s' "$PROMPT" | /opt/homebrew/bin/claude -p \
  --model sonnet \
  --allowed-tools "Read Grep Glob Bash(git:*) Bash(gh:*) Bash(cat:*) Bash(ls:*)" \
  --max-budget-usd 0.50 \
  --output-format text \
  2>>"$TRACE_LOG")

# 결과 JSON 추출: 마지막 라인에 hookSpecificOutput 포함하는 라인 탐색
JSON_LINE=$(echo "$RESULT" | grep -E '"hookSpecificOutput"' | tail -1)

if [ -n "$JSON_LINE" ] && echo "$JSON_LINE" | jq -e '.hookSpecificOutput.permissionDecision' >/dev/null 2>&1; then
  log "review complete PR #$PR_NUMBER decision=$(echo "$JSON_LINE" | jq -r .hookSpecificOutput.permissionDecision)"
  echo "$JSON_LINE"
else
  log "review parse fail PR #$PR_NUMBER result=$(echo "$RESULT" | head -5)"
  emit_allow "PR #$PR_NUMBER 리뷰 응답 파싱 실패 — 통과"
fi
