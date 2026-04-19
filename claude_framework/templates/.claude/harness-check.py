#!/usr/bin/env python3
"""
claude_framework 템플릿 하네스 체커.

Claude Code PreToolUse 훅에서 호출되어 harness-rules.json의 룰을 검증합니다.

사용법:
  CLAUDE_TOOL_INPUT 환경변수(JSON)를 읽어 차단 여부를 결정합니다.
  python3 harness-check.py <check_type>

check_type:
  file-guard     — Write/Edit 시 민감 파일 차단
  code-pattern   — Write/Edit 시 금지 패턴 검출
  git-guard      — Bash 시 위험한 git 명령 차단
  build-check    — Bash 결과에서 빌드 실패 감지 (PostToolUse)

종료 코드:
  0 = 통과
  2 = 차단 (BLOCKED — Claude에게 에러 메시지 반환)
"""

import json
import os
import re
import sys
from fnmatch import fnmatch
from pathlib import Path

# 템플릿 루트 (이 스크립트가 있는 .claude/의 부모)
FRAMEWORK_ROOT = Path(__file__).parent.parent.resolve()


def in_scope() -> bool:
    """현재 cwd가 claude_framework 내부에 있을 때만 훅을 적용."""
    try:
        cwd = Path.cwd().resolve()
        return str(cwd).startswith(str(FRAMEWORK_ROOT))
    except Exception:
        return False


def load_rules():
    rules_path = FRAMEWORK_ROOT / ".claude" / "harness-rules.json"
    if not rules_path.exists():
        return None
    with open(rules_path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_tool_input() -> dict:
    raw = os.environ.get("CLAUDE_TOOL_INPUT", "")
    if not raw:
        try:
            raw = sys.stdin.read()
        except Exception:
            raw = ""
    if not raw:
        return {}
    try:
        data = json.loads(raw)
        if isinstance(data, dict) and "tool_input" in data:
            return data["tool_input"]
        return data
    except json.JSONDecodeError:
        return {}


def get_tool_result() -> dict:
    raw = os.environ.get("CLAUDE_TOOL_RESULT", "")
    if not raw:
        return {}
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {}


def block(message: str) -> None:
    sys.stderr.write(f"[harness-check] {message}\n")
    sys.exit(2)


def check_file_guard(rules: dict, tool_input: dict) -> None:
    guard = rules.get("file_guard") or {}
    file_path = tool_input.get("file_path") or ""
    if not file_path:
        return
    filename = Path(file_path).name.lower()
    ext = Path(file_path).suffix.lower()

    for blocked_ext in guard.get("blocked_extensions", []):
        if ext == blocked_ext or filename.endswith(blocked_ext):
            block(guard.get("message", f"blocked extension: {blocked_ext}"))

    for blocked_name in guard.get("blocked_filenames", []):
        if filename == blocked_name.lower():
            block(guard.get("message", f"blocked filename: {blocked_name}"))


def _glob_match(path: str, pattern: str) -> bool:
    if "/" in pattern or "**" in pattern:
        return fnmatch(path, pattern)
    return fnmatch(Path(path).name, pattern)


def check_code_pattern(rules: dict, tool_input: dict) -> None:
    patterns = (rules.get("forbidden_patterns") or {}).get("rules", [])
    file_path = tool_input.get("file_path") or ""
    content = (
        tool_input.get("content")
        or tool_input.get("new_string")
        or ""
    )
    if not file_path or not content:
        return

    errors: list[str] = []
    for rule in patterns:
        file_glob = rule.get("file_glob", "*")
        if not _glob_match(file_path, file_glob):
            continue
        exclude_glob = rule.get("exclude_glob")
        if exclude_glob and _glob_match(file_path, exclude_glob):
            continue
        try:
            if re.search(rule["pattern"], content):
                severity = rule.get("severity", "error")
                prefix = "ERROR" if severity == "error" else severity.upper()
                errors.append(
                    f"[{prefix}] {rule.get('id', '?')}: {rule.get('message', '')}"
                )
        except re.error:
            continue

    if errors:
        block("금지 패턴 검출\n  " + "\n  ".join(errors))


def check_git_guard(rules: dict, tool_input: dict) -> None:
    guard = rules.get("git_guard") or {}
    command = tool_input.get("command") or ""
    if not command:
        return
    for blocked in guard.get("blocked_commands", []):
        pattern = blocked.get("pattern")
        if not pattern:
            continue
        try:
            if re.search(pattern, command):
                block(blocked.get("message", f"blocked git command: {pattern}"))
        except re.error:
            continue


def check_build(rules: dict) -> None:
    result = get_tool_result()
    stdout = result.get("stdout", "") or ""
    stderr = result.get("stderr", "") or ""
    combined = stdout + "\n" + stderr
    signals = [
        r"BUILD FAILED",
        r"Compilation error",
        r"error:\s+error\[",
        r"FAILURE:\s+Build failed",
    ]
    for sig in signals:
        if re.search(sig, combined):
            sys.stderr.write(
                "[harness-check] 빌드 실패 감지 — 에러를 먼저 해결하고 진행하세요.\n"
            )
            return


def main() -> int:
    if not in_scope():
        return 0

    check_type = sys.argv[1] if len(sys.argv) > 1 else ""
    rules = load_rules()
    if not rules:
        return 0

    tool_input = get_tool_input()

    if check_type == "file-guard":
        check_file_guard(rules, tool_input)
    elif check_type == "code-pattern":
        check_code_pattern(rules, tool_input)
    elif check_type == "git-guard":
        check_git_guard(rules, tool_input)
    elif check_type == "build-check":
        check_build(rules)
    else:
        sys.stderr.write(f"[harness-check] unknown check: {check_type}\n")
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())