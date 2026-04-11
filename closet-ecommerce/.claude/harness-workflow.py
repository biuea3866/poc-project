#!/usr/bin/env python3
"""
Closet 프로젝트 워크플로우 상태 머신.

5단계 강제 플로우:
  idle → ticket → testing → implementing → reviewing → approved → idle

사용법:
  python3 harness-workflow.py <command> [args...]

Commands:
  status                     — 현재 워크플로우 상태 출력
  set-ticket <id> [title]    — 노션 티켓 등록 (idle → ticket)
  check-write                — PreToolUse: Write/Edit 허용 여부 체크
  track-write                — PostToolUse: Write/Edit 후 상태 추적
  check-bash                 — PreToolUse: Bash 명령 체크 (PR 차단 등)
  review-request             — 리뷰 요청 (implementing → reviewing)
  review-approve             — 리뷰 승인 (reviewing → approved)
  review-reject <reason>     — 리뷰 반려 (reviewing → implementing)
  inject-reminder            — UserPromptSubmit: 현재 상태 리마인더
  reset                      — 초기화 (idle로 복귀)
"""

import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
STATE_FILE = SCRIPT_DIR / "workflow-state.json"

PHASES = ["idle", "ticket", "testing", "implementing", "reviewing", "approved"]

# 테스트 파일 판별
TEST_PATH_PATTERNS = ["/test/", "/tests/", "/__tests__/"]
TEST_NAME_PATTERNS = [
    r".*Test\.kt$", r".*Spec\.kt$", r".*Tests\.kt$",
    r".*\.test\.(ts|tsx|js|jsx)$", r".*\.spec\.(ts|tsx|js|jsx)$",
]

# 소스 코드 확장자
SOURCE_EXTENSIONS = {".kt", ".java", ".ts", ".tsx", ".js", ".jsx"}

# 항상 허용되는 파일 패턴 (인프라/설정/문서)
ALWAYS_ALLOWED_EXTENSIONS = {
    ".md", ".yml", ".yaml", ".json", ".xml", ".properties",
    ".kts", ".sql", ".html", ".css", ".scss", ".sh",
    ".toml", ".cfg", ".conf", ".env.example",
}
ALWAYS_ALLOWED_PATHS = [
    ".claude/", ".github/", "docker/", "docs/", ".analysis/",
    "build.gradle", "settings.gradle", "gradlew",
    "package.json", "tsconfig", "next.config",
    "tailwind.config", "postcss.config",
]


def load_state():
    if STATE_FILE.exists():
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return default_state()


