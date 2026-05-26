#!/usr/bin/env bash
# Graceful hook launcher.
#
# Why this exists:
#   Claude Code PreToolUse hooks block the tool call when the hook command
#   exits with code 2. If a referenced hook script is moved/deleted, the
#   Python interpreter exits with code 2 ("can't open file"), which then
#   silently blocks every Write/Edit/Bash call — creating a deadlock
#   where the harness cannot repair itself.
#
# What this does:
#   - If the target script file is missing, exit 0 (silently pass).
#   - Otherwise, exec the script and propagate its exit code (so a
#     legitimate exit 2 still blocks the tool call as designed).
#
# Usage in settings.json:
#   "command": "bash <abs-path>/.claude/hooks/_run.sh <abs-path>/<script>.py [args...]"
#
# Conventions:
#   - .py is launched with python3; .sh is launched with bash.
#   - stdin (hook payload JSON) is passed through unchanged.
#   - stderr is preserved so blocking messages still reach the user.

set -u

script="${1:-}"
shift || true

if [ -z "$script" ]; then
    # No script specified — nothing to do; do not block.
    exit 0
fi

if [ ! -f "$script" ]; then
    # Self-heal: target script missing → pass silently instead of deadlocking.
    exit 0
fi

case "$script" in
    *.py)  exec python3 "$script" "$@" ;;
    *.sh)  exec bash    "$script" "$@" ;;
    *)     exec "$script" "$@" ;;
esac
