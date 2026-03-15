"""
team_pipeline.py
================
Claude Team 기반 파이프라인 실행기.

pipeline.py의 STAGE_BLUEPRINTS를 읽어
Claude Agent SDK subprocess 없이 순수하게
TaskCreate + Agent 시스템으로 16단계를 실행한다.

각 스테이지는 로컬 마크다운 산출물뿐만 아니라
Confluence 문서 / Jira 티켓 등 실제 외부 산출물을 생성한다.

실행 방법:
    python -m ai_orchestrator_lab.team_pipeline --prd input/prd.md

역할 매핑 (pipeline.py owner → Claude team member):
    pm          → pm
    architect   → orchestrator-dev
    tech_lead   → orchestrator-dev
    developer(fe)     → fe-developer
    developer(be)     → be-developer
    developer(devops) → orchestrator-dev
    qa          → orchestrator-dev

Confluence / Jira 산출물 규칙:
    Stage 04  (pm)            Confluence: PRD·요구사항·로드맵 3개 페이지 업데이트
    Stage 08  (orchestrator)  Jira: NAW 프로젝트에 티켓 일괄 생성
    Stage 11  (fe-developer)  Confluence: FE 기술 설계 문서 + Jira: 티켓 In Progress
    Stage 12  (be-developer)  Confluence: BE 기술 설계 문서(코딩 전 선작성) + Jira: In Progress
    Stage 13  (orchestrator)  Confluence: DevOps 기술 설계 문서
    Stage 16  (pm)            Confluence: 최종 운영·릴리즈 문서 + Jira: 티켓 Done
"""

from __future__ import annotations

import json
import textwrap
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

from ai_orchestrator_lab.pipeline import STAGE_BLUEPRINTS
from ai_orchestrator_lab.models import StageBlueprint

# ──────────────────────────────────────────────
# 역할 매핑
# ──────────────────────────────────────────────

OWNER_TO_AGENT: dict[str, str] = {
    "pm": "pm",
    "architect": "orchestrator-dev",
    "tech_lead": "orchestrator-dev",
    "qa": "orchestrator-dev",
}

LANE_TO_AGENT: dict[str, str] = {
    "fe": "fe-developer",
    "be": "be-developer",
    "devops": "orchestrator-dev",
    "common": "orchestrator-dev",
}


def resolve_agent(blueprint: StageBlueprint) -> str:
    if blueprint.owner == "developer":
        return LANE_TO_AGENT.get(blueprint.lane, "orchestrator-dev")
    return OWNER_TO_AGENT.get(blueprint.owner, "orchestrator-dev")


# ──────────────────────────────────────────────
# 스테이지별 외부 산출물 명세
# ──────────────────────────────────────────────

@dataclass
class ExternalDeliverable:
    """Confluence 페이지 또는 Jira 티켓 산출물."""
    kind: str          # "confluence_update" | "confluence_create" | "jira_create" | "jira_transition"
    description: str   # 에이전트에게 보여줄 지시 내용
    cli_hint: str      # 사용할 CLI 명령 힌트


