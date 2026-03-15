from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
import json

from ai_orchestrator_lab.config import RuntimeConfig
from ai_orchestrator_lab.models import (
    AgentSpec,
    ConfluenceUpdateTarget,
    LivePipelineDefinition,
    LoopPolicy,
    StageArtifact,
    StageBlueprint,
    TaskSpec,
)


STAGE_BLUEPRINTS = [
    StageBlueprint(
        id="01_ambiguity",
        title="Ambiguity Review",
        goal="PRD에서 설계 결정을 막는 모호성과 누락을 식별하고 HIGH impact 질문을 추출한다.",
        output_hint="질문 목록, 영향도, 기본 가정",
        owner="pm",
        lane="common",
        kind="analysis",
    ),
    StageBlueprint(
        id="02_requirements_analysis",
        title="Requirements Analysis",
        goal="기능, 비기능, 제약사항, 성공 지표를 구조화해 요구사항을 정리한다.",
        output_hint="기능/비기능 요구사항, 제약사항, 성공 지표",
        owner="pm",
        lane="common",
        kind="analysis",
        depends_on=("01_ambiguity",),
    ),
    StageBlueprint(
        id="03_roadmap_update",
        title="Roadmap Update",
        goal="요구사항 분석 결과를 반영해 PRD, 요구사항, 릴리즈 로드맵을 최신화한다.",
        output_hint="최신 PRD, 요구사항 문서, 릴리즈 로드맵",
        owner="pm",
        lane="common",
        kind="planning",
        depends_on=("02_requirements_analysis",),
    ),
    StageBlueprint(
        id="04_confluence_sync",
        title="Confluence Sync",
        goal="PM이 Confluence에 PRD, 요구사항, 로드맵 페이지를 직접 업데이트한다.",
        output_hint="업데이트 대상 페이지, 변경 요약, 게시 결과",
        owner="pm",
        lane="common",
        kind="documentation",
        depends_on=("03_roadmap_update",),
    ),
    StageBlueprint(
        id="05_technical_analysis",
        title="Technical Analysis",
        goal="기술 스택 적합성, 통합 포인트, 외부 의존성, 운영 제약을 분석한다.",
        output_hint="스택 평가, 통합 포인트, 리스크 메모",
        owner="architect",
        lane="common",
        kind="analysis",
        depends_on=("03_roadmap_update",),
    ),
    StageBlueprint(
        id="06_architecture",
        title="Architecture Draft",
        goal="멀티 모듈 구조, 데이터 흐름, 시스템 경계, 모듈 의존성을 설계한다.",
        output_hint="아키텍처 다이어그램, 모듈 책임, 의존성 맵",
        owner="architect",
        lane="common",
        kind="planning",
        depends_on=("05_technical_analysis",),
    ),
    StageBlueprint(
        id="07_parallel_plan",
        title="Parallel Worktree Plan",
        goal="fe, be, devops lane으로 작업을 분리하고 worktree 기반 병렬 전략을 정의하며 Pinpoint 같은 cross-lane 협업 범위를 식별한다.",
        output_hint="lane별 범위, 브랜치 이름, worktree 경로, 병합 순서, cross-lane 협업 항목",
        owner="tech_lead",
        lane="common",
        kind="planning",
        depends_on=("06_architecture",),
    ),
    StageBlueprint(
        id="08_ticket_breakdown",
        title="Ticket Breakdown",
        goal="독립 PR이 가능한 크기로 티켓을 쪼개고 lane과 선행 의존성을 할당하며 Pinpoint 구축 같은 cross-lane 티켓을 분리하고 Jira 등록 기준으로 정리한다.",
        output_hint="id, lane, acceptance criteria, dependencies가 있는 티켓 리스트와 Jira 등록 대상 cross-lane 티켓",
        owner="tech_lead",
        lane="common",
        kind="planning",
        depends_on=("07_parallel_plan",),
    ),
    StageBlueprint(
        id="09_risk_scoring",
        title="Risk And Complexity Scoring",
        goal="각 티켓의 리스크와 복잡도를 수치화해 우선순위와 병렬 실행 순서를 재정렬한다.",
        output_hint="risk_score, complexity_score, 권장 구현 순서",
        owner="tech_lead",
        lane="common",
        kind="planning",
        depends_on=("08_ticket_breakdown",),
    ),
    StageBlueprint(
        id="10_test_design",
        title="Ticket Test Design",
        goal="티켓별 Given/When/Then 테스트 시나리오와 검증 케이스를 먼저 정의한다.",
        output_hint="티켓별 테스트 케이스와 검증 포인트",
        owner="developer",
        lane="common",
        kind="planning",
        depends_on=("09_risk_scoring",),
    ),
    StageBlueprint(
        id="11_fe_codegen",
        title="FE Code Generation",
        goal="FE lane 티켓을 기준으로 Next.js 코드를 생성하고 FE worktree 계획을 남긴다.",
        output_hint="FE 코드 초안, 변경 파일 목록, worktree 메모",
        owner="developer",
        lane="fe",
        kind="execution",
        depends_on=("10_test_design",),
    ),
    StageBlueprint(
        id="12_be_codegen",
        title="BE Code Generation",
        goal="BE lane 티켓을 기준으로 객체지향, 클린 코드, 헥사고날 아키텍처 원칙에 맞는 Kotlin 코드를 생성하고 BE worktree 계획을 남긴다. 기술 논의가 필요하면 Confluence 기술 문서를 생성한다.",
        output_hint="BE 코드 초안, 변경 파일 목록, worktree 메모, 계층 분리 메모, 필요 시 기술 문서 링크",
        owner="developer",
        lane="be",
        kind="execution",
        depends_on=("10_test_design",),
    ),
    StageBlueprint(
        id="13_devops_codegen",
        title="DevOps Implementation",
        goal="DevOps lane 티켓을 기준으로 인프라와 자동화 구성을 생성한다. 기술 논의가 필요하면 Confluence 기술 문서를 생성한다.",
        output_hint="CI/CD, 환경 변수, 배포 스크립트 초안, 필요 시 기술 문서 링크",
        owner="developer",
        lane="devops",
        kind="execution",
        depends_on=("10_test_design",),
    ),
    StageBlueprint(
        id="14_static_analysis",
        title="Static Analysis Gate",
        goal="빌드, 테스트, 린트, 정적 분석을 수행해 CRITICAL 이슈를 검출한다.",
        output_hint="실패 항목, 로그 요약, 재작업 필요 여부",
        owner="qa",
        lane="common",
        kind="gate",
        depends_on=("11_fe_codegen", "12_be_codegen", "13_devops_codegen"),
        loop_target_on_failure="11_fe_codegen",
    ),
    StageBlueprint(
        id="15_review_gate",
        title="Quality Review Gate",
        goal="보안, 상태 머신, 동시성, 회귀 리스크를 검토해 APPROVE/MINOR/MAJOR를 판정한다.",
        output_hint="리뷰 리포트, verdict, 재작업 지시사항",
        owner="qa",
        lane="common",
        kind="gate",
        depends_on=("14_static_analysis",),
        loop_target_on_failure="08_ticket_breakdown",
    ),
    StageBlueprint(
        id="16_pr_creation",
        title="Pull Request Creation",
        goal="내부 코드리뷰(Stage 15) 통과 후 lane별 GitHub PR을 생성하고 Jira 티켓을 In Progress로 전환한다. PR 생성은 lane별 worktree 브랜치에서 main을 대상으로 진행한다.",
        output_hint="lane별 PR URL, Jira 티켓 In Progress 전환 결과, PR 본문(변경 요약, 테스트 체크리스트)",
        owner="tech_lead",
        lane="common",
        kind="execution",
        depends_on=("15_review_gate",),
    ),
    StageBlueprint(
        id="17_post_merge_docs",
        title="Post-Merge Delivery Docs",
        goal="사용자의 PR 머지 완료 후 최종 기술 문서와 운영 문서를 Confluence에 게시하고 Jira 티켓을 Done으로 전환한다.",
        output_hint="Confluence 최종 운영 문서 URL, Jira Done 전환 결과, 릴리즈 노트",
        owner="pm",
        lane="common",
        kind="documentation",
        depends_on=("16_pr_creation",),
    ),
]

