#!/usr/bin/env bash
# feature-tdd-gate.sh
# PreToolUse git push 시 실행 — TDD 강제
#
# 차단 조건:
#   src/main/**/*.kt (또는 */src/main/**/*.kt) 에 변경이 있는데
#   src/test/**/*.kt (또는 */src/test/**/*.kt) 에 변경이 전혀 없는 경우

set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-.}"

# push 명령인지 확인
INPUT=$(cat)
CMD=$(echo "$INPUT" | python3 -c "
import json, sys
d = json.load(sys.stdin)
print(d.get('tool_input', {}).get('command', ''))
" 2>/dev/null || echo "")

if ! echo "$CMD" | grep -q "git push"; then
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}'
  exit 0
fi

# base 브랜치 대비 변경 파일 목록
BASE=$(git -C "$PROJECT_DIR" symbolic-ref refs/remotes/origin/HEAD 2>/dev/null \
      | sed 's|refs/remotes/origin/||' || echo "dev")
CHANGED=$(git -C "$PROJECT_DIR" diff "origin/${BASE}...HEAD" --name-only 2>/dev/null || \
          git -C "$PROJECT_DIR" diff HEAD~1 --name-only 2>/dev/null || echo "")

MAIN_CHANGED=$(echo "$CHANGED" | grep -E 'src/main/.*\.(kt|java)$' || true)
TEST_CHANGED=$(echo "$CHANGED" | grep -E 'src/test/.*\.(kt|java)$' || true)

if [ -n "$MAIN_CHANGED" ] && [ -z "$TEST_CHANGED" ]; then
  MAIN_COUNT=$(echo "$MAIN_CHANGED" | wc -l | tr -d ' ')
  MSG="🚫 TDD 위반 — 테스트 없는 구현 코드 push 차단.

구현 파일 변경: ${MAIN_COUNT}개
테스트 파일 변경: 0개

변경된 구현 파일:
$(echo "$MAIN_CHANGED" | head -10 | sed 's/^/  - /')

구현 코드 변경에는 반드시 테스트가 함께 있어야 합니다.
  1. 관련 테스트를 src/test/ 하위에 작성하세요.
  2. ./gradlew test 통과를 확인하세요.
  3. 다시 push하세요."
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}' \
    "$(echo "$MSG" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read())[1:-1])')"
  exit 0
fi

echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}'
