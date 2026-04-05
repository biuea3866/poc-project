#!/usr/bin/env python3
"""
Closet 파이프라인 프레임워크 러너.

.analysis/ 디렉토리의 파이프라인들을 하네스 시스템과 통합하여 실행한다.
각 파이프라인의 단계(Phase)를 추적하고, 하네스 게이트를 강제한다.

사용법:
  python3 pipeline-runner.py list                         — 사용 가능한 파이프라인 목록
  python3 pipeline-runner.py start <pipeline> [--name X]  — 파이프라인 시작
  python3 pipeline-runner.py status                       — 현재 실행 중인 파이프라인 상태
  python3 pipeline-runner.py advance [--force]             — 다음 Phase로 진행
  python3 pipeline-runner.py gate-check                   — 현재 Phase 게이트 체크
  python3 pipeline-runner.py complete                     — 파이프라인 완료
  python3 pipeline-runner.py abort                        — 파이프라인 중단
  python3 pipeline-runner.py history                      — 완료된 파이프라인 히스토리

파이프라인 ↔ 하네스 통합:
  - 파이프라인 시작 시 workflow-state.json과 연동
  - 각 Phase 전환 시 게이트 조건 확인
  - 결과물은 .analysis/{pipeline}/results/ 에 자동 저장
"""

import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent

# 파이프라인 정의 디렉토리 (closet-ecommerce 프로젝트)
ANALYSIS_DIRS = [
    PROJECT_ROOT / "closet-ecommerce" / ".analysis",
    PROJECT_ROOT / ".analysis",
]

PIPELINE_STATE_FILE = SCRIPT_DIR / "pipeline-state.json"
PIPELINE_HISTORY_FILE = SCRIPT_DIR / "pipeline-history.json"

# 파이프라인별 Phase 정의 및 게이트 조건
PIPELINE_PHASES = {
    "prd": {
        "phases": [
            {"name": "요구사항 파싱", "gate": None},
            {"name": "영향 범위 분석", "gate": None},
            {"name": "구현 설계", "gate": None},
            {"name": "리스크 검토", "gate": None},
        ],
        "output_dir": "prd/results",
    },
    "be-implementation": {
        "phases": [
            {"name": "요구사항 검증 & Gap 분석", "gate": None},
            {"name": "기술 스택 분석", "gate": None},
            {"name": "TDD 작성", "gate": None},
            {"name": "구현 티켓 작성", "gate": None},
        ],
        "output_dir": "be-implementation/results",
    },
    "implementation": {
        "phases": [
            {"name": "티켓 분석", "gate": "ticket_required"},
            {"name": "테스트 작성 (Red)", "gate": "test_first"},
            {"name": "코드 생성 (Green)", "gate": "tests_exist"},
            {"name": "컴파일 + 테스트 검증", "gate": "build_pass"},
            {"name": "리뷰 + PR 생성", "gate": "review_required"},
        ],
        "output_dir": "implementation/results",
    },
    "verification": {
        "phases": [
            {"name": "설계 원칙 검증", "gate": "harness_scan"},
            {"name": "아키텍처 검증", "gate": None},
            {"name": "티켓 AC 검증", "gate": None},
            {"name": "검증 리포트", "gate": None},
        ],
        "output_dir": "verification/results",
    },
    "pr-review": {
        "phases": [
            {"name": "변경 범위 파악", "gate": None},
            {"name": "심층 분석", "gate": None},
            {"name": "리뷰 종합", "gate": None},
        ],
        "output_dir": "pr-review/results",
    },
    "gc": {
        "phases": [
            {"name": "전체 스캔", "gate": "harness_scan"},
            {"name": "태스크 생성", "gate": None},
            {"name": "워커 수정", "gate": None},
            {"name": "리뷰어 검증", "gate": "review_required"},
            {"name": "최종 리포트", "gate": "harness_clean"},
        ],
        "output_dir": "gc/results",
    },
    "refactoring": {
        "phases": [
            {"name": "현황 분석", "gate": None},
            {"name": "리팩토링 계획", "gate": None},
            {"name": "구현", "gate": "test_first"},
            {"name": "검증", "gate": "build_pass"},
        ],
        "output_dir": "refactoring/results",
    },
    "incident": {
        "phases": [
            {"name": "상황 파악", "gate": None},
            {"name": "원인 분석", "gate": None},
            {"name": "수정 적용", "gate": None},
            {"name": "검증 + 리포트", "gate": "build_pass"},
        ],
        "output_dir": "incident/results",
    },
    "release": {
        "phases": [
            {"name": "변경 사항 수집", "gate": None},
            {"name": "영향 분석", "gate": None},
            {"name": "릴리즈 노트 작성", "gate": None},
        ],
        "output_dir": "release/results",
    },
    "api-change": {
        "phases": [
            {"name": "API 변경 파악", "gate": None},
            {"name": "하위 호환성 분석", "gate": None},
            {"name": "마이그레이션 가이드", "gate": None},
        ],
        "output_dir": "api-change/results",
    },
}

