from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Literal


Lane = Literal["common", "fe", "be", "devops"]
Owner = Literal["pm", "architect", "tech_lead", "developer", "qa"]
StageKind = Literal["analysis", "planning", "execution", "gate", "documentation"]
RunStageStatusType = Literal["pending", "running", "completed", "failed", "skipped"]
RunStatusType = Literal["prepared", "running", "completed", "failed"]


@dataclass(frozen=True)
class StageBlueprint:
    id: str
    title: str
    goal: str
    output_hint: str
    owner: Owner
    lane: Lane
    kind: StageKind
    depends_on: tuple[str, ...] = ()
    loop_target_on_failure: str | None = None


@dataclass(frozen=True)
class StageArtifact:
    stage_id: str
    title: str
    owner: Owner
    lane: Lane
    kind: StageKind
    summary: str
    output_path: str
    next_input_hint: str

    def to_dict(self) -> dict[str, str]:
        return asdict(self)


@dataclass(frozen=True)
class AmbiguityItem:
    id: str
    location: str
    issue: str
    question: str
    impact: Literal["HIGH", "MEDIUM", "LOW"]
    default_assumption: str


@dataclass(frozen=True)
class Ticket:
    id: str
    title: str
    priority: Literal["P0", "P1", "P2"]
    lane: Literal["fe", "be", "devops", "common"]
    owner_role: Literal[
        "pm",
        "architect",
        "tech_lead",
        "be_engineer",
        "fe_engineer",
        "devops_engineer",
        "qa",
    ]
    requirement: str
    description: str
    acceptance_criteria: list[str]
    reviewer_role: Literal["qa", "architect", "tech_lead"] = "qa"
    dependencies: list[str] = field(default_factory=list)
    risk_score: int = 1
    complexity_score: int = 1
    daily_capacity_cost: int = 1
    cross_lane: bool = False
    recommended_day_bucket: Literal["single-day", "multi-day", "buffer-only"] = "single-day"
    worktree_name: str = ""


@dataclass(frozen=True)
class ConfluenceUpdateTarget:
    title: str
    page_id_env: str
    purpose: str


@dataclass(frozen=True)
class AgentSpec:
    key: str
    role: str
    goal: str
    backstory: str
    llm: Literal["openai", "anthropic", "google"]


@dataclass(frozen=True)
class TaskSpec:
    key: str
    stage_id: str
    description: str
    expected_output: str
    agent_key: str
    context_keys: tuple[str, ...] = ()
    output_json_schema: str | None = None
    human_input: bool = False


@dataclass(frozen=True)
class LoopPolicy:
    trigger_stage_id: str
    condition: str
    target_stage_id: str
    max_retries: int
    note: str


@dataclass(frozen=True)
class LivePipelineDefinition:
    agents: tuple[AgentSpec, ...]
    tasks: tuple[TaskSpec, ...]
    loop_policies: tuple[LoopPolicy, ...]

    def to_dict(self) -> dict[str, object]:
        return asdict(self)


@dataclass(frozen=True)
class RunStageStatus:
    stage_id: str
    title: str
    lane: Lane
    owner: Owner
    status: RunStageStatusType = "pending"
    started_at: str | None = None
    finished_at: str | None = None
    note: str | None = None


@dataclass(frozen=True)
class RunStatusSnapshot:
    run_id: str
    prd_path: str
    status: RunStatusType
    created_at: str
    updated_at: str
    current_stage_id: str | None
    crew_execution_enabled: bool
    observability_level: Literal["run-level", "stage-level"] = "run-level"
    stages: tuple[RunStageStatus, ...] = ()
    notes: tuple[str, ...] = ()

    def to_dict(self) -> dict[str, object]:
        return asdict(self)
