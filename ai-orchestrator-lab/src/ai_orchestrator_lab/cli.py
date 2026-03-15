from __future__ import annotations

import argparse
import json
from pathlib import Path

from ai_orchestrator_lab.config import RuntimeConfig
from ai_orchestrator_lab.pipeline import RunContext, build_live_pipeline, run_dry_pipeline
from ai_orchestrator_lab.runtime import LiveRuntimeController


def build_context(prd: str) -> RunContext:
    project_root = Path(__file__).resolve().parents[2]
    prd_path = Path(prd).resolve()
    if not prd_path.exists():
        raise FileNotFoundError(f"PRD file not found: {prd_path}")
    return RunContext(
        project_root=project_root,
        prd_path=prd_path,
        config=RuntimeConfig.from_env(project_root),
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="AI orchestration playground CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    dry_run = subparsers.add_parser("dry-run", help="Create dry-run artifacts")
    dry_run.add_argument("--prd", required=True, help="Path to the input PRD markdown file")

    live_run = subparsers.add_parser("live-run", help="Prepare or execute a live runtime run")
    live_run.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    live_run.add_argument("--execute-crew", action="store_true", help="Execute Claude Agent SDK pipeline")

    show_live = subparsers.add_parser("live-definition", help="Show live pipeline summary")
    show_live.add_argument("--prd", required=True, help="Path to the input PRD markdown file")

    run_status = subparsers.add_parser("run-status", help="Show live run status and event paths")
    run_status.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    run_status.add_argument("--run-dir", required=True, help="Run directory to inspect")

    sync_confluence = subparsers.add_parser(
        "sync-confluence",
        help="Update default Confluence pages from PRD and run artifacts",
    )
    sync_confluence.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    sync_confluence.add_argument("--run-dir", help="Run directory containing stage artifacts")

    create_jira = subparsers.add_parser("create-jira", help="Create Jira issues from ticket JSON")
    create_jira.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    create_jira.add_argument("--tickets-json", required=True, help="Path to ticket JSON file")

    transition_jira = subparsers.add_parser("transition-jira", help="Transition a Jira issue")
    transition_jira.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    transition_jira.add_argument("--issue-key", required=True, help="Jira issue key")
    transition_jira.add_argument("--status-name", required=True, help="Target Jira status name")

    sync_jira_pr = subparsers.add_parser("sync-jira-pr", help="Sync a Jira issue from PR state")
    sync_jira_pr.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    sync_jira_pr.add_argument("--issue-key", required=True, help="Jira issue key")
    sync_jira_pr.add_argument(
        "--pr-state",
        required=True,
        choices=("OPEN", "MERGED", "CLOSED"),
        help="PR state to map into Jira",
    )

    create_tech_doc = subparsers.add_parser(
        "create-tech-doc",
        help="Create a Confluence technical design document",
    )
    create_tech_doc.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    create_tech_doc.add_argument("--title", required=True, help="Confluence page title")
    create_tech_doc.add_argument(
        "--lane",
        choices=("fe", "be", "devops", "common"),
        required=True,
        help="Owner lane",
    )
    create_tech_doc.add_argument("--source", required=True, help="Markdown file to publish")
    create_tech_doc.add_argument("--parent-page-id", help="Optional Confluence parent page id")

    write_ticket_template = subparsers.add_parser(
        "write-ticket-template",
        help="Write a sample Jira import ticket JSON file into a run directory",
    )
    write_ticket_template.add_argument("--prd", required=True, help="Path to the input PRD markdown file")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    context = build_context(args.prd)
    controller = LiveRuntimeController(context)

    if args.command == "dry-run":
        run_dir = run_dry_pipeline(context)
        print(f"dry-run completed: {run_dir}")
        return

    if args.command == "live-run":
        result = controller.run_live(execute_crew=args.execute_crew)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "live-definition":
        print(build_live_pipeline(context))
        return

    if args.command == "run-status":
        result = controller.get_run_status(Path(args.run_dir).resolve())
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "sync-confluence":
        run_dir = Path(args.run_dir).resolve() if args.run_dir else None
        result = controller.sync_default_confluence_pages(run_dir=run_dir)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "create-jira":
        result = controller.create_jira_issues_from_file(Path(args.tickets_json).resolve())
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "transition-jira":
        result = controller.transition_jira_issue(args.issue_key, args.status_name)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "sync-jira-pr":
        result = controller.sync_jira_issue_with_pr_state(args.issue_key, args.pr_state)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "create-tech-doc":
        result = controller.create_tech_doc(
            title=args.title,
            source_path=Path(args.source).resolve(),
            lane=args.lane,
            parent_page_id=args.parent_page_id,
        )
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    if args.command == "write-ticket-template":
        path = controller.write_jira_import_template()
        print(path)
        return

    raise RuntimeError(f"Unsupported command: {args.command}")


if __name__ == "__main__":
    main()
