from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

try:
    from dotenv import load_dotenv
except ImportError:  # pragma: no cover
    def load_dotenv() -> bool:
        return False


load_dotenv()


def _load_local_env_file(project_root: Path) -> None:
    env_path = project_root / ".env"
    if not env_path.exists():
        return
    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())


@dataclass(frozen=True)
class RuntimeConfig:
    output_root: Path
    worktree_root: Path
    confluence_space_key: str | None = None
    confluence_site_name: str | None = None
    prd_page_id: str | None = None
    roadmap_page_id: str | None = None
    requirements_page_id: str | None = None
    tech_doc_parent_page_id: str | None = None
    atlassian_email: str | None = None
    atlassian_api_token: str | None = None
    jira_site_name: str | None = None
    jira_project_key: str | None = None
    jira_board_id: str | None = None
    jira_issue_type: str = "Task"
    openai_model: str = "gpt-5.2"
    anthropic_model: str = "claude-sonnet-4-20250514"
    google_model: str = "gemini-3-pro-preview"
    max_review_loops: int = 2
    max_codegen_retries: int = 3

    @classmethod
    def from_env(cls, project_root: Path) -> "RuntimeConfig":
        _load_local_env_file(project_root)
        output_dir = os.getenv("AI_ORCHESTRATOR_OUTPUT_DIR", "outputs")
        worktree_dir = os.getenv("AI_ORCHESTRATOR_WORKTREE_DIR", "worktrees")
        return cls(
            output_root=project_root / output_dir,
            worktree_root=project_root / worktree_dir,
            confluence_space_key=os.getenv("AI_ORCHESTRATOR_CONFLUENCE_SPACE_KEY"),
            confluence_site_name=os.getenv("AI_ORCHESTRATOR_CONFLUENCE_SITE"),
            prd_page_id=os.getenv("AI_ORCHESTRATOR_PRD_PAGE_ID"),
            roadmap_page_id=os.getenv("AI_ORCHESTRATOR_ROADMAP_PAGE_ID"),
            requirements_page_id=os.getenv("AI_ORCHESTRATOR_REQUIREMENTS_PAGE_ID"),
            tech_doc_parent_page_id=os.getenv("AI_ORCHESTRATOR_TECH_DOC_PARENT_PAGE_ID"),
            atlassian_email=os.getenv("AI_ORCHESTRATOR_ATLASSIAN_EMAIL"),
            atlassian_api_token=os.getenv("AI_ORCHESTRATOR_ATLASSIAN_API_TOKEN"),
            jira_site_name=os.getenv("AI_ORCHESTRATOR_JIRA_SITE"),
            jira_project_key=os.getenv("AI_ORCHESTRATOR_JIRA_PROJECT_KEY"),
            jira_board_id=os.getenv("AI_ORCHESTRATOR_JIRA_BOARD_ID"),
            jira_issue_type=os.getenv("AI_ORCHESTRATOR_JIRA_ISSUE_TYPE", "Task"),
        )