CONFLUENCE_TARGETS = (
    ConfluenceUpdateTarget(
        title="AI Wiki PRD",
        page_id_env="AI_ORCHESTRATOR_PRD_PAGE_ID",
        purpose="제품 요구사항과 주요 제약사항의 공식 문서",
    ),
    ConfluenceUpdateTarget(
        title="AI Wiki Roadmap",
        page_id_env="AI_ORCHESTRATOR_ROADMAP_PAGE_ID",
        purpose="릴리즈 계획과 우선순위 조정 이력",
    ),
    ConfluenceUpdateTarget(
        title="AI Wiki Requirements",
        page_id_env="AI_ORCHESTRATOR_REQUIREMENTS_PAGE_ID",
        purpose="기능/비기능 요구사항과 acceptance criteria 기준",
    ),
)

AGENT_SPECS = (
    AgentSpec(
        key="pm",
        role="시니어 프로덕트 매니저(PM)",
        goal="PRD, 요구사항, 로드맵을 최신 상태로 유지하고 Confluence를 Single Source of Truth로 관리한다.",
        backstory=(
            "당신은 비즈니스 가치와 기술 제약을 함께 다루는 PM입니다. "
            "문서 초안을 끝내지 않고, 공식 문서 시스템까지 갱신해야 완료로 간주합니다."
        ),
        llm="anthropic",
    ),
    AgentSpec(
        key="architect",
        role="시스템 아키텍트",
        goal="멀티 모듈 구조와 데이터 흐름, 통합 포인트, 운영 제약을 설계한다.",
        backstory=(
            "당신은 api, batch, worker, application, domain, infra 경계를 엄격하게 분리하고 "
            "단방향 의존성과 확장 가능한 구조를 선호하는 아키텍트입니다. "
            "특히 백엔드에서는 헥사고날 구조와 JPA Entity/도메인 POJO 분리를 강하게 요구합니다."
        ),
        llm="anthropic",
    ),
    AgentSpec(
        key="tech_lead",
        role="테크 리드",
        goal="병렬 구현 전략, 티켓 분해, 리스크와 복잡도 기반 우선순위 조정을 담당한다.",
        backstory=(
            "당신은 FE, BE, DevOps가 서로 막히지 않도록 인터페이스를 선제적으로 정의하고 "
            "worktree 기반 병렬 작업 계획을 수립하는 데 능숙합니다. "
            "특히 Pinpoint 구축 같은 cross-lane 작업은 별도 공동 티켓으로 분리합니다."
        ),
        llm="anthropic",
    ),
    AgentSpec(
        key="developer",
        role="자율형 시니어 엔지니어",
        goal="테스트 우선 설계와 lane별 코드 생성을 통해 구현 산출물을 만든다.",
        backstory=(
            "당신은 Kotlin Spring Boot 3.x, Next.js App Router, DevOps 자동화에 능숙하며 "
            "각 lane의 구현물을 독립 worktree에서 작업 가능한 단위로 산출합니다. "
            "백엔드에서는 객체지향, 클린 코드, 디자인 패턴, 헥사고날 아키텍처를 지키고 "
            "JPA Entity와 도메인 POJO를 분리합니다."
        ),
        llm="anthropic",
    ),
    AgentSpec(
        key="qa",
        role="품질 및 보안 감사관",
        goal="정적 분석, 보안 점검, 상태 머신과 동시성 정합성 검토를 통해 품질 게이트를 운영한다.",
        backstory=(
            "당신은 단순 리뷰어가 아니라 승인 권한을 가진 품질 게이트입니다. "
            "CRITICAL 또는 MAJOR 이슈가 있으면 반드시 이전 단계로 되돌립니다."
        ),
        llm="anthropic",
    ),
)

