#!/usr/bin/env python3
"""
<PROJECT> 하네스 체커 — 중앙화된 규칙 검증 스크립트.

사용법:
  python3 harness-check.py <check_type>

check_type:
  file-guard      — Write/Edit 시 민감 파일 차단
  code-pattern    — Write/Edit 시 금지 패턴 검출
  git-guard       — Bash 시 위험한 git 명령 차단
  bash-file-guard — Bash 시 cat/echo/heredoc으로 코드 파일 생성 차단
  jira-guard      — Jira 코멘트 차단
  pipeline-gate   — 파이프라인 다음 페이즈 진입 전 이전 페이즈 게이트 통과 확인

종료 코드:
  0 = 통과
  2 = 차단 (BLOCKED)

3-file merge 룰 우선순위 (뒤가 이김):
  1. plugin base  — ~/.claude/plugins/claude-framework/.claude/harness-rules.json
  2. project      — .claude/harness-rules.json  (팀 공유, git tracked)
  3. local        — .claude/harness-rules.local.json  (개인, gitignore)
"""

import json
import os
import re
import subprocess
import sys
from datetime import date
from fnmatch import fnmatch
from pathlib import Path

# Script lives at .claude/hooks/code-rules/ — go up 4 to reach project root.
WORKSPACE_ROOT = Path(__file__).resolve().parent.parent.parent.parent
VIOLATION_LOG = WORKSPACE_ROOT / ".analysis" / "findings" / "auto" / "violation-counters.json"


# ──────────────────────────────────────────────
# 3-file merge helpers (from claude_framework)
# ──────────────────────────────────────────────

def _read_json(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return {}


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
            key = id(item)
            order.append(key)
            by_id[key] = item
    return [by_id[k] for k in order]


def _deep_merge(base: dict, overlay: dict) -> dict:
    """
    deep merge — overlay가 이김.
    특수 키 _rule_overrides / _rule_disabled는 _apply_overrides_and_disables에서 처리.
    """
    result = dict(base)
    for key, value in overlay.items():
        if key.startswith("_rule_"):
            continue
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = _deep_merge(result[key], value)
        elif key in result and isinstance(result[key], list) and isinstance(value, list):
            result[key] = _merge_rule_list(result[key], value)
        else:
            result[key] = value
    return result


def _apply_overrides_and_disables(merged: dict, overlays: list) -> dict:
    """_rule_overrides / _rule_disabled 특수 키 처리."""
    disabled_ids: set = set()
    field_overrides: dict = {}
    for overlay in overlays:
        disabled_ids.update(overlay.get("_rule_disabled", []) or [])
        for rule_id, fields in (overlay.get("_rule_overrides", {}) or {}).items():
            field_overrides.setdefault(rule_id, {}).update(fields)

    for section_key in ("forbidden_patterns", "layer_dependency", "git_guard", "git_guard_extended"):
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


def log_violation(rule_id: str, file_path: str) -> None:
    """Increment violation counter for a rule. Best-effort; fails silently."""
    if not rule_id:
        return
    try:
        VIOLATION_LOG.parent.mkdir(parents=True, exist_ok=True)
        if VIOLATION_LOG.exists():
            with open(VIOLATION_LOG, "r", encoding="utf-8") as f:
                data = json.load(f)
        else:
            data = {}
        entry = data.setdefault(rule_id, {"count": 0, "last_seen": "", "files": []})
        entry["count"] = entry.get("count", 0) + 1
        entry["last_seen"] = date.today().isoformat()
        files = entry.get("files", [])
        if file_path and file_path not in files:
            files.append(file_path)
            entry["files"] = files[-10:]  # keep last 10 unique
        with open(VIOLATION_LOG, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception:
        pass  # never block the hook on logging failure


def load_rules():
    """
    3-레이어 병합:
      1. plugin base  — ~/.claude/plugins/claude-framework/.claude/harness-rules.json
      2. project      — $CWD/.claude/harness-rules.json  (팀 공유)
      3. local        — $CWD/.claude/harness-rules.local.json (개인, gitignore)
    룰이 하나도 없으면 None 반환 (훅 비활성).
    """
    plugin_candidates = [
        Path(os.path.expanduser("~/.claude/plugins/claude-framework/.claude/harness-rules.json")),
    ]
    plugin_base: dict = {}
    for candidate in plugin_candidates:
        if candidate.exists():
            plugin_base = _read_json(candidate)
            break

    cwd = Path.cwd().resolve()
    project_rules = _read_json(cwd / ".claude" / "harness-rules.json")
    local_rules = _read_json(cwd / ".claude" / "harness-rules.local.json")

    if not plugin_base and not project_rules and not local_rules:
        return None

    merged = _deep_merge(plugin_base, project_rules)
    merged = _deep_merge(merged, local_rules)
    merged = _apply_overrides_and_disables(merged, [project_rules, local_rules])
    return merged


def read_hook_payload():
    """Claude Code PreToolUse hook payload from stdin (preferred) or env var (legacy)."""
    if not sys.stdin.isatty():
        try:
            raw = sys.stdin.read()
            if raw:
                data = json.loads(raw)
                return data.get("tool_input", {}) or {}, data.get("tool_name", "") or ""
        except (json.JSONDecodeError, ValueError):
            pass
    try:
        raw_input = os.environ.get("CLAUDE_TOOL_INPUT", "")
        tool_input = json.loads(raw_input) if raw_input else {}
        return tool_input, os.environ.get("CLAUDE_TOOL_NAME", "")
    except (json.JSONDecodeError, KeyError):
        return {}, ""


def emit_allow(reason: str = ""):
    out = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "allow"}}
    if reason:
        out["hookSpecificOutput"]["permissionDecisionReason"] = reason
    print(json.dumps(out))
    sys.exit(0)


