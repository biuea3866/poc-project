#!/usr/bin/env bash
# push 전 변경된 Gradle 모듈의 테스트를 실행한다.
# - gradlew가 있는 모든 레포에서 동작
# - base 브랜치(origin/dev | origin/main | origin/master) 대비 변경된 top-level dir 중
#   build.gradle(.kts)를 가진 모듈만 테스트
# - 실패 시 push deny

set -u

emit_allow() {
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow","permissionDecisionReason":"%s"}}\n' "$1"
  exit 0
}

emit_deny() {
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}\n' "$1"
  exit 0
}

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
[ -z "$REPO_ROOT" ] && emit_allow "git repo 아님 - 테스트 스킵"
[ ! -f "$REPO_ROOT/gradlew" ] && emit_allow "Gradle 프로젝트 아님 - 테스트 스킵"

cd "$REPO_ROOT" || emit_allow "레포 루트 접근 실패 - 테스트 스킵"

BRANCH=$(git branch --show-current 2>/dev/null)
case "$BRANCH" in
  ""|dev|main|master) emit_allow "base 브랜치(${BRANCH:-none}) - 테스트 스킵" ;;
esac

BASE=""
for CAND in origin/dev origin/main origin/master; do
  if git rev-parse --verify "$CAND" >/dev/null 2>&1; then
    BASE="$CAND"
    break
  fi
done
[ -z "$BASE" ] && emit_allow "base 브랜치 미발견 - 테스트 스킵"

CHANGED=$(git diff --name-only "$BASE...HEAD" 2>/dev/null | awk -F/ '{print $1}' | sort -u)
[ -z "$CHANGED" ] && emit_allow "변경 파일 없음 - 테스트 스킵"

MODULES=""
while IFS= read -r DIR; do
  [ -z "$DIR" ] && continue
  if [ -f "$REPO_ROOT/$DIR/build.gradle.kts" ] || [ -f "$REPO_ROOT/$DIR/build.gradle" ]; then
    MODULES="$MODULES $DIR"
  fi
done <<< "$CHANGED"
[ -z "$MODULES" ] && emit_allow "변경된 Gradle 모듈 없음"

check_java_version() {
  [ -n "$1" ] && [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | head -1 | grep -q 'version "17'
}

if ! check_java_version "${JAVA_HOME:-}"; then
  for JDK in \
    /Users/${USER}/Library/Java/JavaVirtualMachines/corretto-17.0.4.1/Contents/Home \
    /Users/${USER}/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home \
    $(/usr/libexec/java_home -v 17 2>/dev/null); do
    if check_java_version "$JDK"; then
      export JAVA_HOME="$JDK"
      break
    fi
  done
fi

LOG_DIR="/tmp/claude-push-test"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

FAILED=""
SKIPPED=""
EXECUTED=""
for MOD in $MODULES; do
  LOG_FILE="$LOG_DIR/${TIMESTAMP}-${MOD}.log"
  {
    echo "=== push-test hook ==="
    echo "MOD=$MOD"
    echo "JAVA_HOME=$JAVA_HOME"
    echo "PWD=$(pwd)"
    echo "USER=$(whoami)"
    echo "PATH=$PATH"
    echo "===================="
  } > "$LOG_FILE"
  ./gradlew ":$MOD:test" >> "$LOG_FILE" 2>&1
  if grep -q "Task 'test' not found" "$LOG_FILE"; then
    SKIPPED="$SKIPPED $MOD"
    continue
  fi
  if tail -5 "$LOG_FILE" | grep -q 'BUILD SUCCESSFUL'; then
    EXECUTED="$EXECUTED $MOD"
  else
    FAILED="$FAILED $MOD"
  fi
done

if [ -n "$FAILED" ]; then
  emit_deny "테스트 실패:$FAILED. push를 차단합니다. 로그: $LOG_DIR/${TIMESTAMP}-*.log"
fi

REASON="테스트 통과"
[ -n "$EXECUTED" ] && REASON="$REASON (실행:$EXECUTED)"
[ -n "$SKIPPED" ] && REASON="$REASON (test task 없음:$SKIPPED)"
emit_allow "$REASON"