TASK_SPECS = (
    TaskSpec(
        key="ambiguity",
        stage_id="01_ambiguity",
        description=(
            "입력된 PRD에서 개발 착수 전 반드시 확인해야 할 모호성을 검출하세요. "
            "로직 모순, 데이터 정의 누락, 기술적 제약 사항을 확인하고 HIGH impact 항목을 우선 정리하세요."
        ),
        expected_output="모호성 질문 리스트와 기본 가정(JSON)",
        agent_key="pm",
        output_json_schema="AmbiguityItem[]",
        human_input=True,
    ),
    TaskSpec(
        key="requirements_analysis",
        stage_id="02_requirements_analysis",
        description=(
            "모호성 답변을 반영해 기능 요구사항, 비기능 요구사항, 제약사항, 성공 지표를 구조화하세요."
        ),
        expected_output="구조화된 요구사항 분석 문서",
        agent_key="pm",
        context_keys=("ambiguity",),
    ),
    TaskSpec(
        key="roadmap_update",
        stage_id="03_roadmap_update",
        description=(
            "요구사항 분석 결과를 바탕으로 PRD, 요구사항 문서, 릴리즈 로드맵을 최신화하세요."
        ),
        expected_output="최신 PRD, 요구사항, 로드맵 문서",
        agent_key="pm",
        context_keys=("requirements_analysis",),
    ),
    TaskSpec(
        key="confluence_sync",
        stage_id="04_confluence_sync",
        description=(
            "PRD, 요구사항, 로드맵 결과를 공식 Confluence 페이지에 반영하세요. "
            "게시 대상 페이지, 변경 요약, 게시 성공 여부를 기록하세요."
        ),
        expected_output="Confluence 게시 결과와 변경 로그",
        agent_key="pm",
        context_keys=("roadmap_update",),
    ),
    TaskSpec(
        key="technical_analysis",
        stage_id="05_technical_analysis",
        description=(
            "요구사항을 구현하기 위한 기술 스택 적합성, 통합 포인트, 외부 의존성, 운영 제약을 분석하세요."
        ),
        expected_output="기술 분석 문서",
        agent_key="architect",
        context_keys=("roadmap_update",),
    ),
    TaskSpec(
        key="architecture",
        stage_id="06_architecture",
        description=(
            "멀티 모듈 구조, 데이터 흐름, 시스템 경계, 인프라 의존성을 정의하세요."
        ),
        expected_output="아키텍처 보고서와 의존성 맵",
        agent_key="architect",
        context_keys=("technical_analysis",),
    ),
    TaskSpec(
        key="parallel_plan",
        stage_id="07_parallel_plan",
        description=(
            "FE, BE, DevOps lane을 기준으로 병렬 구현 전략을 세우고 worktree 생성 규칙과 병합 순서를 정의하세요. "
            "Pinpoint 모니터링 구축처럼 BE와 DevOps가 함께 수행해야 하는 cross-lane 작업도 식별하세요."
        ),
        expected_output="lane별 병렬 작업 계획",
        agent_key="tech_lead",
        context_keys=("architecture",),
    ),
    TaskSpec(
        key="ticket_breakdown",
        stage_id="08_ticket_breakdown",
        description=(
            "구현 티켓을 하나의 PR로 완결 가능한 단위로 나누고 id, lane, acceptance criteria, dependencies를 부여하세요. "
            "Pinpoint 구축은 BE와 DevOps 공동 작업 티켓으로 명시하세요. "
            "모든 티켓은 Jira 프로젝트 NAW에 등록 가능한 형태로 작성하세요. "
            "각 티켓에 risk_score, complexity_score, daily_capacity_cost, recommended_day_bucket을 함께 제안하세요."
        ),
        expected_output="구조화된 티켓 리스트(JSON)",
        agent_key="tech_lead",
        context_keys=("parallel_plan",),
        output_json_schema="Ticket[]",
    ),
    TaskSpec(
        key="risk_scoring",
        stage_id="09_risk_scoring",
        description=(
            "각 티켓의 리스크와 복잡도를 1~5로 평가하고 병렬 구현 순서를 재정렬하세요. "
            "또한 lane별 하루 capacity(be 4, fe 4, devops 3) 안에서 배치 가능한 일일 계획 기준을 제안하세요."
        ),
        expected_output="리스크/복잡도 스코어가 포함된 티켓 우선순위 문서",
        agent_key="tech_lead",
        context_keys=("ticket_breakdown",),
    ),
    TaskSpec(
        key="test_design",
        stage_id="10_test_design",
        description=(
            "티켓별 Given/When/Then 테스트 시나리오와 경계 조건을 설계하세요. "
            "FE, BE, DevOps 각각의 검증 포인트를 분리하세요."
        ),
        expected_output="티켓별 테스트 설계 문서",
        agent_key="developer",
        context_keys=("risk_scoring",),
    ),
    TaskSpec(
        key="fe_codegen",
        stage_id="11_fe_codegen",
        description=(
            "FE lane 티켓을 기준으로 Next.js App Router, TypeScript, Tailwind, React Query 코드를 생성하세요. "
            "SSE 상태 전이와 API client 연동을 포함하세요."
        ),
        expected_output="FE 코드 산출물과 변경 파일 목록",
        agent_key="developer",
        context_keys=("test_design",),
    ),
    TaskSpec(
        key="be_codegen",
        stage_id="12_be_codegen",
        description=(
            "BE lane 티켓을 기준으로 Kotlin Spring Boot 3.x 코드를 생성하세요. "
            "ai_status 상태 머신, 낙관적 잠금, analyze API 제약을 반영하세요. "
            "반드시 객체지향, 클린 코드, 디자인 패턴 원칙을 따르고 "
            "JPA Entity와 도메인 POJO를 분리하며, 헥사고날 아키텍처 구조로 작성하세요. "
            "또한 Pinpoint 에이전트 연동 지점과 운영에 필요한 백엔드 설정 포인트를 포함하세요. "
            "구현 방식 또는 기술 선택 논의가 필요하면 Confluence에 기술 문서를 생성하세요. "
            "문서 포맷은 개요, 목표, 설계(데이터 흐름, ERD, 컴포넌트 다이어그램, 플로우 다이어그램)을 따르세요."
        ),
        expected_output="BE 코드 산출물과 변경 파일 목록",
        agent_key="developer",
        context_keys=("test_design",),
    ),
    TaskSpec(
        key="devops_codegen",
        stage_id="13_devops_codegen",
        description=(
            "DevOps lane 티켓을 기준으로 Docker, CI/CD, 환경 변수, 배포 자동화 구성을 생성하세요. "
            "Pinpoint 서버/수집/운영 구성을 포함하고, BE lane과의 연동 조건을 문서화하세요. "
            "구현 방식 또는 기술 선택 논의가 필요하면 Confluence에 기술 문서를 생성하세요. "
            "문서 포맷은 개요, 목표, 설계(데이터 흐름, ERD, 컴포넌트 다이어그램, 플로우 다이어그램)을 따르세요."
        ),
        expected_output="DevOps 산출물과 운영 메모",
        agent_key="developer",
        context_keys=("test_design",),
    ),
    TaskSpec(
        key="static_analysis",
        stage_id="14_static_analysis",
        description=(
            "생성된 FE, BE, DevOps 산출물에 대해 빌드, 테스트, 린트, 정적 분석 결과를 정리하고 CRITICAL 이슈를 식별하세요."
        ),
        expected_output="정적 분석 리포트",
        agent_key="qa",
        context_keys=("fe_codegen", "be_codegen", "devops_codegen"),
    ),
    TaskSpec(
        key="review_gate",
        stage_id="15_review_gate",
        description=(
            "정적 분석 결과와 구현 산출물을 바탕으로 보안, 동시성, 상태 머신, 회귀 위험을 리뷰하고 "
            "APPROVE, MINOR, MAJOR 중 하나를 판정하세요."
        ),
        expected_output="최종 리뷰 리포트와 verdict",
        agent_key="qa",
        context_keys=("static_analysis",),
    ),
    TaskSpec(
        key="pr_creation",
        stage_id="16_pr_creation",
        description=(
            "내부 코드리뷰(Stage 15) APPROVE 이후 lane별 GitHub PR을 생성하세요. "
            "FE, BE, DevOps 각 lane에 대해 worktree 브랜치에서 main을 대상으로 PR을 만들고 "
            "PR 본문에는 변경 요약, 테스트 체크리스트, 관련 Jira 티켓 번호를 포함하세요. "
            "PR 생성 후 해당 Jira 티켓을 'In Progress' 상태로 전환하세요. "
            "PR URL과 Jira 전환 결과를 산출물로 기록하세요. "
            "⚠️ PR 머지는 사용자가 직접 수행합니다. 자동 머지 금지."
        ),
        expected_output="lane별 PR URL 목록, Jira In Progress 전환 결과",
        agent_key="tech_lead",
        context_keys=("review_gate",),
    ),
    TaskSpec(
        key="post_merge_docs",
        stage_id="17_post_merge_docs",
        description=(
            "PR 머지 완료 후 전체 결과물을 종합해 최종 기술 문서와 운영 문서를 Markdown으로 작성하고 "
            "Confluence에 릴리즈 노트를 게시하세요. "
            "완료된 Jira 티켓을 'Done' 상태로 전환하세요. "
            "⚠️ 이 단계는 사용자가 PR을 머지한 후에 실행합니다."
        ),
        expected_output="최종 기술 문서, Confluence 릴리즈 노트 URL, Jira Done 전환 결과",
        agent_key="pm",
        context_keys=("roadmap_update", "architecture", "review_gate", "pr_creation"),
    ),
)