def emit_deny(reason: str):
    out = {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }
    print(json.dumps(out))
    sys.exit(0)


def get_tool_input():
    """Backward-compat shim. Reads stdin (Claude Code hook payload) first; falls back to env."""
    payload, tool_name = read_hook_payload()
    if tool_name:
        os.environ["CLAUDE_TOOL_NAME"] = tool_name
    return payload


def check_file_guard(rules, tool_input):
    guard = rules.get("file_guard")
    if not guard:
        return True, ""

    file_path = tool_input.get("file_path", "")
    if not file_path:
        return True, ""

    filename = Path(file_path).name.lower()
    ext = Path(file_path).suffix.lower()

    for blocked_ext in guard.get("blocked_extensions", []):
        if ext == blocked_ext or filename.endswith(blocked_ext):
            return False, guard["message"]

    for blocked_name in guard.get("blocked_filenames", []):
        if filename == blocked_name.lower():
            return False, guard["message"]

    return True, ""


def check_code_patterns(rules, tool_input):
    forbidden = rules.get("forbidden_patterns")
    if not forbidden:
        return True, ""

    file_path = tool_input.get("file_path", "")
    content = tool_input.get("content", "") or tool_input.get("new_string", "")

    if not file_path or not content:
        return True, ""

    violations = []
    for rule in forbidden.get("rules", []):
        file_glob = rule.get("file_glob", "*")
        if not fnmatch(Path(file_path).name, file_glob):
            continue

        exclude_glob = rule.get("exclude_glob")
        if exclude_glob and fnmatch(file_path, exclude_glob):
            continue

        pattern = rule["pattern"]
        if re.search(pattern, content, re.MULTILINE):
            severity = rule.get("severity", "error")
            rule_id = rule.get("id", "unknown")
            violations.append((severity, rule["message"], rule_id))
            log_violation(rule_id, file_path)

    errors = [(msg, rid) for s, msg, rid in violations if s == "error"]
    warnings = [(msg, rid) for s, msg, rid in violations if s == "warning"]

    if warnings:
        for msg, _ in warnings:
            print(f"[WARNING] {msg}", file=sys.stderr)

    if errors:
        header = f"BLOCKED: {len(errors)}개 규칙 위반 감지"
        detail = "\n".join(f"  • {msg}" for msg, _ in errors)
        return False, f"{header}\n{detail}"

    return True, ""


def check_git_guard(rules, tool_input):
    command = tool_input.get("command", "")
    if not command:
        return True, ""

    # 기본 git_guard 검사
    for rule in (rules.get("git_guard") or {}).get("rules", []):
        pattern = rule["pattern"]
        if re.search(pattern, command):
            return False, rule["message"]

    # 확장 git_guard_extended 검사 (framework에서 추가)
    for rule in (rules.get("git_guard_extended") or {}).get("rules", []):
        pattern = rule["pattern"]
        if re.search(pattern, command):
            return False, rule["message"]

    return True, ""


