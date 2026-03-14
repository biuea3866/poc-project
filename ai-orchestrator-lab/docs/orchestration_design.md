# Orchestration Design

## 목적

이 프로젝트는 "PRD 하나를 입력하면 질문 정리, 아키텍처 초안, 티켓 초안, 리뷰 포인트까지 단계적으로 생성하는 흐름"을 빠르게 실험하기 위한 랩입니다.

`wiki/ai_orchestration/pipeline.py`는 아이디어를 한 파일에 압축해 둔 상태였습니다.
이 랩에서는 다음을 분리합니다.

- 입력 계약: PRD 원문
- 실행 계약: dry-run / live
- 산출물 계약: 단계별 파일 규칙
- 확장 지점: Claude Team (pm / be-developer / fe-developer / orchestrator-dev) 기반 실행
- 병렬 lane 계약: fe / be / devops
- PM 운영 계약: Confluence 직접 업데이트
- 티켓 운영 계약: Jira `NAW` 프로젝트와 board `34` 사용

## 작업 루트

`ai-orchestrator-lab` 자체를 하나의 작업 루트로 사용합니다. 실제 제품 구현도 이 루트 안에서만 진행합니다.

- `fe/`: 프론트엔드 구현
- `be/`: 백엔드 구현
- `devops/`: 인프라 및 운영 자동화
- `worktrees/`: 병렬 브랜치 작업공간

기존 외부 프로젝트 디렉토리를 구현 기준으로 삼지 않습니다. `worktrees/` 또한 `ai-orchestrator-lab` 내부 `fe`, `be`, `devops`를 기준으로 분기합니다.

백엔드 lane은 아래 구현 규칙을 기본 전제로 합니다.

- 객체지향, 클린 코드, 디자인 패턴 기반
- JPA Entity와 도메인 POJO 분리
- 헥사고날 아키텍처 준수

추가로 `be`와 `devops`는 공동으로 Pinpoint 모니터링 시스템을 구축합니다.

- `be`: 애플리케이션 연동 포인트, 에이전트 설정 방식, 추적 범위 정의
- `devops`: Pinpoint 인프라, 배포 구성, 운영 체크리스트 정의

이 구조는 "하나의 PRD에서 여러 lane이 동시에 구현을 진행하되, 최종적으로 하나의 운영 문서 체계로 수렴"하는 워크플로를 전제로 합니다.

## 단계 정의

1. `01_ambiguity`
2. `02_requirements_analysis`
3. `03_roadmap_update`
4. `04_confluence_sync`
5. `05_technical_analysis`
6. `06_architecture`
7. `07_parallel_plan`
8. `08_ticket_breakdown`
9. `09_risk_scoring`
10. `10_test_design`
11. `11_fe_codegen`
12. `12_be_codegen`
13. `13_devops_codegen`
14. `14_static_analysis`
15. `15_review_gate`
16. `16_docs`

각 단계는 아래 메타데이터를 가집니다.

- `id`: 출력 디렉토리와 파일명 prefix
- `title`: 사람이 읽는 단계 이름
- `goal`: 해당 단계의 목적
- `output_hint`: 기대 산출물 형식
- `owner`: 주 책임자
- `lane`: `common`, `fe`, `be`, `devops`

티켓 모델은 일일 계획 산정을 위해 아래 값을 함께 가집니다.

- `owner_role`
- `reviewer_role`
- 티켓 단위 브랜치명
- PR 상태와 Jira 상태 동기화 규칙
- `risk_score`
- `complexity_score`
- `daily_capacity_cost`
- `cross_lane`
- `recommended_day_bucket`

티켓 담당 규칙은 [`agent_assignment.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/agent_assignment.md) 를 따른다.

## 병렬 Worktree 전략

병렬 구현은 git worktree를 기준으로 합니다.

- 공통 설계 단계: `common`
- 구현 단계: `fe`, `be`, `devops`
- 검토/문서화 단계: 다시 `common`

권장 규칙:

- `worktrees/fe-*`: 프론트엔드 전용 브랜치
- `worktrees/be-*`: 백엔드 전용 브랜치
- `worktrees/devops-*`: 인프라 전용 브랜치
- lane 간 인터페이스 변경은 `05_parallel_plan`과 `06_tickets`에서 먼저 명시
- Pinpoint 구축처럼 lane 간 공동 작업은 별도 cross-lane 티켓으로 관리

## PM과 Confluence

이 프로젝트에서 PM은 산출물 작성자이면서 문서 시스템 관리자 역할도 맡습니다.

- `03_roadmap_update`: 로컬 기준 PRD, 로드맵, 요구사항 초안 생성
- `04_confluence_sync`: Confluence 페이지 직접 갱신
- `16_docs`: 최종 운영 문서와 게시 체크리스트 정리

즉, PM 단계의 완료 기준은 "문서 생성"이 아니라 "Confluence 반영 완료"입니다.
현재 기본 예시에서는 PRD 페이지와 Requirements 페이지를 동일한 Confluence page id로 관리합니다.

## BE / DevOps 기술 문서 규칙

`be`와 `devops` 엔지니어는 구현 과정에서 기술 구현 방식이나 기술 선택을 논의해야 하면 Confluence에 별도 기술 문서를 생성합니다.

- 문서는 필요 시점에 생성합니다.
- 로컬 메모가 아니라 Confluence 문서를 기준으로 의사결정합니다.
- 기본 포맷은 [`tech_doc_template.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/tech_doc_template.md) 를 따릅니다.

