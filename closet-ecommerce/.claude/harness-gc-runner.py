#!/usr/bin/env python3
"""
Closet GC(Garbage Collection) 파이프라인 러너.

코드베이스 전체를 스캔하여 규칙 위반을 찾고,
워커 에이전트가 수정 → 리뷰어 에이전트가 검증 → 루프하는 자동 리팩토링 시스템.

사용법:
  python3 harness-gc-runner.py plan              — 스캔 + 수정 계획 생성
  python3 harness-gc-runner.py plan --module X    — 특정 모듈만
  python3 harness-gc-runner.py status             — 현재 GC 상태
  python3 harness-gc-runner.py complete <task_id> — 태스크 완료 처리
  python3 harness-gc-runner.py review <task_id>   — 리뷰 결과 기록
  python3 harness-gc-runner.py report             — 최종 리포트
  python3 harness-gc-runner.py reset              — 초기화

GC 플로우:
  plan → (worker fixes) → (reviewer validates) → complete/reject → report
"""

import json
import re
import subprocess
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
GC_STATE_FILE = SCRIPT_DIR / "gc-state.json"
GC_REPORT_FILE = SCRIPT_DIR / "gc-report.json"
HARNESS_GC = SCRIPT_DIR / "harness-gc.py"


def load_gc_state():
    if GC_STATE_FILE.exists():
        with open(GC_STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return default_gc_state()


def save_gc_state(state):
    state["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(GC_STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2, ensure_ascii=False)


def default_gc_state():
    return {
        "phase": "idle",
        "scan_result": None,
        "tasks": [],
        "completed_tasks": [],
        "rejected_tasks": [],
        "cycle": 0,
        "started_at": None,
        "updated_at": None,
    }


def run_scan(module_filter=None):
    """harness-gc.py scan --json 실행."""
    result = subprocess.run(
        ["python3", str(HARNESS_GC), "scan", "--json"],
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True,
    )
    try:
        report = json.loads(result.stdout)
    except json.JSONDecodeError:
        print("GC 스캔 실패:", result.stderr, file=sys.stderr)
        sys.exit(1)

    if module_filter:
        report["violations"] = [
            v for v in report["violations"]
            if module_filter in v["file"]
        ]
        report["total_violations"] = len(report["violations"])

    return report


def group_violations(violations):
    """위반을 모듈 + 규칙별로 그룹화하여 태스크 생성."""
    # 모듈별 그룹
    by_module = defaultdict(lambda: defaultdict(list))
    for v in violations:
        # closet-ecommerce/closet-order/src/... → closet-order
        parts = v["file"].split("/")
        module = "unknown"
        for p in parts:
            if p.startswith("closet-") and p != "closet-ecommerce":
                module = p
                break
        by_module[module][v["rule_id"]].append(v)

    tasks = []
    task_id = 1

    for module, rules in sorted(by_module.items()):
        for rule_id, rule_violations in sorted(rules.items()):
            files = sorted(set(v["file"] for v in rule_violations))
            auto_fixable = rule_violations[0].get("severity") == "error"

            tasks.append({
                "id": f"GC-{task_id:03d}",
                "module": module,
                "rule_id": rule_id,
                "message": rule_violations[0]["message"],
                "violation_count": len(rule_violations),
                "files": files,
                "status": "pending",  # pending → in_progress → review → done/rejected
                "worker": None,
                "reviewer": None,
                "review_comments": [],
                "attempts": 0,
            })
            task_id += 1

    return tasks


def cmd_plan(args):
    """스캔 + 수정 계획 생성."""
    module_filter = None
    if "--module" in args:
        idx = args.index("--module")
        if idx + 1 < len(args):
            module_filter = args[idx + 1]

    print("GC 스캔 실행 중...")
    report = run_scan(module_filter)
    violations = report.get("violations", [])

    if not violations:
        print("위반 없음. GC 불필요.")
        return

    tasks = group_violations(violations)

    state = load_gc_state()
    state["phase"] = "planned"
    state["scan_result"] = {
        "scanned_at": report["scanned_at"],
        "total_violations": report["total_violations"],
        "by_rule": report.get("by_rule", {}),
    }
    state["tasks"] = tasks
    state["completed_tasks"] = []
    state["rejected_tasks"] = []
    state["cycle"] += 1
    state["started_at"] = datetime.now(timezone.utc).isoformat()
    save_gc_state(state)

    # 태스크 요약 출력
    print(f"\n{'='*70}")
    print(f"GC Cycle #{state['cycle']} — {len(violations)}개 위반, {len(tasks)}개 태스크")
    print(f"{'='*70}")

    # 모듈별 요약
    by_module = defaultdict(list)
    for t in tasks:
        by_module[t["module"]].append(t)

    for module, module_tasks in sorted(by_module.items()):
        total = sum(t["violation_count"] for t in module_tasks)
        print(f"\n📦 {module} ({total}건)")
        for t in module_tasks:
            print(f"  {t['id']} [{t['rule_id']}] {t['violation_count']}건 — {len(t['files'])}개 파일")

    # 워커/리뷰어 배정 가이드
    print(f"\n{'─'*70}")
    print("워커 에이전트 배정 가이드:")
    print("")

    auto_fix_rules = {"no-local-datetime", "no-db-boolean-type", "no-db-datetime-no-precision"}
    auto_tasks = [t for t in tasks if t["rule_id"] in auto_fix_rules]
    manual_tasks = [t for t in tasks if t["rule_id"] not in auto_fix_rules]

    if auto_tasks:
        print(f"  🤖 자동 수정 가능 ({len(auto_tasks)}개 태스크):")
        for t in auto_tasks:
            print(f"     {t['id']} [{t['rule_id']}] {t['violation_count']}건")
        print(f"     → `python3 .claude/harness-gc.py fix` 로 일괄 수정")

    if manual_tasks:
        print(f"\n  👷 수동 리팩토링 필요 ({len(manual_tasks)}개 태스크):")
        for t in manual_tasks:
            print(f"     {t['id']} [{t['rule_id']}] {t['violation_count']}건 in {t['module']}")
        print(f"     → 워커 에이전트를 모듈별로 스폰하여 수정")

    print(f"\n{'─'*70}")
    print("다음 단계:")
    print("  1. 자동 수정: `python3 .claude/harness-gc.py fix`")
    print("  2. 워커 스폰: 모듈별 에이전트에게 수동 태스크 할당")
    print("  3. 완료 기록: `python3 .claude/harness-gc-runner.py complete <task_id>`")
    print("  4. 리뷰 요청: `python3 .claude/harness-gc-runner.py review <task_id>`")


def cmd_status(args):
    """현재 GC 상태."""
    state = load_gc_state()

    if state["phase"] == "idle":
        print("GC idle 상태. `plan` 으로 시작하세요.")
        return

    tasks = state["tasks"]
    completed = state["completed_tasks"]
    rejected = state["rejected_tasks"]

    pending = [t for t in tasks if t["status"] == "pending"]
    in_progress = [t for t in tasks if t["status"] == "in_progress"]
    in_review = [t for t in tasks if t["status"] == "review"]
    done = [t for t in tasks if t["status"] == "done"]

    total = len(tasks)
    print(f"GC Cycle #{state['cycle']} — {state['phase']}")
    print(f"  총 태스크: {total}")
    print(f"  ⏳ pending:     {len(pending)}")
    print(f"  🔧 in_progress: {len(in_progress)}")
    print(f"  🔍 review:      {len(in_review)}")
    print(f"  ✅ done:         {len(done)}")
    print(f"  ❌ rejected:     {len(rejected)}")

    if in_progress:
        print(f"\n현재 작업 중:")
        for t in in_progress:
            print(f"  {t['id']} [{t['rule_id']}] in {t['module']} — worker: {t.get('worker', 'N/A')}")

    if in_review:
        print(f"\n리뷰 대기:")
        for t in in_review:
            print(f"  {t['id']} [{t['rule_id']}] in {t['module']}")

    # 워커 프롬프트 생성
    if "--prompt" in args and pending:
        task = pending[0]
        print(f"\n{'─'*70}")
        print(f"다음 태스크 워커 프롬프트 ({task['id']}):")
        print(f"{'─'*70}")
        print(generate_worker_prompt(task))


def cmd_worker_prompt(args):
    """특정 태스크의 워커 프롬프트 생성."""
    if not args:
        print("Usage: worker-prompt <task_id>", file=sys.stderr)
        sys.exit(1)

    task_id = args[0]
    state = load_gc_state()

    task = None
    for t in state["tasks"]:
        if t["id"] == task_id:
            task = t
            break

    if not task:
        print(f"Task {task_id} not found", file=sys.stderr)
        sys.exit(1)

    task["status"] = "in_progress"
    task["attempts"] += 1
    save_gc_state(state)

    print(generate_worker_prompt(task))


def cmd_reviewer_prompt(args):
    """특정 태스크의 리뷰어 프롬프트 생성."""
    if not args:
        print("Usage: reviewer-prompt <task_id>", file=sys.stderr)
        sys.exit(1)

    task_id = args[0]
    state = load_gc_state()

    task = None
    for t in state["tasks"]:
        if t["id"] == task_id:
            task = t
            break

    if not task:
        print(f"Task {task_id} not found", file=sys.stderr)
        sys.exit(1)

    task["status"] = "review"
    save_gc_state(state)

    print(generate_reviewer_prompt(task))


def generate_worker_prompt(task):
    """워커 에이전트에게 전달할 프롬프트 생성."""
    files_list = "\n".join(f"  - {f}" for f in task["files"][:20])
    if len(task["files"]) > 20:
        files_list += f"\n  ... 외 {len(task['files']) - 20}개"

    reject_context = ""
    if task["review_comments"]:
        reject_context = "\n\n## 이전 리뷰 반려 사유\n"
        for i, c in enumerate(task["review_comments"], 1):
            reject_context += f"{i}. {c}\n"

    return f"""## GC 태스크: {task['id']}

**규칙**: {task['rule_id']}
**모듈**: {task['module']}
**위반 수**: {task['violation_count']}건
**메시지**: {task['message']}

## 수정 대상 파일
{files_list}
{reject_context}
## 수정 지침

1. 위 파일들에서 `{task['rule_id']}` 규칙 위반을 모두 수정하세요.
2. 수정 후 `python3 .claude/harness-gc.py watch` 로 위반이 제거되었는지 확인하세요.
3. 컴파일 확인: `JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home ./gradlew compileKotlin`
4. 수정 완료 후 보고하세요.

## 주의사항
- 해당 규칙 위반만 수정. 다른 코드 변경 금지.
- import 정리 필요 시 함께 수정.
- 기존 테스트가 깨지지 않도록 주의."""


def generate_reviewer_prompt(task):
    """리뷰어 에이전트에게 전달할 프롬프트 생성."""
    files_list = "\n".join(f"  - {f}" for f in task["files"][:20])

    return f"""## GC 리뷰: {task['id']}

**규칙**: {task['rule_id']}
**모듈**: {task['module']}
**메시지**: {task['message']}

## 검증 대상 파일
{files_list}

## 리뷰 체크리스트

1. **규칙 위반 제거 확인**: `python3 .claude/harness-gc.py watch` 실행하여 해당 규칙 위반이 0건인지 확인
2. **컴파일 확인**: `JAVA_HOME=/Users/biuea/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home ./gradlew compileKotlin` 성공 여부
3. **기존 동작 보존**: 수정이 비즈니스 로직을 변경하지 않았는지 확인
4. **import 정리**: 불필요한 import가 남아있지 않은지 확인
5. **코드 품질**: 수정된 코드가 프로젝트 컨벤션을 따르는지 확인

## 판정
- **승인**: 모든 체크리스트 통과 → `python3 .claude/harness-gc-runner.py complete {task['id']}`
- **반려**: 이슈 발견 → `python3 .claude/harness-gc-runner.py reject {task['id']} "반려 사유"`"""


def cmd_complete(args):
    """태스크 완료 처리."""
    if not args:
        print("Usage: complete <task_id>", file=sys.stderr)
        sys.exit(1)

    task_id = args[0]
    state = load_gc_state()

    for t in state["tasks"]:
        if t["id"] == task_id:
            t["status"] = "done"
            state["completed_tasks"].append(task_id)
            save_gc_state(state)
            print(f"[GC] {task_id} 완료 처리됨")

            # 전체 완료 체크
            pending = [x for x in state["tasks"] if x["status"] not in ("done",)]
            if not pending:
                state["phase"] = "complete"
                save_gc_state(state)
                print(f"[GC] 모든 태스크 완료! Cycle #{state['cycle']} 종료")
            return

    print(f"Task {task_id} not found", file=sys.stderr)
    sys.exit(1)


def cmd_reject(args):
    """태스크 반려 → implementing으로 되돌림."""
    if len(args) < 2:
        print("Usage: reject <task_id> <reason>", file=sys.stderr)
        sys.exit(1)

    task_id = args[0]
    reason = " ".join(args[1:])
    state = load_gc_state()

    for t in state["tasks"]:
        if t["id"] == task_id:
            t["status"] = "pending"  # 다시 pending으로
            t["review_comments"].append(reason)
            state["rejected_tasks"].append({"id": task_id, "reason": reason})
            save_gc_state(state)
            print(f"[GC] {task_id} 반려됨: {reason}")
            print(f"  → 워커에게 재할당 필요 (시도 횟수: {t['attempts']})")
            return

    print(f"Task {task_id} not found", file=sys.stderr)
    sys.exit(1)


def cmd_report(args):
    """최종 GC 리포트."""
    state = load_gc_state()

    if state["phase"] == "idle":
        print("GC가 실행되지 않았습니다.")
        return

    tasks = state["tasks"]
    done = [t for t in tasks if t["status"] == "done"]
    pending = [t for t in tasks if t["status"] != "done"]

    total_violations = state.get("scan_result", {}).get("total_violations", 0)
    fixed_violations = sum(t["violation_count"] for t in done)
    remaining = sum(t["violation_count"] for t in pending)

    print(f"# GC 리포트 — Cycle #{state['cycle']}")
    print(f"")
    print(f"| 항목 | 수치 |")
    print(f"|------|------|")
    print(f"| 스캔 위반 | {total_violations}건 |")
    print(f"| 수정 완료 | {fixed_violations}건 |")
    print(f"| 미처리 | {remaining}건 |")
    print(f"| 태스크 | {len(done)}/{len(tasks)} 완료 |")
    print(f"| 반려 횟수 | {len(state.get('rejected_tasks', []))}회 |")

    if done:
        print(f"\n## 완료된 태스크")
        for t in done:
            print(f"  ✅ {t['id']} [{t['rule_id']}] {t['module']} — {t['violation_count']}건 수정")

    if pending:
        print(f"\n## 미처리 태스크")
        for t in pending:
            print(f"  ⏳ {t['id']} [{t['rule_id']}] {t['module']} — {t['violation_count']}건 ({t['status']})")

    by_rule = defaultdict(int)
    for t in tasks:
        by_rule[t["rule_id"]] += t["violation_count"]

    print(f"\n## 규칙별 위반 분포")
    for rule_id, count in sorted(by_rule.items(), key=lambda x: -x[1]):
        status = "✅" if all(t["status"] == "done" for t in tasks if t["rule_id"] == rule_id) else "⏳"
        print(f"  {status} {rule_id}: {count}건")


def cmd_reset(args):
    state = default_gc_state()
    save_gc_state(state)
    print("[GC] 상태 초기화 완료")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    cmd = sys.argv[1]
    args = sys.argv[2:]

    commands = {
        "plan": cmd_plan,
        "status": cmd_status,
        "worker-prompt": cmd_worker_prompt,
        "reviewer-prompt": cmd_reviewer_prompt,
        "complete": cmd_complete,
        "reject": cmd_reject,
        "report": cmd_report,
        "reset": cmd_reset,
    }

    if cmd not in commands:
        print(f"Unknown command: {cmd}", file=sys.stderr)
        print("Available:", ", ".join(commands.keys()))
        sys.exit(1)

    commands[cmd](args)


if __name__ == "__main__":
    main()