def check_bash_file_guard(rules, tool_input):
    """Bash에서 cat/echo/heredoc으로 .kt, .sql 파일을 생성하는 패턴 차단."""
    command = tool_input.get("command", "")
    if not command:
        return True, ""

    # cat > *.kt, echo > *.kt, > *.kt << 'EOF' 등의 패턴 감지
    code_extensions = r"\.(kt|sql|java|ts|js)"
    patterns = [
        rf"cat\s*>\s*\S*{code_extensions}",
        rf"echo\s.*>\s*\S*{code_extensions}",
        rf">\s*\S*{code_extensions}\s*<<",
        rf"tee\s+\S*{code_extensions}",
    ]

    for pattern in patterns:
        if re.search(pattern, command):
            return False, (
                "BLOCKED: Bash로 코드 파일(.kt, .sql) 생성 금지 — "
                "Write/Edit 도구를 사용하세요 (하네스 훅 우회 방지)."
            )

    return True, ""


# Maps phase output directory name → (phase number, required gate filename)
PHASE_GATE_MAP = {
    "plan": (2, "_gate_phase1.md"),       # be-refactoring Phase 2
    "be": (2, "_gate_phase1.md"),          # project-analysis Phase 2
    "fix": (2, "_gate_phase1.md"),         # be-tech-debt Phase 2
}


def check_pipeline_gate(rules, tool_input):
    """파이프라인 Phase 2+ 파일 쓰기 전 이전 페이즈 게이트 통과 확인."""
    file_path = tool_input.get("file_path", "")
    if not file_path:
        return True, ""

    path = Path(file_path)
    parts = path.parts

    pip_idx = next((i for i, p in enumerate(parts) if p == "pipelines"), None)
    if pip_idx is None:
        return True, ""

    res_idx = next((i for i in range(pip_idx + 1, len(parts)) if parts[i] == "results"), None)
    if res_idx is None:
        return True, ""

    if res_idx + 2 >= len(parts):
        return True, ""

    phase_dir = parts[res_idx + 2]
    if phase_dir not in PHASE_GATE_MAP:
        return True, ""

    _phase_num, gate_filename = PHASE_GATE_MAP[phase_dir]
    session_dir = Path(*parts[: res_idx + 2])
    gate_file = session_dir / gate_filename

    if not gate_file.exists():
        prev_phase = _phase_num - 1
        return False, (
            f"BLOCKED: 파이프라인 게이트 미완료.\n"
            f"'{phase_dir}/' 진입 전에 Phase {prev_phase} 리뷰가 필요합니다.\n"
            f"Phase {prev_phase} 산출물 완료 후 세션 루트에 `_phase{prev_phase}_complete.md`를 작성하면 자동 리뷰가 실행됩니다.\n"
            f"예상 게이트 파일: {gate_file}"
        )

    gate_content = gate_file.read_text(encoding="utf-8")
    if "STATUS: FAIL" in gate_content:
        issue_lines = [l.strip() for l in gate_content.splitlines() if "[CRITICAL]" in l or "[WARNING]" in l]
        issues_summary = "\n".join(f"  {l}" for l in issue_lines[:5])
        return False, (
            f"BLOCKED: Phase {_phase_num - 1} 게이트 미통과 (FAIL).\n"
            f"{gate_file} 를 확인하고 지적사항을 해결 후 `_phase{_phase_num - 1}_complete.md`를 다시 작성하세요.\n"
            + (f"주요 이슈:\n{issues_summary}" if issues_summary else "")
        )

    return True, ""


def check_jira_guard(rules, tool_input):
    guard = rules.get("jira_guard")
    if not guard:
        return True, ""

    tool_name = os.environ.get("CLAUDE_TOOL_NAME", "")
    for rule in guard.get("rules", []):
        if tool_name == rule.get("tool", ""):
            return False, rule["message"]

    return True, ""


def main():
    if len(sys.argv) < 2:
        print("Usage: harness-check.py <check_type>", file=sys.stderr)
        sys.exit(1)

    check_type = sys.argv[1]
    rules = load_rules()
    if not rules:
        sys.exit(0)

    tool_input = get_tool_input()
    passed = True
    message = ""

    if check_type == "file-guard":
        passed, message = check_file_guard(rules, tool_input)
    elif check_type == "code-pattern":
        passed, message = check_code_patterns(rules, tool_input)
    elif check_type == "git-guard":
        passed, message = check_git_guard(rules, tool_input)
    elif check_type == "bash-file-guard":
        passed, message = check_bash_file_guard(rules, tool_input)
    elif check_type == "jira-guard":
        passed, message = check_jira_guard(rules, tool_input)
    elif check_type == "pipeline-gate":
        passed, message = check_pipeline_gate(rules, tool_input)
    else:
        print(f"Unknown check type: {check_type}", file=sys.stderr)
        sys.exit(1)

    if not passed:
        print(message, file=sys.stderr)
        sys.exit(2)

    sys.exit(0)


if __name__ == "__main__":
    main()
