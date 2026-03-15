# AI Orchestrator Lab

`wiki/ai_orchestration`에서 출발한 실험용 오케스트레이션 프로젝트입니다.
단일 `pipeline.py` 초안 대신, 아래 목표를 가진 독립 작업 디렉토리로 재구성했습니다.

- `ai-orchestrator-lab` 자체를 루트 작업공간으로 사용
- `fe`, `be`, `devops` 작업을 분리하고 병렬 진행 가능하게 설계
- PRD 기반 산출물 생성 흐름을 단계별로 명시
- `dry-run`으로 구조와 출력물 계약을 먼저 검증
- 실제 LLM 연동 시 필요한 설정 포인트를 코드와 문서로 분리
- 피드백 루프와 품질 게이트를 별도 단계로 모델링

## 디렉토리 구조

```text
ai-orchestrator-lab/
├── be/
│   ├── pinpoint/
│   │   └── README.md
│   └── README.md
├── devops/
│   ├── pinpoint/
│   │   ├── .env.example
│   │   ├── docker-compose.yml
│   │   └── README.md
│   └── README.md
├── docs/
│   ├── agent_assignment.md
│   ├── daily_capacity.md
│   ├── orchestration_design.md
│   ├── ticket_lifecycle.md
│   └── tech_doc_template.md
├── fe/
│   └── README.md
├── input/
│   └── prd.md
├── src/
│   └── ai_orchestrator_lab/
│       ├── cli.py
│       ├── config.py
│       ├── models.py
│       └── pipeline.py
├── worktrees/
│   └── README.md
├── .env.example
└── pyproject.toml
```

## 핵심 개선점

### 0. 루트 작업공간 통합
- `ai-orchestrator-lab`를 실험 루트로 사용
- FE, BE, DevOps 작업 디렉토리를 루트 아래에서 함께 관리
- 병렬 작업용 git worktree도 동일 루트 아래 `worktrees/`에 배치

### 1. 실행 모드 분리
- `dry-run`: LLM 호출 없이 단계별 아웃풋 파일을 생성
- `live`: Claude Team (TaskCreate + Agent) 기반으로 팀원별 역할 분리 실행

### 2. 출력물 계약 고정
- 모든 실행은 `outputs/<timestamp>/` 아래에 결과를 생성
- 단계별 파일명 규칙을 고정해 후처리 자동화에 유리하게 구성

### 3. 설정 분리
- 모델명, 출력 디렉토리, 재시도 정책을 `config.py`에서 관리
- API 키는 `.env` 기준으로 읽도록 설계

### 4. PM의 Confluence 책임 명시
- PM lane은 PRD 초안만 만드는 것이 아니라 Confluence 페이지까지 직접 업데이트
- 로드맵, 요구사항, 최종 문서가 Confluence 동기화 대상
- 현재 예시 설정은 PRD와 Requirements를 동일한 Confluence 페이지로 관리

### 5. Jira 작업 기준
- 구현 티켓과 진행 관리는 Jira 프로젝트 `NAW` 기준으로 수행
- 기본 보드는 `34`
- Confluence와 동일하게 Atlassian 사이트 `biuea3866.atlassian.net` 기준으로 사용

### 6. 품질 게이트와 피드백 루프
- `static analysis gate`에서 빌드/테스트/린트 실패를 검출
- `review gate`에서 `APPROVE`, `MINOR`, `MAJOR` 판정을 내림
- `CRITICAL` 실패 시 codegen 재시도, `MAJOR` 시 티켓 분해 단계로 되돌림

### 7. 일일 작업량 기준
- 하루 작업량은 시간보다 `티켓 완결 단위`로 관리
- lane별 `complexity_score` 합계로 일일 capacity를 제한
- 기본 기준은 `be 4`, `fe 4`, `devops 3`, `cross-lane 1개`
- 자세한 규칙은 [`daily_capacity.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/daily_capacity.md) 참고

### 8. 티켓 담당 방식
- 티켓마다 `owner_role`을 하나 둠
- owner가 티켓을 end-to-end로 책임짐
- 리뷰와 승인은 별도 `reviewer_role`이 담당
- 자세한 규칙은 [`agent_assignment.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/agent_assignment.md) 참고

### 9. 티켓 수명주기
- 모든 구현은 티켓 단위 브랜치로 진행
- PR이 열리면 Jira를 `진행 중`으로 전환
- PR이 머지되면 Jira를 `완료`로 전환
- 자세한 규칙은 [`ticket_lifecycle.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/ticket_lifecycle.md) 참고

## 워크플로 원칙

### 병렬 작업 lane
- `fe/`: 화면, UX, SSE, API client
- `be/`: API, 도메인, AI 파이프라인 서버
- `devops/`: CI/CD, Docker, 배포, 환경 관리

### BE + DevOps 공동 항목
- Pinpoint 기반 애플리케이션 모니터링 시스템 구축
- BE는 애플리케이션 연동 포인트를 제공하고, DevOps는 수집/운영 환경을 구성
- 병렬 계획과 티켓 분해 단계에서 반드시 공동 작업 티켓으로 추출
- 초기 스캐폴드는 [`be/pinpoint/README.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/be/pinpoint/README.md), [`devops/pinpoint/README.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/devops/pinpoint/README.md), [`docker-compose.yml`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/devops/pinpoint/docker-compose.yml) 에 준비

### BE 구현 규칙
- 객체지향, 클린 코드, 디자인 패턴 기반으로 작성
- JPA Entity와 도메인 POJO를 분리
- 헥사고날 아키텍처를 기본 구조로 사용

### 병렬 실행 방식
- 각 lane은 별도 git worktree를 생성해 동시에 진행 가능
- PM/Architect 단계에서 공통 설계를 먼저 정리한 뒤 FE, BE, DevOps lane으로 분기
- QA 단계에서 다시 공통 검토로 수렴
- 하루 계획은 lane별 capacity를 넘기지 않도록 티켓을 배치

### PM 운영 규칙
- PM은 PRD, 요구사항, 로드맵을 로컬 파일로만 남기지 않음
- 오케스트레이션의 공식 산출물은 Confluence 반영까지 완료되어야 함
- 따라서 파이프라인에 `Confluence Sync` 단계가 포함됨

### BE / DevOps 기술 문서 규칙
- `be`, `devops` 엔지니어는 구현 중 기술 검토가 필요하면 Confluence에 기술 문서를 직접 생성해 논의
- 기술 문서는 고정 포맷을 따름: `개요`, `목표`, `설계`
- `설계`에는 반드시 `데이터 흐름`, `ERD`, `컴포넌트 다이어그램`, `플로우 다이어그램` 포함
- 기본 템플릿은 [`tech_doc_template.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/tech_doc_template.md)