LOOP_POLICIES = (
    LoopPolicy(
        trigger_stage_id="14_static_analysis",
        condition="CRITICAL 또는 빌드/테스트 실패",
        target_stage_id="11_fe_codegen",
        max_retries=3,
        note="분석 결과를 기준으로 FE/BE/DevOps 코드 생성 단계를 재실행한다.",
    ),
    LoopPolicy(
        trigger_stage_id="15_review_gate",
        condition="verdict == MAJOR",
        target_stage_id="08_ticket_breakdown",
        max_retries=2,
        note="리뷰에서 구조적 문제가 확인되면 티켓 분해부터 다시 시작한다.",
    ),
)


@dataclass(frozen=True)
class RunContext:
    project_root: Path
    prd_path: Path
    config: RuntimeConfig


def create_run_directory(output_root: Path) -> Path:
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    run_dir = output_root / timestamp
    run_dir.mkdir(parents=True, exist_ok=False)
    return run_dir


def build_live_definition() -> LivePipelineDefinition:
    return LivePipelineDefinition(
        agents=AGENT_SPECS,
        tasks=TASK_SPECS,
        loop_policies=LOOP_POLICIES,
    )


def _write_stage_placeholder(stage_file: Path, blueprint: StageBlueprint, prd_text: str) -> None:
    stage_file.write_text(
        "\n".join(
            [
                f"# {blueprint.title}",
                "",
                "## Goal",
                blueprint.goal,
                "",
                "## Output Hint",
                blueprint.output_hint,
                "",
                "## Owner",
                blueprint.owner,
                "",
                "## Lane",
                blueprint.lane,
                "",
                "## Kind",
                blueprint.kind,
                "",
                "## Depends On",
                ", ".join(blueprint.depends_on) if blueprint.depends_on else "-",
                "",
                "## Loop Target On Failure",
                blueprint.loop_target_on_failure or "-",
                "",
                "## Input Snapshot",
                prd_text[:2000],
                "",
                "## Dry Run Note",
                "This file is a placeholder artifact. Replace this section with live LLM output.",
            ]
        ),
        encoding="utf-8",
    )