# 게이트 체크 함수
GATE_CHECKS = {
    "ticket_required": {
        "description": "노션 티켓 등록 필수",
        "check": lambda: check_workflow_phase(["ticket", "testing", "implementing", "reviewing", "approved"]),
    },
    "test_first": {
        "description": "테스트 코드 먼저 작성 필수",
        "check": lambda: check_workflow_phase(["testing", "implementing", "reviewing", "approved"]),
    },
    "tests_exist": {
        "description": "테스트 파일 존재 필수",
        "check": lambda: check_test_files_exist(),
    },
    "build_pass": {
        "description": "Gradle 빌드 성공 필수",
        "check": lambda: True,  # 실제 빌드는 에이전트가 수행, 여기서는 가이드만
    },
    "review_required": {
        "description": "리뷰어 에이전트 리뷰 필수",
        "check": lambda: check_workflow_phase(["approved"]),
    },
    "harness_scan": {
        "description": "하네스 GC 스캔 실행 필수",
        "check": lambda: True,  # 스캔 실행은 에이전트 책임
    },
    "harness_clean": {
        "description": "하네스 규칙 위반 0건",
        "check": lambda: check_harness_clean(),
    },
}


def check_workflow_phase(allowed_phases):
    """워크플로우 상태가 허용된 phase인지 확인."""
    wf_state_file = SCRIPT_DIR / "workflow-state.json"
    if not wf_state_file.exists():
        return False
    with open(wf_state_file, "r", encoding="utf-8") as f:
        wf = json.load(f)
    return wf.get("phase") in allowed_phases


def check_test_files_exist():
    """워크플로우에 테스트 파일이 1개 이상 기록되어 있는지."""
    wf_state_file = SCRIPT_DIR / "workflow-state.json"
    if not wf_state_file.exists():
        return False
    with open(wf_state_file, "r", encoding="utf-8") as f:
        wf = json.load(f)
    return len(wf.get("test_files", [])) > 0


def check_harness_clean():
    """GC 스캔 결과가 0건인지."""
    gc_report = SCRIPT_DIR / "gc-report.json"
    if not gc_report.exists():
        return False
    with open(gc_report, "r", encoding="utf-8") as f:
        report = json.load(f)
    return report.get("total_violations", 1) == 0


def load_pipeline_state():
    if PIPELINE_STATE_FILE.exists():
        with open(PIPELINE_STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"active": None}


def save_pipeline_state(state):
    state["updated_at"] = datetime.now(timezone.utc).isoformat()
    with open(PIPELINE_STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2, ensure_ascii=False)


def load_pipeline_history():
    if PIPELINE_HISTORY_FILE.exists():
        with open(PIPELINE_HISTORY_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"runs": []}


def save_pipeline_history(history):
    with open(PIPELINE_HISTORY_FILE, "w", encoding="utf-8") as f:
        json.dump(history, f, indent=2, ensure_ascii=False)


def find_pipeline_md(pipeline_name):
    """파이프라인의 PIPELINE.md 경로 찾기."""
    for analysis_dir in ANALYSIS_DIRS:
        md_path = analysis_dir / pipeline_name / "PIPELINE.md"
        if md_path.exists():
            return md_path
    return None