STAGE_DELIVERABLES: dict[str, list[ExternalDeliverable]] = {
    "04_confluence_sync": [
        ExternalDeliverable(
            kind="confluence_update",
            description="PRD, 요구사항, 로드맵 3개 Confluence 페이지를 현행화한다.",
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py sync-confluence "
                "--prd input/prd.md --run-dir outputs/team_run"
            ),
        ),
    ],
    "08_ticket_breakdown": [
        ExternalDeliverable(
            kind="jira_create",
            description=(
                "티켓 분해 결과를 JSON으로 저장한 뒤 Jira NAW 프로젝트에 이슈를 일괄 생성한다. "
                "출력: outputs/team_run/08_tickets.json"
            ),
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py create-jira "
                "--prd input/prd.md --tickets-json outputs/team_run/08_tickets.json"
            ),
        ),
    ],
    "11_fe_codegen": [
        ExternalDeliverable(
            kind="confluence_create",
            description=(
                "FE 기술 설계 문서를 Confluence에 생성한다. "
                "(제목: 'AI Wiki FE Technical Design', lane: fe)"
            ),
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py create-tech-doc "
                "--prd input/prd.md --title 'AI Wiki FE Technical Design' "
                "--lane fe --source outputs/team_run/11_fe_codegen.md"
            ),
        ),
        # ⚠️ Jira 'In Progress' 전환은 Stage 16 PR 생성 시점에 수행한다.
    ],
    "12_be_codegen": [
        ExternalDeliverable(
            kind="confluence_create",
            description=(
                "⚠️ 코딩 시작 전에 먼저 작성. "
                "BE 기술 설계 문서(아키텍처, API 명세, 도메인 모델)를 Confluence에 생성한다. "
                "(제목: 'AI Wiki BE Technical Design', lane: be)"
            ),
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py create-tech-doc "
                "--prd input/prd.md --title 'AI Wiki BE Technical Design' "
                "--lane be --source outputs/team_run/12_be_tech_doc.md"
            ),
        ),
        # ⚠️ Jira 'In Progress' 전환은 Stage 16 PR 생성 시점에 수행한다.
    ],
    "13_devops_codegen": [
        ExternalDeliverable(
            kind="confluence_create",
            description=(
                "DevOps 기술 설계 문서(인프라, CI/CD, 모니터링)를 Confluence에 생성한다. "
                "(제목: 'AI Wiki DevOps Technical Design', lane: devops)"
            ),
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py create-tech-doc "
                "--prd input/prd.md --title 'AI Wiki DevOps Technical Design' "
                "--lane devops --source outputs/team_run/13_devops_codegen.md"
            ),
        ),
        # ⚠️ Jira 'In Progress' 전환은 Stage 16 PR 생성 시점에 수행한다.
    ],
    "16_pr_creation": [
        ExternalDeliverable(
            kind="github_pr",
            description=(
                "lane별 GitHub PR을 생성한다. "
                "FE, BE, DevOps 각 worktree 브랜치 → main PR을 순서대로 생성하고 "
                "PR 본문에 변경 요약, 테스트 체크리스트, 관련 Jira 티켓 번호를 포함한다. "
                "⚠️ PR 머지는 사용자가 직접 수행한다. 자동 머지 금지."
            ),
            cli_hint="gh pr create --base main --title '<lane> <description>' --body '<body>'",
        ),
        ExternalDeliverable(
            kind="jira_transition",
            description="lane별 Jira 티켓을 'In Progress' 상태로 전환한다. (FE, BE, DevOps 각 티켓)",
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py transition-jira "
                "--prd input/prd.md --issue-key NAW-XXX --status-name 'In Progress'"
            ),
        ),
    ],
    "17_post_merge_docs": [
        ExternalDeliverable(
            kind="confluence_create",
            description="최종 운영 문서 및 릴리즈 노트를 Confluence에 게시한다.",
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py create-tech-doc "
                "--prd input/prd.md --title 'AI Wiki Release Notes v1.0' "
                "--lane common --source outputs/team_run/17_post_merge_docs.md"
            ),
        ),
        ExternalDeliverable(
            kind="jira_transition",
            description="머지 완료된 Jira 티켓을 'Done' 상태로 전환한다.",
            cli_hint=(
                "python3 src/ai_orchestrator_lab/cli.py transition-jira "
                "--prd input/prd.md --issue-key NAW-XXX --status-name 'Done'"
            ),
        ),
    ],
}


# ──────────────────────────────────────────────
# 스테이지 → Task 명세 변환
# ──────────────────────────────────────────────

@dataclass
class TeamStageTask:
    stage_id: str
    title: str
    agent: str
    subject: str
    description: str
    output_path: str
    depends_on: tuple[str, ...] = field(default_factory=tuple)
    loop_target_on_failure: Optional[str] = None
    deliverables: list[ExternalDeliverable] = field(default_factory=list)


def _build_deliverables_section(deliverables: list[ExternalDeliverable]) -> str:
    if not deliverables:
        return ""
    lines = ["\n### 외부 산출물 (Confluence / Jira)"]
    for d in deliverables:
        lines.append(f"\n**[{d.kind}]** {d.description}")
        lines.append(f"```bash\n{d.cli_hint}\n```")
    return "\n".join(lines)