def run_dry_pipeline(context: RunContext) -> Path:
    prd_text = context.prd_path.read_text(encoding="utf-8")
    run_dir = create_run_directory(context.config.output_root)

    manifest = {
        "prd_path": str(context.prd_path),
        "run_dir": str(run_dir),
        "confluence_targets": [target.__dict__ for target in CONFLUENCE_TARGETS],
        "stages": [],
        "loop_policies": [policy.__dict__ for policy in LOOP_POLICIES],
    }

    for blueprint in STAGE_BLUEPRINTS:
        stage_file = run_dir / f"{blueprint.id}.md"
        artifact = StageArtifact(
            stage_id=blueprint.id,
            title=blueprint.title,
            owner=blueprint.owner,
            lane=blueprint.lane,
            kind=blueprint.kind,
            summary=f"{blueprint.goal} Dry-run placeholder generated from input PRD.",
            output_path=str(stage_file),
            next_input_hint=f"Read {blueprint.id}.md before executing the next stage.",
        )
        _write_stage_placeholder(stage_file, blueprint, prd_text)
        manifest["stages"].append(artifact.to_dict())

    (run_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return run_dir


def validate_sdk_environment() -> None:
    try:
        import claude_agent_sdk  # noqa: F401
        import anyio  # noqa: F401
    except ImportError as exc:
        raise RuntimeError(
            "live mode requires claude-agent-sdk. Install with `pip install -e .[live]`."
        ) from exc


def _agent_tools(agent_key: str) -> list[str]:
    if agent_key == "qa":
        return ["Read", "Bash", "Glob", "Grep"]
    if agent_key == "developer":
        return ["Read", "Write", "Edit", "Bash", "Glob", "Grep"]
    return ["Read", "Write", "Edit"]


def build_agent_definitions() -> dict:
    validate_sdk_environment()
    from claude_agent_sdk import AgentDefinition

    return {
        spec.key: AgentDefinition(
            description=spec.role,
            prompt="\n".join([
                f"역할: {spec.role}",
                f"목표: {spec.goal}",
                f"배경: {spec.backstory}",
            ]),
            tools=_agent_tools(spec.key),
        )
        for spec in AGENT_SPECS
    }


def build_orchestrator_prompt(prd_text: str, run_dir: Path) -> str:
    stage_list = "\n".join(
        f"  {b.id}: {b.title} (owner={b.owner}, lane={b.lane})"
        + (f" → depends: {', '.join(b.depends_on)}" if b.depends_on else "")
        for b in STAGE_BLUEPRINTS
    )

    task_details = "\n\n".join(
        f"### {spec.stage_id}\n"
        f"에이전트: {spec.agent_key}\n"
        f"작업: {spec.description}\n"
        f"출력: {spec.expected_output}"
        + (f"\n참조 단계: {', '.join(spec.context_keys)}" if spec.context_keys else "")
        for spec in TASK_SPECS
    )

    return f"""당신은 PRD를 소프트웨어 명세로 변환하는 16단계 파이프라인의 오케스트레이터입니다.

## 작업 디렉토리
{run_dir}

## PRD 입력
{prd_text}

## 파이프라인 단계 목록
{stage_list}

## 단계별 상세 작업
{task_details}

## 실행 규칙

### 순차 실행
각 단계를 순서대로 Agent 도구로 해당 에이전트에게 위임하세요.
이전 단계의 출력 내용을 다음 단계의 프롬프트에 포함해 컨텍스트를 전달하세요.

### 병렬 실행 (11~13단계)
10_test_design 완료 후 11_fe_codegen, 12_be_codegen, 13_devops_codegen은 동시에 실행 가능합니다.

### 단계 출력 저장
각 단계 완료 후 결과를 `{run_dir}/{{stage_id}}.md` 파일에 저장하세요.
예: 01_ambiguity 완료 → `{run_dir}/01_ambiguity.md`

### 루프 정책
- 14_static_analysis에서 CRITICAL 또는 빌드/테스트 실패 시: 11_fe_codegen부터 재시작 (최대 3회)
- 15_review_gate에서 verdict = MAJOR 판정 시: 08_ticket_breakdown부터 재시작 (최대 2회)

지금 01_ambiguity 단계부터 시작하세요."""


def build_live_pipeline(context: RunContext) -> str:
    validate_sdk_environment()
    definition = build_live_definition()
    summary = {
        "sdk": "claude-agent-sdk",
        "task_count": len(definition.tasks),
        "agent_count": len(definition.agents),
        "loop_policy_count": len(definition.loop_policies),
        "confluence_targets": [target.title for target in CONFLUENCE_TARGETS],
        "agents": [
            {"key": spec.key, "role": spec.role, "tools": _agent_tools(spec.key)}
            for spec in AGENT_SPECS
        ],
    }
    return json.dumps(summary, ensure_ascii=False, indent=2)