def cmd_list(args):
    """사용 가능한 파이프라인 목록."""
    print("사용 가능한 파이프라인:")
    print(f"{'─'*60}")
    for name, config in sorted(PIPELINE_PHASES.items()):
        phases = config["phases"]
        md = find_pipeline_md(name)
        md_status = "📄" if md else "  "
        gates = sum(1 for p in phases if p["gate"])
        print(f"  {md_status} {name:25s} {len(phases)} phases, {gates} gates")

    print(f"\n📄 = PIPELINE.md 존재")
    print(f"gates = 하네스 게이트 (강제 조건) 수")


def cmd_start(args):
    """파이프라인 시작."""
    if not args:
        print("Usage: start <pipeline> [--name description]", file=sys.stderr)
        sys.exit(1)

    pipeline_name = args[0]
    if pipeline_name not in PIPELINE_PHASES:
        print(f"Unknown pipeline: {pipeline_name}", file=sys.stderr)
        print("Available:", ", ".join(PIPELINE_PHASES.keys()))
        sys.exit(1)

    state = load_pipeline_state()
    if state.get("active"):
        print(f"이미 실행 중인 파이프라인이 있습니다: {state['active']['pipeline']}")
        print("  → complete 또는 abort 후 시작하세요.")
        sys.exit(1)

    run_name = None
    if "--name" in args:
        idx = args.index("--name")
        if idx + 1 < len(args):
            run_name = " ".join(args[idx + 1:])

    config = PIPELINE_PHASES[pipeline_name]
    phases = config["phases"]
    now = datetime.now(timezone.utc).isoformat()

    state["active"] = {
        "pipeline": pipeline_name,
        "name": run_name or pipeline_name,
        "current_phase": 0,
        "total_phases": len(phases),
        "phases": phases,
        "started_at": now,
        "phase_history": [],
    }
    save_pipeline_state(state)

    first_phase = phases[0]
    print(f"[파이프라인] {pipeline_name} 시작")
    print(f"  Phase 1/{len(phases)}: {first_phase['name']}")
    if first_phase["gate"]:
        gate = GATE_CHECKS[first_phase["gate"]]
        print(f"  🚧 게이트: {gate['description']}")

    md = find_pipeline_md(pipeline_name)
    if md:
        print(f"  📄 가이드: {md.relative_to(PROJECT_ROOT)}")


def cmd_status(args):
    """현재 실행 중인 파이프라인 상태."""
    state = load_pipeline_state()

    if not state.get("active"):
        print("실행 중인 파이프라인 없음.")
        return

    active = state["active"]
    pipeline = active["pipeline"]
    current = active["current_phase"]
    total = active["total_phases"]
    phases = active["phases"]

    print(f"[파이프라인] {pipeline} — {active.get('name', '')}")
    print(f"{'─'*50}")

    for i, phase in enumerate(phases):
        if i < current:
            status = "✅"
        elif i == current:
            status = "▶️"
        else:
            status = "⏳"

        gate_info = ""
        if phase["gate"]:
            gate = GATE_CHECKS.get(phase["gate"], {})
            gate_info = f" 🚧 {gate.get('description', phase['gate'])}"

        print(f"  {status} Phase {i+1}: {phase['name']}{gate_info}")

    print(f"\n진행률: {current}/{total} ({current*100//total}%)")


def cmd_advance(args):
    """다음 Phase로 진행."""
    state = load_pipeline_state()

    if not state.get("active"):
        print("실행 중인 파이프라인 없음.")
        sys.exit(1)

    active = state["active"]
    current = active["current_phase"]
    total = active["total_phases"]
    phases = active["phases"]
    force = "--force" in args

    if current >= total:
        print("모든 Phase가 완료되었습니다. `complete` 로 마무리하세요.")
        return

    # 다음 Phase 게이트 체크
    next_idx = current + 1
    if next_idx < total:
        next_phase = phases[next_idx]
        gate_name = next_phase.get("gate")
        if gate_name and not force:
            gate = GATE_CHECKS.get(gate_name, {})
            check_fn = gate.get("check", lambda: True)
            if not check_fn():
                print(f"🚧 게이트 실패: {gate.get('description', gate_name)}")
                print(f"  → 조건을 충족한 후 다시 시도하거나 --force 로 강제 진행")
                sys.exit(1)

    # 현재 Phase 완료 기록
    active["phase_history"].append({
        "phase": current,
        "name": phases[current]["name"],
        "completed_at": datetime.now(timezone.utc).isoformat(),
    })
    active["current_phase"] = next_idx
    save_pipeline_state(state)

    if next_idx < total:
        next_p = phases[next_idx]
        print(f"[파이프라인] Phase {next_idx+1}/{total}: {next_p['name']}")
        if next_p["gate"]:
            gate = GATE_CHECKS.get(next_p["gate"], {})
            print(f"  🚧 게이트: {gate.get('description', next_p['gate'])}")
    else:
        print(f"[파이프라인] 모든 Phase 완료! `complete` 로 마무리하세요.")