필수 섹션:

- `개요`
- `목표`
- `설계`

`설계` 필수 하위 항목:

- `데이터 흐름`
- `ERD`
- `컴포넌트 다이어그램`
- `플로우 다이어그램`

## Jira 작업 기준

- Jira site: `biuea3866.atlassian.net`
- Jira project key: `NAW`
- Jira board id: `34`

`08_ticket_breakdown` 단계의 산출물은 최종적으로 Jira `NAW` 프로젝트에 등록되는 티켓 초안입니다.

실제 자동 등록/자동 갱신을 위해서는 Atlassian email + API token 기반 인증이 필요합니다.

## 일일 작업량 산정

하루 작업량은 시간 추정이 아니라 티켓 완결 단위로 산정합니다.

- `be`: 하루 `complexity_score` 총합 4 이하
- `fe`: 하루 `complexity_score` 총합 4 이하
- `devops`: 하루 `complexity_score` 총합 3 이하
- `cross-lane`: 하루 1개

보다 자세한 기준은 [`daily_capacity.md`](/Users/biuea/feature/flag_project/ai-orchestrator-lab/docs/daily_capacity.md) 에 정리합니다.

## 피드백 루프

이미지의 빨간 점선은 설계상 핵심입니다. 따라서 live 파이프라인은 아래 루프를 반드시 가져야 합니다.

- `14_static_analysis`에서 `CRITICAL` 또는 빌드/테스트 실패 발생
  다음 동작: `11_fe_codegen`, `12_be_codegen`, `13_devops_codegen` 재실행
  최대 재시도: 3회
- `15_review_gate`에서 `MAJOR` 판정 발생
  다음 동작: `08_ticket_breakdown`부터 재실행
  최대 재시도: 2회

`MINOR`는 수정 메모를 남기고 진행 가능하지만, `MAJOR`는 병렬 구현 전략 자체를 다시 조정해야 합니다.

## Dry Run 철학

오케스트레이션 설계에서 먼저 검증해야 하는 것은 LLM 품질보다도 "흐름이 깨지지 않는가"입니다.
그래서 `dry-run`은 실제 모델 호출 없이도 아래를 확인하게 합니다.

- 입력 PRD를 정상적으로 읽는가
- 단계 순서가 올바른가
- 출력 디렉토리 규칙이 유지되는가
- 후속 자동화가 읽을 수 있는 최소 메타데이터가 생성되는가

## CrewAI 관측

현재 런타임은 아래 관측 파일을 생성합니다.

- `live_runtime_definition.json`: 실행에 사용된 파이프라인 정의
- `run_status.json`: 현재 run 상태와 stage 목록
- `events.ndjson`: run 수준 이벤트 로그

운영자는 `run-status` CLI로 현재 상태를 조회할 수 있습니다.
다만 현재 관측 수준은 `run-level` 이므로, CrewAI kickoff 내부 stage별 세부 시작/종료 시각은 아직 실시간 추적되지 않습니다.

## Live 확장 원칙

`live` 모드에서는 아래 구조를 권장합니다.

- PM 에이전트: 모호성 탐지, PRD 보강, Confluence 동기화, 최종 문서화
- Architect 에이전트: 기술 분석, 멀티 모듈 구조와 의존성 맵 설계
- Tech Lead 에이전트: 병렬 lane 계획, 티켓 분해, 리스크/복잡도 스코어링
- Developer 에이전트: 테스트 설계, FE/BE/DevOps lane 코드 생성
- QA/Security 에이전트: 보안/동시성/회귀 검토

추가 규칙:

- `be`와 `devops` lane은 구현 중 기술 논의가 필요하면 Confluence 기술 문서를 생성해야 합니다.
- 기술 문서 포맷은 `개요`, `목표`, `설계(데이터 흐름, ERD, 컴포넌트 다이어그램, 플로우 다이어그램)`를 반드시 포함해야 합니다.

각 단계 산출물은 이전 단계 파일을 입력으로 받도록 구성하고, 중간 결과를 다시 파일로 남겨야 합니다.

## 권장 후속 작업

1. `models.py`에 lane별 티켓 및 Confluence 게시 결과 스키마 추가
2. `pipeline.py` 바깥에서 loop policy를 실행하는 런타임 컨트롤러 추가
3. PM 단계에서 Atlassian Confluence 업데이트와 Jira `NAW` 이슈 생성을 수행하는 live task 추가
