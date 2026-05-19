#!/usr/bin/env python3
"""subagent-verify: Force the main session to check that a just-finished
implementation subagent actually performed:

  1. `git push` (or remote push via MCP)
  2. PR creation (`gh pr create` or github MCP)
  3. Reviewer agent invocation (pr-reviewer / code-reviewer / *reviewer*)

Wires as PostToolUse hook on the `Agent` tool. When the spawned subagent
matches an implementation role and is missing any of the three actions, the
hook injects a `<system-reminder>`-style message into the next assistant
turn so the main session is forced to handle it (manually push / open PR /
spawn reviewer).

Hook IO contract (Claude Code):
- stdin: JSON object with `tool_name`, `tool_input`, `tool_response`,
  `transcript_path`, ...
- stdout (PostToolUse): JSON with `hookSpecificOutput.additionalContext`
  is fed back to the assistant on next turn. Plain text on stdout is also
  shown. Exit 0 keeps things non-blocking; exit 2 blocks (we deliberately
  do NOT block — PostToolUse is observation-only, the work already ran).

Implementation roles (case-insensitive substring match on subagent_type):
    be-implementer, fe-implementer, kotlin-spring-impl, tdd-implement,
    *implementer, *implement

The script reads `transcript_path` (JSONL) to walk the subagent's tool
calls and detect the three required actions.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any, Iterable

# Subagent role patterns that REQUIRE push + PR + review.
# Case-insensitive substring match on subagent_type / agent role name.
IMPLEMENT_ROLE_PATTERNS = (
    "be-implementer",
    "fe-implementer",
    "kotlin-spring-impl",
    "tdd-implement",
    "implementer",
    "implement",
)

# Reviewer subagent_type patterns the parent must have observed running.
REVIEWER_ROLE_PATTERNS = (
    "pr-reviewer",
    "code-reviewer",
    "be-senior",
    "be-tech-lead",
    "fe-lead",
    "kotlin-reviewer",
    "security-reviewer",
    "reviewer",
    "review-pr",
)

# Bash command patterns that count as a remote push.
PUSH_PATTERNS = (
    re.compile(r"\bgit\s+push\b"),
    re.compile(r"\bgh\s+pr\s+create\b"),          # gh implicitly pushes
)

# Bash / MCP tool patterns that count as PR creation.
PR_CREATE_BASH_PATTERNS = (
    re.compile(r"\bgh\s+pr\s+create\b"),
    re.compile(r"\bhub\s+pull-request\b"),
)
PR_CREATE_MCP_TOOLS = (
    "mcp__plugin_everything-claude-code_github__create_pull_request",
    "mcp__github__create_pull_request",
)


def _safe_load_jsonl(path: Path) -> Iterable[dict[str, Any]]:
    """Yield each JSON object from a JSONL transcript, tolerating bad lines."""
    if not path or not path.exists():
        return
    try:
        with path.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    yield json.loads(line)
                except json.JSONDecodeError:
                    continue
    except OSError:
        return


def _match_role(role: str, patterns: tuple[str, ...]) -> bool:
    r = (role or "").lower()
    return any(p in r for p in patterns)


def _bash_command_of(tool_input: Any) -> str:
    if isinstance(tool_input, dict):
        cmd = tool_input.get("command")
        if isinstance(cmd, str):
            return cmd
    return ""


def _scan_subagent_actions(
    transcript_path: Path,
    subagent_call_id: str | None,
) -> dict[str, bool]:
    """Walk the transcript and infer what the subagent did.

    Heuristics:
    - Subagent turns are usually nested under an `Agent` tool call. We look
      for messages whose `parent_tool_use_id` (or similar) matches the
      Agent call_id when present; otherwise fall back to scanning the most
      recent N messages.
    - We accept either explicit nesting or recency-based scoping.
    """
    pushed = False
    pr_created = False
    reviewer_invoked = False

    candidates: list[dict[str, Any]] = []
    for msg in _safe_load_jsonl(transcript_path):
        # Try to scope by parent agent id if the transcript schema exposes it.
        parent = (
            msg.get("parent_tool_use_id")
            or msg.get("parentToolUseId")
            or msg.get("agent_call_id")
        )
        if subagent_call_id and parent and parent != subagent_call_id:
            continue
        candidates.append(msg)

    # If we never matched by parent id, fall back to the last 400 messages.
    if subagent_call_id and not any(
        (m.get("parent_tool_use_id") or m.get("parentToolUseId") or m.get("agent_call_id"))
        == subagent_call_id
        for m in candidates
    ):
        candidates = candidates[-400:]

    for msg in candidates:
        # Find tool-use blocks regardless of message-format variant.
        content = msg.get("message", {}).get("content") if isinstance(msg.get("message"), dict) else None
        if content is None:
            content = msg.get("content")
        if not isinstance(content, list):
            continue
        for block in content:
            if not isinstance(block, dict):
                continue
            if block.get("type") != "tool_use":
                continue
            name = block.get("name", "")
            inp = block.get("input", {})
            if name == "Bash":
                cmd = _bash_command_of(inp)
                if any(p.search(cmd) for p in PUSH_PATTERNS):
                    pushed = True
                if any(p.search(cmd) for p in PR_CREATE_BASH_PATTERNS):
                    pr_created = True
            elif name in PR_CREATE_MCP_TOOLS:
                pr_created = True
            elif name == "Agent":
                role = (inp or {}).get("subagent_type", "") if isinstance(inp, dict) else ""
                if _match_role(role, REVIEWER_ROLE_PATTERNS):
                    reviewer_invoked = True
            elif name == "Skill":
                skill = (inp or {}).get("skill", "") if isinstance(inp, dict) else ""
                if any(p in skill.lower() for p in REVIEWER_ROLE_PATTERNS):
                    reviewer_invoked = True

    return {"pushed": pushed, "pr_created": pr_created, "reviewer_invoked": reviewer_invoked}


def _emit(message: str) -> None:
    """Emit additional context for next assistant turn."""
    payload = {
        "hookSpecificOutput": {
            "hookEventName": "PostToolUse",
            "additionalContext": message,
        }
    }
    # Also print plain text in case the harness shows stdout to the model.
    sys.stdout.write(json.dumps(payload))
    sys.stdout.flush()


def main() -> int:
    try:
        raw = sys.stdin.read()
        event = json.loads(raw) if raw else {}
    except json.JSONDecodeError:
        return 0  # silent if we can't parse

    if event.get("hook_event_name") != "PostToolUse":
        return 0
    if event.get("tool_name") != "Agent":
        return 0

    tool_input = event.get("tool_input") or {}
    role = tool_input.get("subagent_type", "") or ""
    if not _match_role(role, IMPLEMENT_ROLE_PATTERNS):
        return 0  # not an implementation subagent — nothing to enforce

    transcript_path = Path(event.get("transcript_path") or "")
    subagent_call_id = (
        event.get("tool_use_id")
        or event.get("toolUseId")
        or event.get("tool_call_id")
    )

    actions = _scan_subagent_actions(transcript_path, subagent_call_id)
    missing = [k for k, v in actions.items() if not v]
    if not missing:
        return 0

    label = {
        "pushed": "원격 push (`git push` 또는 동등 작업)",
        "pr_created": "PR 생성 (`gh pr create` 또는 GitHub MCP)",
        "reviewer_invoked": "리뷰어 에이전트 호출 (pr-reviewer / code-reviewer 계열)",
    }
    bullets = "\n".join(f"- ❌ {label[k]}" for k in missing)
    role_label = role or "implementation subagent"

    msg = (
        f"[subagent-verify] 방금 끝난 서브에이전트(`{role_label}`)가 다음 필수 단계를 수행하지 않았습니다:\n"
        f"{bullets}\n\n"
        "메인 세션이 직접 다음 중 하나를 수행하세요:\n"
        "1. 누락된 단계를 메인 세션에서 즉시 실행 (push → PR → reviewer 스폰)\n"
        "2. 동일 서브에이전트에게 SendMessage로 누락 단계 보강 지시\n"
        "3. 의도적으로 생략한 경우 사용자에게 확인 후 진행\n"
        "이 검증을 건너뛰면 안 됩니다."
    )
    _emit(msg)
    return 0


if __name__ == "__main__":
    sys.exit(main())
