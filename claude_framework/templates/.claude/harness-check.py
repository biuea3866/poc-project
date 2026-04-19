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
    """
    훅 적용 스코프 판정:
      - 개발 모드: cwd가 FRAMEWORK_ROOT 내부 (플러그인 자체 개발)
      - 플러그인 모드: cwd에 .claude/harness-rules.json 또는 harness-rules.local.json 존재
    """
    try:
        cwd = Path.cwd().resolve()
        if str(cwd).startswith(str(FRAMEWORK_ROOT)):
            return True
        claude_dir = cwd / ".claude"
        return (
            (claude_dir / "harness-rules.json").exists()
            or (claude_dir / "harness-rules.local.json").exists()
        )
    except Exception:
        return False


def _read_json(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {}


def _deep_merge(base: dict, overlay: dict) -> dict:
    """
    3-file 병합 규칙:
      - dict: deep merge (overlay가 이김)
      - list: base + overlay (dedupe by 'id' if rules)
      - primitive: overlay가 이김
      - 특수 키 _rule_overrides: base의 rules 중 동일 id 룰의 필드 재정의
      - 특수 키 _rule_disabled: 해당 id 룰 제거
    """
    result = dict(base)
    for key, value in overlay.items():
        if key.startswith("_rule_"):
            continue  # 특수 키는 별도 처리
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = _deep_merge(result[key], value)
        elif key in result and isinstance(result[key], list) and isinstance(value, list):
            result[key] = _merge_rule_list(result[key], value)
        else:
            result[key] = value
    return result


def _merge_rule_list(base_list: list, overlay_list: list) -> list:
    """룰 배열 병합 — id 기준 dedupe, overlay가 기존 id 덮어쓰기."""
    by_id: dict = {}
    order: list = []
    for item in base_list + overlay_list:
        if isinstance(item, dict) and "id" in item:
            if item["id"] not in by_id:
                order.append(item["id"])
            by_id[item["id"]] = item
        else:
            order.append(id(item))
            by_id[id(item)] = item
    return [by_id[key] for key in order]


def _apply_overrides_and_disables(merged: dict, overlays: list) -> dict:
    """_rule_overrides / _rule_disabled 특수 키 처리."""
    disabled_ids: set = set()
    field_overrides: dict = {}
    for overlay in overlays:
        disabled_ids.update(overlay.get("_rule_disabled", []) or [])
        for rule_id, fields in (overlay.get("_rule_overrides", {}) or {}).items():
            field_overrides.setdefault(rule_id, {}).update(fields)

    for section_key in ("forbidden_patterns", "layer_dependency"):
        section = merged.get(section_key)
        if not isinstance(section, dict):
            continue
        rules = section.get("rules")
        if not isinstance(rules, list):
            continue
        new_rules = []
        for rule in rules:
            if not isinstance(rule, dict):
                new_rules.append(rule)
                continue
            rule_id = rule.get("id")
            if rule_id in disabled_ids:
                continue
            if rule_id in field_overrides:
                overridden = dict(rule)
                overridden.update(field_overrides[rule_id])
                if overridden.get("disabled") is True:
                    continue
                new_rules.append(overridden)
            else:
                new_rules.append(rule)
        section["rules"] = new_rules
    return merged


def load_rules():
    """
    3-레이어 병합:
      1. plugin base   — ~/.claude/plugins/claude-framework/.claude/harness-rules.json
                          또는 FRAMEWORK_ROOT/.claude/harness-rules.json (개발 모드)
      2. project       — $CWD/.claude/harness-rules.json  (팀 공유, git tracked)
      3. local         — $CWD/.claude/harness-rules.local.json (개인, gitignore)
    """
    plugin_paths = [
        Path(os.path.expanduser("~/.claude/plugins/claude-framework/.claude/harness-rules.json")),
        FRAMEWORK_ROOT / ".claude" / "harness-rules.json",
    ]
    plugin_base: dict = {}
    for candidate in plugin_paths:
        if candidate.exists():
            plugin_base = _read_json(candidate)
            break

    cwd = Path.cwd().resolve()
    project_rules = _read_json(cwd / ".claude" / "harness-rules.json")
    local_rules = _read_json(cwd / ".claude" / "harness-rules.local.json")

    # 플러그인 base와 프로젝트가 같은 파일이면 중복 병합 방지
    if plugin_base is project_rules or (
        plugin_paths and plugin_paths[-1] == cwd / ".claude" / "harness-rules.json"
    ):
        project_rules = {} if plugin_paths[-1] == cwd / ".claude" / "harness-rules.json" else project_rules

    if not plugin_base and not project_rules and not local_rules:
        return None

    merged = _deep_merge(plugin_base, project_rules)
    merged = _deep_merge(merged, local_rules)
    merged = _apply_overrides_and_disables(merged, [project_rules, local_rules])
    return merged


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