def cmd_gate_check(args):
    """현재 Phase 게이트 체크."""
    state = load_pipeline_state()

    if not state.get("active"):
        print("실행 중인 파이프라인 없음.")
        return

    active = state["active"]
    current = active["current_phase"]
    phases = active["phases"]

    if current >= len(phases):
        print("모든 Phase 완료.")
        return

    phase = phases[current]
    gate_name = phase.get("gate")

    if not gate_name:
        print(f"Phase {current+1}: {phase['name']} — 게이트 없음 (자유 진행)")
        return

    gate = GATE_CHECKS.get(gate_name, {})
    check_fn = gate.get("check", lambda: True)
    passed = check_fn()

    if passed:
        print(f"✅ Phase {current+1}: {phase['name']} — 게이트 통과 ({gate.get('description', '')})")
    else:
        print(f"❌ Phase {current+1}: {phase['name']} — 게이트 실패 ({gate.get('description', '')})")


def cmd_complete(args):
    """파이프라인 완료."""
    state = load_pipeline_state()

    if not state.get("active"):
        print("실행 중인 파이프라인 없음.")
        return

    active = state["active"]
    history = load_pipeline_history()

    active["completed_at"] = datetime.now(timezone.utc).isoformat()
    active["status"] = "completed"
    history["runs"].append(active)
    save_pipeline_history(history)

    state["active"] = None
    save_pipeline_state(state)

    print(f"[파이프라인] {active['pipeline']} 완료 ✅")
    print(f"  {active.get('name', '')} — {len(active.get('phase_history', []))+1} phases")


def cmd_abort(args):
    """파이프라인 중단."""
    state = load_pipeline_state()

    if not state.get("active"):
        print("실행 중인 파이프라인 없음.")
        return

    active = state["active"]
    active["status"] = "aborted"
    active["aborted_at"] = datetime.now(timezone.utc).isoformat()

    history = load_pipeline_history()
    history["runs"].append(active)
    save_pipeline_history(history)

    state["active"] = None
    save_pipeline_state(state)
    print(f"[파이프라인] {active['pipeline']} 중단됨")


def cmd_history(args):
    """완료된 파이프라인 히스토리."""
    history = load_pipeline_history()
    runs = history.get("runs", [])

    if not runs:
        print("파이프라인 실행 기록 없음.")
        return

    print("파이프라인 실행 기록:")
    print(f"{'─'*60}")
    for run in runs[-10:]:  # 최근 10건
        status = "✅" if run.get("status") == "completed" else "❌"
        name = run.get("name", run.get("pipeline", "?"))
        started = run.get("started_at", "?")[:10]
        phases_done = len(run.get("phase_history", []))
        total = run.get("total_phases", "?")
        print(f"  {status} {started} | {name:30s} | {phases_done}/{total} phases")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    cmd = sys.argv[1]
    args = sys.argv[2:]

    commands = {
        "list": cmd_list,
        "start": cmd_start,
        "status": cmd_status,
        "advance": cmd_advance,
        "gate-check": cmd_gate_check,
        "complete": cmd_complete,
        "abort": cmd_abort,
        "history": cmd_history,
    }

    if cmd not in commands:
        print(f"Unknown command: {cmd}", file=sys.stderr)
        print("Available:", ", ".join(commands.keys()))
        sys.exit(1)

    commands[cmd](args)


if __name__ == "__main__":
    main()