def _build_task_description(bp: StageBlueprint, run_dir: Path, prd_path: Path) -> str:
    deps_note = ""
    if bp.depends_on:
        dep_files = "\n".join(
            f"  - {run_dir / (dep + '.md')}" for dep in bp.depends_on
        )
        deps_note = f"\n**선행 입력 파일 (반드시 먼저 읽을 것):**\n{dep_files}\n"

    loop_note = ""
    if bp.loop_target_on_failure:
        loop_note = (
            f"\n**실패 시 루프백:** `{bp.loop_target_on_failure}` 스테이지부터 재실행 필요 — "
            "팀장에게 FAILED 상태와 이유를 보고할 것.\n"
        )

    deliverables_section = _build_deliverables_section(
        STAGE_DELIVERABLES.get(bp.id, [])
    )

    project_root = run_dir.parent.parent

    return textwrap.dedent(f"""\
        ## 스테이지: {bp.id} — {bp.title}

        **목표:** {bp.goal}

        **PRD 파일:** `{prd_path}`
        **로컬 출력 파일:** `{run_dir / (bp.id + '.md')}`
        **프로젝트 루트:** `{project_root}`
        {deps_note}
        **출력 힌트:** {bp.output_hint}
        {loop_note}
        {deliverables_section}

        ### 수행 절차
        1. PRD 및 선행 스테이지 출력 파일을 Read 도구로 읽는다.
        2. 목표에 맞는 분석/설계/코드를 수행한다.
        3. 결과를 마크다운으로 작성해 **로컬 출력 파일**에 Write한다.
        4. 외부 산출물이 있는 경우 위의 CLI 명령을 Bash 도구로 실행한다.
           (실행 디렉토리: `{project_root}`, venv: `.venv/bin/python3`)
        5. TaskUpdate로 이 태스크를 completed 처리한다.
        6. SendMessage(to: "team-lead")로 완료를 보고한다.
           보고 형식:
           - 스테이지 ID
           - 주요 결론 3줄 요약
           - 로컬 출력 파일 경로
           - Confluence/Jira 산출물 결과 (URL 또는 이슈 키)

        ### 품질 기준
        - 출력 파일이 존재하고 내용이 비어있지 않아야 한다.
        - 선행 스테이지 내용을 반드시 참조해 일관성을 유지해야 한다.
        - 코드 생성 스테이지(11~13)는 실제 동작 가능한 코드를 작성한다.
        - BE 코딩(Stage 12)은 반드시 Confluence 기술 문서를 먼저 작성한 뒤 코드를 작성한다.

        ### 다이어그램 작성 규칙
        - ASCII/텍스트 다이어그램 사용 금지.
        - 아키텍처, ERD, 시퀀스 다이어그램은 **Excalidraw JSON** 또는 **draw.io XML**로 작성한다.
        - 마크다운 파일 안에 다이어그램이 필요한 경우:
          1. 별도 파일로 저장 (`outputs/team_run/<stage_id>_<name>.excalidraw` 또는 `.drawio`)
          2. 마크다운에서 파일 경로를 참조: `[아키텍처 다이어그램](./<stage_id>_<name>.excalidraw)`
          3. Confluence 업로드 시 draw.io 매크로로 렌더링됨
        - Excalidraw JSON 최소 구조:
          ```json
          {{"type":"excalidraw","version":2,"elements":[],"appState":{{"viewBackgroundColor":"#ffffff"}}}}
          ```
    """)


def build_team_pipeline(prd_path: Path, run_dir: Path) -> list[TeamStageTask]:
    tasks: list[TeamStageTask] = []
    for bp in STAGE_BLUEPRINTS:
        agent = resolve_agent(bp)
        tasks.append(TeamStageTask(
            stage_id=bp.id,
            title=bp.title,
            agent=agent,
            subject=f"[{bp.id}] {bp.title}",
            description=_build_task_description(bp, run_dir, prd_path),
            output_path=str(run_dir / (bp.id + ".md")),
            depends_on=bp.depends_on or (),
            loop_target_on_failure=bp.loop_target_on_failure,
            deliverables=STAGE_DELIVERABLES.get(bp.id, []),
        ))
    return tasks


# ──────────────────────────────────────────────
# 파이프라인 요약 출력
# ──────────────────────────────────────────────

def print_pipeline_summary(tasks: list[TeamStageTask]) -> None:
    print("\n" + "=" * 72)
    print("  Claude Team Pipeline — 16 Stage Plan (with Confluence / Jira)")
    print("=" * 72)
    print(f"  {'Stage':<26} {'Agent':<20} {'Confluence/Jira':<20}")
    print("  " + "-" * 68)
    for t in tasks:
        external = ", ".join(d.kind for d in t.deliverables) if t.deliverables else "-"
        loop = f"  loop→{t.loop_target_on_failure}" if t.loop_target_on_failure else ""
        print(f"  {t.stage_id:<26} {t.agent:<20} {external:<20}{loop}")
    print("=" * 72)
    print()


# ──────────────────────────────────────────────
# CLI 진입점
# ──────────────────────────────────────────────

def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(
        description="Claude Team 기반 파이프라인 계획 출력 및 run 디렉토리 준비"
    )
    parser.add_argument("--prd", required=True, help="PRD 마크다운 파일 경로")
    parser.add_argument("--output-dir", default="outputs/team_run")
    parser.add_argument(
        "--format",
        choices=["summary", "json"],
        default="summary",
    )
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[2]
    prd_path = (project_root / args.prd).resolve()
    run_dir = (project_root / args.output_dir).resolve()
    run_dir.mkdir(parents=True, exist_ok=True)

    tasks = build_team_pipeline(prd_path, run_dir)

    if args.format == "summary":
        print_pipeline_summary(tasks)
    elif args.format == "json":
        print(json.dumps(
            [
                {
                    "stage_id": t.stage_id,
                    "title": t.title,
                    "agent": t.agent,
                    "output_path": t.output_path,
                    "depends_on": list(t.depends_on),
                    "loop_target_on_failure": t.loop_target_on_failure,
                    "deliverables": [
                        {"kind": d.kind, "description": d.description}
                        for d in t.deliverables
                    ],
                }
                for t in tasks
            ],
            ensure_ascii=False,
            indent=2,
        ))


if __name__ == "__main__":
    main()