def save_state(state):
    state["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2, ensure_ascii=False)


def default_state():
    return {
        "phase": "idle",
        "ticket": {"id": None, "title": None, "url": None},
        "test_files": [],
        "impl_files": [],
        "review": {"status": None, "reviewer": None, "attempts": 0, "comments": []},
        "pr": {"url": None, "number": None},
        "started_at": None,
        "updated_at": None,
    }


def is_test_file(file_path):
    """테스트 파일인지 판별."""
    path_lower = file_path.lower()
    for pattern in TEST_PATH_PATTERNS:
        if pattern in path_lower:
            return True
    for pattern in TEST_NAME_PATTERNS:
        if re.search(pattern, file_path):
            return True
    return False


def is_source_file(file_path):
    """소스 코드 파일인지 판별 (테스트/설정 제외)."""
    ext = Path(file_path).suffix.lower()
    if ext not in SOURCE_EXTENSIONS:
        return False
    if is_test_file(file_path):
        return False
    return True


def is_always_allowed(file_path):
    """항상 허용되는 파일인지 판별 (설정/문서/인프라)."""
    ext = Path(file_path).suffix.lower()
    if ext in ALWAYS_ALLOWED_EXTENSIONS:
        return True
    for allowed_path in ALWAYS_ALLOWED_PATHS:
        if allowed_path in file_path:
            return True
    return False


def get_tool_input():
    try:
        raw = os.environ.get("CLAUDE_TOOL_INPUT", "")
        if raw:
            return json.loads(raw)
    except (json.JSONDecodeError, KeyError):
        pass
    return {}


# ─── Commands ───────────────────────────────────────────────


def cmd_status():
    state = load_state()
    phase = state["phase"]
    ticket = state["ticket"]
    tests = len(state["test_files"])
    impls = len(state["impl_files"])
    review = state["review"]

    print(f"Phase: {phase}")
    if ticket["id"]:
        print(f"Ticket: {ticket['id']} — {ticket.get('title', 'N/A')}")
    print(f"Test files: {tests}, Impl files: {impls}")
    if review["status"]:
        print(f"Review: {review['status']} (attempts: {review['attempts']})")


def cmd_set_ticket(args):
    if len(args) < 1:
        print("Usage: set-ticket <id> [title] [url]", file=sys.stderr)
        sys.exit(1)

    state = load_state()
    if state["phase"] != "idle":
        print(f"현재 phase가 '{state['phase']}'입니다. reset 후 사용하세요.", file=sys.stderr)
        sys.exit(1)

    state["phase"] = "ticket"
    state["ticket"]["id"] = args[0]
    state["ticket"]["title"] = args[1] if len(args) > 1 else None
    state["ticket"]["url"] = args[2] if len(args) > 2 else None
    state["started_at"] = datetime.now(timezone.utc).isoformat()
    save_state(state)
    print(f"Ticket registered: {args[0]}. Phase → ticket (테스트 케이스부터 작성하세요)")


def cmd_check_write():
    """PreToolUse: Write/Edit 시 워크플로우 단계 체크."""
    state = load_state()
    phase = state["phase"]
    tool_input = get_tool_input()
    file_path = tool_input.get("file_path", "")

    if not file_path:
        sys.exit(0)

    # 워크플로우 상태 파일 자체는 항상 허용
    if "workflow-state" in file_path or "harness-" in file_path:
        sys.exit(0)

    # 설정/문서 파일은 항상 허용
    if is_always_allowed(file_path):
        sys.exit(0)

    # Phase별 소스 코드 제어
    if phase == "idle":
        if is_source_file(file_path) or is_test_file(file_path):
            print(
                "BLOCKED: [워크플로우] idle 상태 — 노션 티켓을 먼저 작성하세요.\n"
                "  → `harness-workflow.py set-ticket <ticket-id> <title>` 로 등록",
                file=sys.stderr,
            )
            sys.exit(2)

    elif phase == "ticket":
        if is_source_file(file_path):
            print(
                "BLOCKED: [워크플로우] ticket 상태 — 테스트 케이스를 먼저 작성하세요.\n"
                f"  현재 ticket: {state['ticket']['id']}\n"
                "  → *Test.kt / *Spec.kt 파일을 먼저 작성해야 구현 코드를 작성할 수 있습니다.",
                file=sys.stderr,
            )
            sys.exit(2)

    elif phase == "testing":
        # 테스트 + 구현 모두 허용 (testing → implementing 자동 전환)
        pass

    elif phase == "implementing":
        # 구현 중 — 소스/테스트 모두 허용
        pass

    elif phase == "reviewing":
        # 리뷰 중에는 리뷰 반영만 허용 (수정은 가능하지만 새 파일은 주의)
        pass

    elif phase == "approved":
        # 승인됨 — PR 생성 단계
        pass

    sys.exit(0)


def cmd_track_write():
    """PostToolUse: Write/Edit 후 상태 추적."""
    state = load_state()
    phase = state["phase"]
    tool_input = get_tool_input()
    file_path = tool_input.get("file_path", "")

    if not file_path:
        return

    # 설정 파일이면 추적 불필요
    if is_always_allowed(file_path):
        return

    # 테스트 파일 추적
    if is_test_file(file_path):
        if file_path not in state["test_files"]:
            state["test_files"].append(file_path)

        # ticket → testing 자동 전환
        if phase == "ticket":
            state["phase"] = "testing"
            print(f"[워크플로우] Phase → testing (테스트 작성 시작: {Path(file_path).name})")

    # 소스 파일 추적
    elif is_source_file(file_path):
        if file_path not in state["impl_files"]:
            state["impl_files"].append(file_path)

        # testing → implementing 자동 전환
        if phase == "testing" and len(state["test_files"]) > 0:
            state["phase"] = "implementing"
            print(f"[워크플로우] Phase → implementing (테스트 {len(state['test_files'])}개 존재, 구현 시작)")

    save_state(state)


def cmd_check_bash():
    """PreToolUse: Bash 명령에서 PR 생성 차단."""
    state = load_state()
    phase = state["phase"]
    tool_input = get_tool_input()
    command = tool_input.get("command", "")

    if not command:
        sys.exit(0)

    # gh pr create 차단: approved 상태에서만 허용
    if "gh pr create" in command or "gh pr merge" in command:
        if phase not in ("approved",):
            if phase == "implementing":
                msg = (
                    "BLOCKED: [워크플로우] 리뷰를 먼저 받아야 PR을 생성할 수 있습니다.\n"
                    "  → 리뷰어 에이전트를 스폰하여 코드 리뷰를 받으세요.\n"
                    "  → 리뷰 통과 후 `harness-workflow.py review-approve` 로 승인 등록"
                )
            elif phase == "reviewing":
                msg = (
                    "BLOCKED: [워크플로우] 리뷰가 아직 진행 중입니다.\n"
                    f"  Review status: {state['review']['status']}\n"
                    "  → 리뷰 통과 후 `harness-workflow.py review-approve` 로 승인 등록"
                )
            else:
                msg = (
                    f"BLOCKED: [워크플로우] 현재 phase '{phase}'에서는 PR을 생성할 수 없습니다.\n"
                    "  → 전체 플로우: idle → ticket → testing → implementing → reviewing → approved → PR"
                )
            print(msg, file=sys.stderr)
            sys.exit(2)

    sys.exit(0)


def cmd_review_request():
    state = load_state()
    if state["phase"] not in ("implementing", "reviewing"):
        print(f"현재 phase '{state['phase']}'에서는 리뷰 요청을 할 수 없습니다.", file=sys.stderr)
        sys.exit(1)

    state["phase"] = "reviewing"
    state["review"]["status"] = "pending"
    state["review"]["attempts"] += 1
    save_state(state)
    print(f"[워크플로우] Phase → reviewing (리뷰 요청 #{state['review']['attempts']})")


def cmd_review_approve():
    state = load_state()
    if state["phase"] != "reviewing":
        print(f"현재 phase '{state['phase']}'에서는 리뷰 승인을 할 수 없습니다.", file=sys.stderr)
        sys.exit(1)

    state["phase"] = "approved"
    state["review"]["status"] = "approved"
    save_state(state)
    print("[워크플로우] Phase → approved (리뷰 통과! PR을 생성하세요)")


def cmd_review_reject(args):
    state = load_state()
    if state["phase"] != "reviewing":
        print(f"현재 phase '{state['phase']}'에서는 리뷰 반려를 할 수 없습니다.", file=sys.stderr)
        sys.exit(1)

    reason = " ".join(args) if args else "No reason provided"
    state["phase"] = "implementing"
    state["review"]["status"] = "rejected"
    state["review"]["comments"].append(reason)
    save_state(state)
    print(f"[워크플로우] Phase → implementing (리뷰 반려: {reason})")


def cmd_reset():
    state = default_state()
    save_state(state)
    print("[워크플로우] Phase → idle (초기화 완료)")


def _load_orchestrator_context():
    """오케스트레이터 큐 상태를 읽는다."""
    queue_file = SCRIPT_DIR / "orchestrator-queue.json"
    if not queue_file.exists():
        return None
    try:
        with open(queue_file, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, KeyError):
        return None


def cmd_inject_reminder():
    """UserPromptSubmit: 현재 워크플로우 상태를 에이전트에게 주입."""
    state = load_state()
    phase = state["phase"]
    ticket = state["ticket"]
    test_count = len(state["test_files"])
    impl_count = len(state["impl_files"])

    lines = [
        "[하네스 워크플로우 상태]",
        f"현재 Phase: {phase}",
    ]

    if ticket["id"]:
        lines.append(f"Ticket: {ticket['id']} — {ticket.get('title', 'N/A')}")

    lines.append(f"테스트 파일: {test_count}개, 구현 파일: {impl_count}개")

    # 오케스트레이터 큐 상태 주입
    orch = _load_orchestrator_context()
    if orch and orch.get("tickets"):
        total = len(orch["tickets"])
        done = sum(1 for t in orch["tickets"] if t["status"] == "done")
        pending = sum(1 for t in orch["tickets"] if t["status"] == "pending")
        lines.append(f"로드맵: {done}/{total} 완료, {pending}개 대기")

    if phase == "idle":
        lines.append("")
        # 오케스트레이터에 pending 티켓이 있으면 자동 시작 안내
        if orch and any(t["status"] == "pending" for t in orch.get("tickets", [])):
            lines.append("로드맵 큐에 대기 중인 티켓이 있습니다:")
            lines.append("  → `python3 .claude/harness-orchestrator.py next` 로 다음 티켓 시작")
            lines.append("  → 또는 사용자가 '로드맵 수행해'라고 하면 전체 자동 실행")
        else:
            lines.append("idle 상태입니다. 작업을 시작하려면:")
            lines.append("  1. 노션에 티켓을 먼저 작성하세요 (자동으로 워크플로우에 등록됩니다)")
            lines.append("  2. 또는 `python3 .claude/harness-orchestrator.py add <id> <title>` 로 큐에 추가")
            lines.append("  ※ 티켓 없이 소스 코드 작성이 차단됩니다")

    elif phase == "ticket":
        lines.append("")
        lines.append("ticket 상태입니다. 테스트 케이스를 먼저 작성하세요:")
        lines.append("  - 노션 티켓의 AC/테스트 케이스를 코드로 작성")
        lines.append("  - Kotest BehaviorSpec (Given/When/Then)")
        lines.append("  - *Test.kt 또는 *Spec.kt 파일로 작성")
        lines.append("  ※ 테스트 없이 구현 코드 작성이 차단됩니다")

    elif phase == "testing":
        lines.append("")
        lines.append("테스트 작성 중입니다. 구현 코드도 작성할 수 있습니다.")
        lines.append("  → Red-Green-Refactor 사이클을 따르세요")

    elif phase == "implementing":
        lines.append("")
        lines.append("구현 중입니다. 완료 후 리뷰를 요청하세요:")
        lines.append("  → 리뷰어 에이전트를 스폰하여 코드 리뷰를 받으세요")
        lines.append("  → `python3 .claude/harness-workflow.py review-request` 로 리뷰 시작")
        lines.append("  ※ 리뷰 없이 PR 생성이 차단됩니다")

    elif phase == "reviewing":
        review = state["review"]
        lines.append(f"리뷰 상태: {review['status']} (시도: {review['attempts']}회)")
        lines.append("  → 리뷰 반영 후 승인 대기 중")
        lines.append("  → 승인: `python3 .claude/harness-workflow.py review-approve`")
        lines.append("  → 반려: `python3 .claude/harness-workflow.py review-reject <이유>`")

    elif phase == "approved":
        lines.append("")
        lines.append("리뷰 승인 완료! PR을 생성하고 머지하세요.")
        lines.append("  → PR 머지 후: `python3 .claude/harness-orchestrator.py done` 으로 다음 티켓 자동 시작")

    print("\n".join(lines))


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    cmd = sys.argv[1]
    args = sys.argv[2:]

    commands = {
        "status": lambda: cmd_status(),
        "set-ticket": lambda: cmd_set_ticket(args),
        "check-write": lambda: cmd_check_write(),
        "track-write": lambda: cmd_track_write(),
        "check-bash": lambda: cmd_check_bash(),
        "review-request": lambda: cmd_review_request(),
        "review-approve": lambda: cmd_review_approve(),
        "review-reject": lambda: cmd_review_reject(args),
        "reset": lambda: cmd_reset(),
        "inject-reminder": lambda: cmd_inject_reminder(),
    }

    if cmd not in commands:
        print(f"Unknown command: {cmd}", file=sys.stderr)
        print("Available:", ", ".join(commands.keys()), file=sys.stderr)
        sys.exit(1)

    commands[cmd]()


if __name__ == "__main__":
    main()