## 현재 파이프라인 단계

1. `01_ambiguity`
2. `02_requirements_analysis`
3. `03_roadmap_update`
4. `04_confluence_sync`
5. `05_technical_analysis`
6. `06_architecture`
7. `07_parallel_plan`
8. `08_ticket_breakdown`
   이 단계에서 생성된 티켓은 Jira `NAW` 프로젝트 등록 대상입니다.
9. `09_risk_scoring`
10. `10_test_design`
11. `11_fe_codegen`
12. `12_be_codegen`
13. `13_devops_codegen`
14. `14_static_analysis`
15. `15_review_gate`
16. `16_docs`

## 빠른 시작

### 1. 가상환경 생성

```bash
cd ai-orchestrator-lab
python3 -m venv .venv
source .venv/bin/activate
```

### 2. Dry Run 실행

```bash
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli \
  dry-run \
  --prd input/prd.md
```

실행이 끝나면 `outputs/<timestamp>/` 아래에 단계별 초안 파일이 생성됩니다.

## Claude Team 파이프라인 실행

`team_pipeline.py`를 통해 Claude Team (pm / be-developer / fe-developer / orchestrator-dev) 기반으로 16단계를 실행합니다.

파이프라인 구조 확인:

```bash
.venv/bin/python3 src/ai_orchestrator_lab/team_pipeline.py \
  --prd input/prd.md \
  --format summary
```

각 스테이지는 로컬 마크다운 산출물과 함께 Confluence 문서 / Jira 티켓을 실제로 생성합니다.
- Stage 04: PM → Confluence PRD/요구사항/로드맵 페이지 업데이트
- Stage 08: orchestrator-dev → Jira NAW 프로젝트 티켓 일괄 생성
- Stage 11/12/13: 각 lane → 코딩 전 Confluence 기술 설계 문서 작성
- Stage 16: PM → 최종 운영 문서 Confluence 게시 + Jira 티켓 Done 전환

`live-run` 명령으로 스테이지별 이벤트 기록 확인:

```bash
.venv/bin/python3 src/ai_orchestrator_lab/cli.py \
  run-status \
  --prd input/prd.md \
  --run-dir outputs/<timestamp>
```

## 실제 제품 코드 루트

- [`fe`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/fe): AI Wiki 프론트엔드 앱 루트
- [`be`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/be): AI Wiki 백엔드 앱 루트
- [`devops`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/devops): 운영 및 배포 루트

이제부터 실제 제품 구현은 위 세 디렉토리에서 진행합니다.

## Live 실행 준비

`.env.example`을 복사해 `.env`를 만들고 필요한 키를 채운 뒤:

```bash
pip install -e .[live]
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli \
  live-run \
  --prd input/prd.md
```

현재 `live` 모드는 진입점과 검증 로직까지 포함합니다.
`build_live_pipeline()`은 CrewAI `Agent`, `Task`, `Crew`를 조립하고, 루프 정책과 Confluence 대상 정보를 별도 메타데이터로 반환합니다.
실제 재시도 루프와 Confluence API 호출은 이 Crew를 감싸는 런타임 컨트롤러에서 처리해야 합니다.

### 런타임 명령 예시

```bash
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli dry-run --prd input/prd.md
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli live-run --prd input/prd.md
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli run-status --prd input/prd.md --run-dir outputs/<timestamp>
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli sync-confluence --prd input/prd.md --run-dir outputs/<timestamp>
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli write-ticket-template --prd input/prd.md
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli create-jira --prd input/prd.md --tickets-json outputs/<timestamp>/08_ticket_breakdown.sample.json
PYTHONPATH=src python3 -m ai_orchestrator_lab.cli create-tech-doc --prd input/prd.md --lane be --title "Pinpoint Agent Injection" --source docs/tech_doc_template.md
```

실제 Atlassian 연동 명령을 실행하려면 아래 값이 필요합니다.

- `AI_ORCHESTRATOR_ATLASSIAN_EMAIL`
- `AI_ORCHESTRATOR_ATLASSIAN_API_TOKEN`
- `AI_ORCHESTRATOR_CONFLUENCE_SITE`
- `AI_ORCHESTRATOR_JIRA_SITE`

기술 문서를 특정 상위 페이지 아래에 만들고 싶으면 `AI_ORCHESTRATOR_TECH_DOC_PARENT_PAGE_ID`를 설정합니다.

## 추천 다음 작업

1. `build_live_pipeline()` 바깥에 런타임 컨트롤러를 추가해 `loop_policies`를 실제 재시도로 연결
2. PM의 `Confluence Sync` 단계를 Atlassian API 호출과 연결
3. `08_ticket_breakdown` 결과를 Jira `NAW` 프로젝트 이슈 생성과 연결
