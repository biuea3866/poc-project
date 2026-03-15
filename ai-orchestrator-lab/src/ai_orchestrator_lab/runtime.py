from __future__ import annotations

import json
import logging
import time
from datetime import datetime
from pathlib import Path
from typing import Any

from ai_orchestrator_lab.atlassian import AtlassianClient, AtlassianApiError, JiraIssuePayload
from ai_orchestrator_lab.config import RuntimeConfig
from ai_orchestrator_lab.models import LivePipelineDefinition, LoopPolicy, RunStageStatus, RunStatusSnapshot
from ai_orchestrator_lab.pipeline import (
    LOOP_POLICIES,
    RunContext,
    STAGE_BLUEPRINTS,
    build_agent_definitions,
    build_live_definition,
    build_orchestrator_prompt,
    create_run_directory,
)


logger = logging.getLogger(__name__)


class LiveRuntimeController:
    def __init__(self, context: RunContext):
        self.context = context
        self._stage_start_times: dict[str, float] = {}
        self._loop_retry_counts: dict[str, int] = {}

    def prepare_run(self, run_dir: Path | None = None) -> Path:
        actual_run_dir = run_dir or create_run_directory(self.context.config.output_root)
        actual_run_dir.mkdir(parents=True, exist_ok=True)
        definition = build_live_definition()
        self._write_runtime_metadata(actual_run_dir, definition)
        snapshot = self._build_initial_status(actual_run_dir)
        self._write_run_status(actual_run_dir, snapshot)
        self._append_event(
            actual_run_dir,
            "run_prepared",
            {"run_id": actual_run_dir.name, "prd_path": str(self.context.prd_path)},
        )
        return actual_run_dir

    def _write_runtime_metadata(self, run_dir: Path, definition: LivePipelineDefinition) -> None:
        payload = {
            "prd_path": str(self.context.prd_path),
            "runtime_config": {
                "output_root": str(self.context.config.output_root),
                "worktree_root": str(self.context.config.worktree_root),
                "confluence_site_name": self.context.config.confluence_site_name,
                "confluence_space_key": self.context.config.confluence_space_key,
                "jira_site_name": self.context.config.jira_site_name,
                "jira_project_key": self.context.config.jira_project_key,
                "jira_board_id": self.context.config.jira_board_id,
            },
            "pipeline_definition": definition.to_dict(),
        }
        (run_dir / "live_runtime_definition.json").write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def run_live(self, run_dir: Path | None = None, execute_crew: bool = False) -> dict[str, Any]:
        actual_run_dir = self.prepare_run(run_dir)
        self._transition_run(
            actual_run_dir,
            status="running" if execute_crew else "prepared",
            current_stage_id="01_ambiguity" if execute_crew else None,
            crew_execution_enabled=execute_crew,
        )
        summary: dict[str, Any] = {
            "run_dir": str(actual_run_dir),
            "sdk_executed": False,
            "result_path": None,
            "status_path": str(self._run_status_path(actual_run_dir)),
            "events_path": str(self._events_path(actual_run_dir)),
        }

        if execute_crew:
            import anyio
            self._append_event(actual_run_dir, "sdk_started", {})
            try:
                result = anyio.run(self._run_sdk_pipeline, actual_run_dir)
                output_path = actual_run_dir / "sdk_result.txt"
                output_path.write_text(result, encoding="utf-8")
                summary["sdk_executed"] = True
                summary["result_path"] = str(output_path)
                self._transition_run(
                    actual_run_dir,
                    status="completed",
                    current_stage_id="16_docs",
                    mark_all_completed=True,
                    crew_execution_enabled=True,
                    note="Claude Agent SDK pipeline completed successfully.",
                )
                self._append_event(actual_run_dir, "sdk_completed", {"output_path": str(output_path)})
            except Exception as exc:
                self._transition_run(
                    actual_run_dir,
                    status="failed",
                    current_stage_id=None,
                    crew_execution_enabled=True,
                    note=str(exc),
                )
                self._append_event(actual_run_dir, "sdk_failed", {"error": str(exc)})
                raise

        return summary

    async def _run_sdk_pipeline(self, run_dir: Path) -> str:
        from claude_agent_sdk import query, ClaudeAgentOptions, ResultMessage, HookMatcher

        prd_text = self.context.prd_path.read_text(encoding="utf-8")
        prompt = build_orchestrator_prompt(prd_text, run_dir)
        agents = build_agent_definitions()
        stage_hook = self._build_stage_hook(run_dir)

        result_text = ""
        async for message in query(
            prompt=prompt,
            options=ClaudeAgentOptions(
                cwd=str(run_dir),
                allowed_tools=["Agent", "Read", "Write", "Edit"],
                agents=agents,
                permission_mode="acceptEdits",
                hooks={
                    "PostToolUse": [
                        HookMatcher(matcher="Write", hooks=[stage_hook])
                    ]
                },
            ),
        ):
            if isinstance(message, ResultMessage):
                result_text = message.result

        return result_text

    def _build_stage_hook(self, run_dir: Path):
        stage_ids = {b.id for b in STAGE_BLUEPRINTS}

        async def _hook(input_data, tool_use_id, context):
            file_path = input_data.get("tool_input", {}).get("file_path", "")
            file_name = Path(file_path).name if file_path else ""
            stage_id = file_name.replace(".md", "") if file_name.endswith(".md") else None
            if not (stage_id and stage_id in stage_ids):
                return {}

            now_ts = self._now()

            # Record stage_start if not yet started
            if stage_id not in self._stage_start_times:
                self._stage_start_times[stage_id] = time.monotonic()
                self._append_event(
                    run_dir,
                    "stage_start",
                    {"stage_id": stage_id, "ts": now_ts},
                )

            # Calculate duration and record stage_complete
            start_mono = self._stage_start_times.get(stage_id, time.monotonic())
            duration_s = round(time.monotonic() - start_mono, 2)
            self._mark_stage_completed(run_dir, stage_id)
            self._append_event(
                run_dir,
                "stage_complete",
                {"stage_id": stage_id, "ts": now_ts, "duration_s": duration_s},
            )

            # Confluence auto-sync after 04_confluence_sync
            if stage_id == "04_confluence_sync":
                self._execute_confluence_sync(run_dir)

            # Check loop policies for gate stages
            loop_target = self._check_loop_policies(run_dir, stage_id, file_path)
            if loop_target:
                return {"loop_target": loop_target}

            return {}

        return _hook

    def _check_loop_policies(
        self, run_dir: Path, stage_id: str, output_file_path: str
    ) -> str | None:
        """Check if any loop policy triggers for the completed stage."""
        for policy in LOOP_POLICIES:
            if policy.trigger_stage_id != stage_id:
                continue

            retry_key = f"{policy.trigger_stage_id}->{policy.target_stage_id}"
            current_retries = self._loop_retry_counts.get(retry_key, 0)

            if current_retries >= policy.max_retries:
                self._append_event(run_dir, "loop_max_retries_reached", {
                    "trigger_stage_id": policy.trigger_stage_id,
                    "target_stage_id": policy.target_stage_id,
                    "retries": current_retries,
                })
                continue

            # Read stage output to evaluate condition
            stage_output = ""
            try:
                output_path = Path(output_file_path)
                if output_path.exists():
                    stage_output = output_path.read_text(encoding="utf-8")
            except OSError:
                pass

            triggered = False
            output_upper = stage_output.upper()
            if stage_id == "14_static_analysis" and (
                "FAIL" in output_upper or "CRITICAL" in output_upper
            ):
                triggered = True
            elif stage_id == "15_review_gate" and "MAJOR" in output_upper:
                triggered = True

            if triggered:
                self._loop_retry_counts[retry_key] = current_retries + 1
                self._append_event(run_dir, "loop_policy_triggered", {
                    "trigger_stage_id": policy.trigger_stage_id,
                    "target_stage_id": policy.target_stage_id,
                    "condition": policy.condition,
                    "retry_count": current_retries + 1,
                    "max_retries": policy.max_retries,
                    "reason": policy.note,
                })
                # Reset start times for stages that will be re-executed
                stage_order = [b.id for b in STAGE_BLUEPRINTS]
                target_idx = stage_order.index(policy.target_stage_id)
                for sid in stage_order[target_idx:]:
                    self._stage_start_times.pop(sid, None)
                return policy.target_stage_id

        return None

    def _execute_confluence_sync(self, run_dir: Path) -> None:
        """Auto-sync Confluence pages after 04_confluence_sync stage completes."""
        try:
            result = self.sync_default_confluence_pages(run_dir)
            self._append_event(run_dir, "confluence_auto_sync_success", {
                "updated_pages": len(result.get("updated_pages", [])),
            })
        except Exception as exc:
            logger.warning("Confluence auto-sync failed (non-blocking): %s", exc)
            self._append_event(run_dir, "confluence_auto_sync_failed", {
                "error": str(exc),
            })

    def get_run_status(self, run_dir: Path) -> dict[str, Any]:
        if not run_dir.exists():
            raise AtlassianApiError(f"Run directory not found: {run_dir}")
        status_path = self._run_status_path(run_dir)
        if not status_path.exists():
            raise AtlassianApiError(f"Run status file not found: {status_path}")
        payload = json.loads(status_path.read_text(encoding="utf-8"))
        payload["events_path"] = str(self._events_path(run_dir))
        payload["runtime_definition_path"] = str(run_dir / "live_runtime_definition.json")
        return payload

    def sync_default_confluence_pages(self, run_dir: Path | None = None) -> dict[str, Any]:
        client = AtlassianClient(self.context.config)
        if run_dir is not None and not run_dir.exists():
            raise AtlassianApiError(f"Run directory not found: {run_dir}")
        prd_text = self.context.prd_path.read_text(encoding="utf-8")
        roadmap_path = run_dir / "03_roadmap_update.md" if run_dir else None
        roadmap_text = (
            roadmap_path.read_text(encoding="utf-8")
            if roadmap_path and roadmap_path.exists()
            else None
        )

        page_ids = {
            "prd": self._require_config(self.context.config.prd_page_id, "prd_page_id"),
            "roadmap": self._require_config(self.context.config.roadmap_page_id, "roadmap_page_id"),
            "requirements": self._require_config(
                self.context.config.requirements_page_id,
                "requirements_page_id",
            ),
        }
        updates: list[dict[str, Any]] = []

        updates.append(
            {
                "kind": "prd",
                "result": client.update_page(page_ids["prd"], "AI Wiki", prd_text),
            }
        )
        if page_ids["requirements"] != page_ids["prd"]:
            updates.append(
                {
                    "kind": "requirements",
                    "result": client.update_page(
                        page_ids["requirements"],
                        "AI Wiki Requirements",
                        prd_text,
                    ),
                }
            )
        if roadmap_text is not None:
            updates.append(
                {
                    "kind": "roadmap",
                    "result": client.update_page(page_ids["roadmap"], "AI Wiki Road Map", roadmap_text),
                }
            )
        else:
            updates.append(
                {
                    "kind": "roadmap",
                    "skipped": True,
                    "reason": "No roadmap artifact provided. Pass --run-dir with 03_roadmap_update.md to update roadmap safely.",
                }
            )
        return {"updated_pages": updates}

    def create_jira_issues_from_file(self, tickets_json: Path) -> dict[str, Any]:
        client = AtlassianClient(self.context.config)
        project_key = self._require_config(self.context.config.jira_project_key, "jira_project_key")
        issue_type = self.context.config.jira_issue_type
        if not tickets_json.exists():
            raise AtlassianApiError(f"Ticket JSON file not found: {tickets_json}")
        ticket_payload = json.loads(tickets_json.read_text(encoding="utf-8"))
        tickets = ticket_payload["tickets"] if isinstance(ticket_payload, dict) else ticket_payload
        if not isinstance(tickets, list):
            raise AtlassianApiError("Ticket JSON must be a list or a dict with a `tickets` list.")
        created: list[dict[str, Any]] = []

        for ticket in tickets:
            title = ticket.get("title")
            if not title:
                raise AtlassianApiError("Each ticket must include a `title`.")
            description = self.build_ticket_markdown(ticket)
            payload = JiraIssuePayload(
                summary=f"[{ticket.get('lane', 'common')}] {title}",
                description=description,
                issue_type=issue_type,
                project_key=project_key,
            )
            result = client.create_jira_issue(payload)
            created.append(
                {
                    "ticket_id": ticket.get("id"),
                    "ticket_title": title,
                    "jira_issue_key": result.get("key"),
                    "jira_issue_id": result.get("id"),
                }
            )
        return {"created_issues": created}

    def create_tech_doc(
        self,
        title: str,
        source_path: Path,
        lane: str,
        parent_page_id: str | None = None,
    ) -> dict[str, Any]:
        client = AtlassianClient(self.context.config)
        space_key = self._require_config(self.context.config.confluence_space_key, "confluence_space_key")
        if not source_path.exists():
            raise AtlassianApiError(f"Tech doc source file not found: {source_path}")
        effective_parent_page_id = parent_page_id or self.context.config.tech_doc_parent_page_id
        markdown_body = source_path.read_text(encoding="utf-8")
        body = "\n".join([f"# [{lane.upper()}] {title}", "", markdown_body])
        result = client.create_page(
            space_key=space_key,
            title=title,
            markdown_body=body,
            parent_page_id=effective_parent_page_id,
        )
        return {
            "title": title,
            "lane": lane,
            "parent_page_id": effective_parent_page_id,
            "page": result,
        }

    def transition_jira_issue(self, issue_key: str, status_name: str) -> dict[str, Any]:
        client = AtlassianClient(self.context.config)
        return client.transition_jira_issue_by_name(issue_key, status_name)

    def sync_jira_issue_with_pr_state(self, issue_key: str, pr_state: str) -> dict[str, Any]:
        normalized = pr_state.strip().upper()
        if normalized == "MERGED":
            status_name = "완료"
        elif normalized == "OPEN":
            status_name = "진행 중"
        else:
            status_name = "해야 할 일"
        result = self.transition_jira_issue(issue_key, status_name)
        return {
            "issue_key": issue_key,
            "pr_state": normalized,
            "target_status": status_name,
            "transition": result,
        }

    def write_jira_import_template(self, run_dir: Path | None = None) -> Path:
        actual_run_dir = self.prepare_run(run_dir)
        template = {
            "tickets": [
                {
                    "id": "NAW-AI-001",
                    "title": "예시 티켓",
                    "priority": "P1",
                    "lane": "be",
                    "requirement": "예시 요구사항을 실제 요구사항으로 교체합니다.",
                    "description": "이 예시를 실제 생성된 티켓 내용으로 교체합니다.",
                    "acceptance_criteria": ["예시 수용 기준을 실제 기준으로 교체합니다."],
                    "risk_score": 2,
                    "complexity_score": 3,
                    "daily_capacity_cost": 3,
                    "cross_lane": False,
                    "recommended_day_bucket": "single-day",
                    "owner_role": "be_engineer",
                    "reviewer_role": "qa",
                    "design_markdown": "\n".join(
                        [
                            "- 데이터 흐름: 요청 -> 애플리케이션 서비스 -> 포트 -> 어댑터",
                            "- ERD: 대상 엔티티와 연관 관계를 정리합니다.",
                            "- 컴포넌트 다이어그램: API, 서비스, 인프라 구성을 명시합니다.",
                            "- 플로우 다이어그램: 정상 흐름과 예외 흐름을 포함합니다.",
                        ]
                    ),
                }
            ]
        }
        path = actual_run_dir / "08_ticket_breakdown.sample.json"
        path.write_text(json.dumps(template, ensure_ascii=False, indent=2), encoding="utf-8")
        return path

    def _build_initial_status(self, run_dir: Path) -> RunStatusSnapshot:
        created_at = self._now()
        return RunStatusSnapshot(
            run_id=run_dir.name,
            prd_path=str(self.context.prd_path),
            status="prepared",
            created_at=created_at,
            updated_at=created_at,
            current_stage_id=None,
            crew_execution_enabled=False,
            stages=tuple(
                RunStageStatus(
                    stage_id=blueprint.id,
                    title=blueprint.title,
                    lane=blueprint.lane,
                    owner=blueprint.owner,
                )
                for blueprint in STAGE_BLUEPRINTS
            ),
            notes=(
                "Claude Agent SDK 기반 파이프라인입니다.",
                "각 단계 완료 시 stage_completed 이벤트가 events.ndjson에 기록됩니다.",
            ),
        )

    def _transition_run(
        self,
        run_dir: Path,
        *,
        status: str,
        current_stage_id: str | None,
        crew_execution_enabled: bool,
        mark_all_completed: bool = False,
        note: str | None = None,
    ) -> None:
        payload = self.get_run_status(run_dir)
        payload["status"] = status
        payload["updated_at"] = self._now()
        payload["current_stage_id"] = current_stage_id
        payload["crew_execution_enabled"] = crew_execution_enabled
        stages = payload.get("stages", [])
        if mark_all_completed:
            finished_at = self._now()
            for stage in stages:
                stage["status"] = "completed"
                stage["started_at"] = stage.get("started_at") or finished_at
                stage["finished_at"] = finished_at
        if note:
            notes = list(payload.get("notes", []))
            notes.append(note)
            payload["notes"] = notes
        self._run_status_path(run_dir).write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def _mark_stage_completed(self, run_dir: Path, stage_id: str) -> None:
        payload = self.get_run_status(run_dir)
        now = self._now()
        payload["status"] = "running"
        payload["updated_at"] = now
        payload["current_stage_id"] = stage_id
        for stage in payload.get("stages", []):
            if stage["stage_id"] == stage_id:
                stage["status"] = "completed"
                stage["started_at"] = stage.get("started_at") or now
                stage["finished_at"] = now
                break
        self._run_status_path(run_dir).write_text(
            json.dumps(payload, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def _write_run_status(self, run_dir: Path, snapshot: RunStatusSnapshot) -> None:
        self._run_status_path(run_dir).write_text(
            json.dumps(snapshot.to_dict(), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def _append_event(self, run_dir: Path, event_type: str, payload: dict[str, Any]) -> None:
        event = {"timestamp": self._now(), "type": event_type, "payload": payload}
        with self._events_path(run_dir).open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(event, ensure_ascii=False) + "\n")

    @staticmethod
    def _run_status_path(run_dir: Path) -> Path:
        return run_dir / "run_status.json"

    @staticmethod
    def _events_path(run_dir: Path) -> Path:
        return run_dir / "events.ndjson"

    @staticmethod
    def _now() -> str:
        return datetime.now().isoformat(timespec="seconds")

    @staticmethod
    def build_ticket_markdown(ticket: dict[str, Any]) -> str:
        visual_markdown = ticket.get("visual_markdown", "").strip()
        return "\n".join(
            [
                "## 개요",
                f"- 티켓 ID: {ticket.get('id', '')}",
                f"- 레인: {ticket.get('lane', 'common')}",
                f"- 담당 역할: {ticket.get('owner_role', '')}",
                f"- 리뷰 역할: {ticket.get('reviewer_role', '')}",
                f"- 우선순위: {ticket.get('priority', 'P1')}",
                f"- 리스크 점수: {ticket.get('risk_score', '')}",
                f"- 복잡도 점수: {ticket.get('complexity_score', '')}",
                "",
                "## 목표",
                ticket.get("requirement", ""),
                "",
                "## 작업 내용",
                ticket.get("description", ""),
                "",
                "## 설계",
                ticket.get("design_markdown", "- 별도 설계 메모 없음"),
                "",
                "## 시각화",
                visual_markdown or "```text\n시각화 초안이 아직 없습니다.\n```",
                "",
                "## 테스트 케이스",
                *[
                    f"- Given: {case.get('given', '')} / When: {case.get('when', '')} / Then: {case.get('then', '')}"
                    for case in ticket.get("test_cases", [])
                ],
                "",
                "## 수용 기준",
                *[f"- {item}" for item in ticket.get("acceptance_criteria", [])],
            ]
        ).strip()

    @staticmethod
    def _require_config(value: str | None, field_name: str) -> str:
        if not value:
            raise AtlassianApiError(f"Missing config field: {field_name}")
        return value
