#!/usr/bin/env bash
# feature-gate.sh
# PreToolUse Write/Edit 시 실행 — /feature 파이프라인 강제
#
# 차단 조건:
#   1. main/dev/master 브랜치에서 구현 코드(*.kt, *.java, *.ts, *.tsx) 작성
#   2. .feature-pipeline-state.json 이 없거나 step 이 APPROVED/IMPLEMENTING 미만인 상태에서 구현 코드 작성

set -euo pipefail

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c "
import json, sys
d = json.load(sys.stdin)
print(d.get('tool_input', {}).get('file_path', ''))
" 2>/dev/null || echo "")

# ── 구현 코드 파일만 검사 ────────────────────────────────────────────────────
if ! echo "$FILE_PATH" | grep -qE '\.(kt|java|ts|tsx|vue)$'; then
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}'
  exit 0
fi

# 테스트 파일은 자유롭게 허용
if echo "$FILE_PATH" | grep -qE '(/test/|\.test\.|\.spec\.)'; then
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}'
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-.}"

# FILE_PATH 기준 실제 git repo root 탐색 (서브레포 지원)
FILE_GIT_ROOT=$(git -C "$(dirname "$FILE_PATH")" rev-parse --show-toplevel 2>/dev/null || echo "")
GIT_DIR="${FILE_GIT_ROOT:-$PROJECT_DIR}"
BRANCH=$(git -C "$GIT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

# ── 1. main/dev/master 브랜치 차단 ──────────────────────────────────────────
if echo "$BRANCH" | grep -qE '^(main|master|dev)$'; then
  MSG="🚫 구현 코드를 ${BRANCH} 브랜치에 직접 작성할 수 없습니다.

파일: ${FILE_PATH}
브랜치: ${BRANCH}

해결 방법:
  1. /feature 파이프라인을 통해 TPM 분석 + 사용자 승인을 먼저 받으세요.
  2. 승인 후 티켓 브랜치를 생성하세요: git checkout -b feat/<ticket-id>"
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}' \
    "$(echo "$MSG" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read())[1:-1])')"
  exit 0
fi

# ── 2. 파이프라인 상태 확인 ──────────────────────────────────────────────────
# 파이프라인 상태 파일: 파일 git root 먼저 확인, 없으면 PROJECT_DIR
STATE_FILE="${GIT_DIR}/.feature-pipeline-state.json"
if [ ! -f "$STATE_FILE" ]; then
  STATE_FILE="${PROJECT_DIR}/.feature-pipeline-state.json"
fi

if [ ! -f "$STATE_FILE" ]; then
  MSG="🚫 /feature 파이프라인이 시작되지 않았습니다.

파일: ${FILE_PATH}
브랜치: ${BRANCH}

구현 코드를 작성하려면:
  1. /feature <prd.md 경로> 로 파이프라인을 시작하세요.
  2. Step 0(PRD 리뷰) → Step 1(TPM 분석) → Step 1-C(사용자 승인) 완료 후
  3. 티켓 브랜치에서 구현을 시작하세요."
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}' \
    "$(echo "$MSG" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read())[1:-1])')"
  exit 0
fi

STEP=$(python3 -c "
import json, sys
try:
    d = json.load(open('${STATE_FILE}'))
    print(d.get('step', 'NONE'))
except:
    print('NONE')
" 2>/dev/null || echo "NONE")

ALLOWED_STEPS="APPROVED IMPLEMENTING"
if ! echo "$ALLOWED_STEPS" | grep -qw "$STEP"; then
  TICKET=$(python3 -c "
import json, sys
try:
    d = json.load(open('${STATE_FILE}'))
    print(d.get('currentTicket', ''))
except:
    print('')
" 2>/dev/null || echo "")
  MSG="🚫 /feature 파이프라인 Step 1-C 사용자 승인이 완료되지 않았습니다.

파일: ${FILE_PATH}
현재 파이프라인 상태: ${STEP}
현재 티켓: ${TICKET:-없음}

구현 코드는 사용자 승인(APPROVED) 후에만 작성할 수 있습니다.
파이프라인을 계속 진행하세요."
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}' \
    "$(echo "$MSG" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read())[1:-1])')"
  exit 0
fi

echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}'
