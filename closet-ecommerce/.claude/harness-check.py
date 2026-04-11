#!/usr/bin/env python3
"""
Closet 프로젝트 하네스 체커 — 중앙화된 규칙 검증 스크립트.

사용법:
  echo "$CLAUDE_TOOL_INPUT" | python3 harness-check.py <check_type>

check_type:
  file-guard     — Write/Edit 시 민감 파일 차단
  code-pattern   — Write/Edit 시 금지 패턴 검출
  git-guard      — Bash 시 위험한 git 명령 차단
  build-check    — Bash 결과에서 빌드 실패 감지

종료 코드:
  0 = 통과
  2 = 차단 (BLOCKED)
"""

import json
import os
import re
import sys
from fnmatch import fnmatch
from pathlib import Path

# closet-ecommerce 디렉토리에서만 실행
CLOSET_ROOT = Path(__file__).parent.parent
if "closet-ecommerce" not in str(Path.cwd()) and str(Path.cwd()) != str(CLOSET_ROOT.parent):
    sys.exit(0)


def load_rules():
    """harness-rules.json을 로드한다."""
    script_dir = Path(__file__).parent
    rules_path = script_dir / "harness-rules.json"
    if not rules_path.exists():
        return None
    with open(rules_path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_tool_input():
    """stdin에서 CLAUDE_TOOL_INPUT JSON을 파싱한다."""
    try:
        raw = os.environ.get("CLAUDE_TOOL_INPUT", "")
        if raw:
            return json.loads(raw)
    except (json.JSONDecodeError, KeyError):
        pass
    return {}


def get_tool_result():
    """stdin에서 CLAUDE_TOOL_RESULT JSON을 파싱한다."""
    try:
        raw = os.environ.get("CLAUDE_TOOL_RESULT", "")
        if raw:
            return json.loads(raw)
    except (json.JSONDecodeError, KeyError):
        pass
    return {}


def check_file_guard(rules, tool_input):
    """민감 파일 수정 차단."""
    guard = rules.get("file_guard")
    if not guard:
        return True, ""

    file_path = tool_input.get("file_path", "")
    if not file_path:
        return True, ""

    filename = Path(file_path).name.lower()
    ext = Path(file_path).suffix.lower()

    # 확장자 체크
    for blocked_ext in guard.get("blocked_extensions", []):
        if ext == blocked_ext or filename.endswith(blocked_ext):
            return False, guard["message"]

    # 파일명 체크
    for blocked_name in guard.get("blocked_filenames", []):
        if filename == blocked_name.lower():
            return False, guard["message"]

    return True, ""


def check_code_patterns(rules, tool_input):
    """금지 패턴 검출."""
    forbidden = rules.get("forbidden_patterns")
    if not forbidden:
        return True, ""

    file_path = tool_input.get("file_path", "")
    # Write의 경우 content, Edit의 경우 new_string 검사
    content = tool_input.get("content", "") or tool_input.get("new_string", "")

    if not file_path or not content:
        return True, ""

    violations = []
    for rule in forbidden.get("rules", []):
        # 파일 glob 매칭
        file_glob = rule.get("file_glob", "*")
        if not fnmatch(Path(file_path).name, file_glob):
            continue

        # 제외 glob 매칭
        exclude_glob = rule.get("exclude_glob")
        if exclude_glob and fnmatch(file_path, exclude_glob):
            continue

        # 패턴 매칭
        pattern = rule["pattern"]
        if re.search(pattern, content):
            severity = rule.get("severity", "error")
            violations.append(f"[{severity.upper()}] {rule['message']}")

    if violations:
        header = f"BLOCKED: {len(violations)}개 규칙 위반 감지"
        detail = "\n".join(f"  • {v}" for v in violations)
        return False, f"{header}\n{detail}"

    return True, ""


def check_git_guard(rules, tool_input):
    """위험한 git 명령 차단."""
    guard = rules.get("git_guard")
    if not guard:
        return True, ""

    command = tool_input.get("command", "")
    if not command:
        return True, ""

    for rule in guard.get("rules", []):
        pattern = rule["pattern"]
        if re.search(pattern, command):
            return False, rule["message"]

    return True, ""


def check_build_result(rules, tool_input, tool_result):
    """빌드 성공 여부 확인."""
    build = rules.get("build_check")
    if not build:
        return True, ""

    command = tool_input.get("command", "")
    if not command:
        return True, ""

    # Gradle 빌드 명령인지 확인
    is_gradle = False
    for gradle_cmd in build.get("gradle_commands", []):
        if gradle_cmd in command and ("gradlew" in command or "gradle" in command):
            is_gradle = True
            break

    if not is_gradle:
        return True, ""

    # 결과에서 성공 마커 확인
    stdout = tool_result.get("stdout", "")
    success_marker = build.get("success_marker", "BUILD SUCCESSFUL")
    if success_marker not in stdout:
        return False, build.get("failure_message", "BUILD FAILED")

    return True, ""


def main():
    if len(sys.argv) < 2:
        print("Usage: harness-check.py <check_type>", file=sys.stderr)
        sys.exit(1)

    check_type = sys.argv[1]
    rules = load_rules()
    if not rules:
        sys.exit(0)  # 규칙 파일 없으면 통과

    tool_input = get_tool_input()
    passed = True
    message = ""

    if check_type == "file-guard":
        passed, message = check_file_guard(rules, tool_input)

    elif check_type == "code-pattern":
        passed, message = check_code_patterns(rules, tool_input)

    elif check_type == "git-guard":
        passed, message = check_git_guard(rules, tool_input)

    elif check_type == "build-check":
        tool_result = get_tool_result()
        passed, message = check_build_result(rules, tool_input, tool_result)

    else:
        print(f"Unknown check type: {check_type}", file=sys.stderr)
        sys.exit(1)

    if not passed:
        print(message, file=sys.stderr)
        sys.exit(2)

    sys.exit(0)


if __name__ == "__main__":
    main()
