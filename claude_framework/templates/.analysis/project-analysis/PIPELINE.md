# 프로젝트 분석 파이프라인

## 목적
정제된 PRD를 기반으로 "팀이 바로 착수할 수 있는 수준"의 설계/TDD/티켓을 한 번에 만든다. 이 산출물만 있으면 구현 에이전트(be-implementer, fe-implementer)가 독립 작업 가능해야 한다.

## 담당 에이전트 & 스킬

| 역할 | 에이전트 | 스킬 | 참조 |
|---|---|---|---|
| 오케스트레이션 | `pipeline-runner` | — | `.claude/agents/pipeline-runner.md` |
| 1~7단계 설계/TDD | `project-analyst` | `project-analysis-flow`, `mermaid-diagrams` | `.claude/agents/project-analyst.md` |
| 8단계 티켓 분해 | `ticket-splitter` | `ticket-breakdown` | `.claude/agents/ticket-splitter.md` |
| (구현) BE | `be-implementer` | `tdd-loop` | `.claude/agents/be-implementer.md` |
| (구현) FE | `fe-implementer` | `tdd-loop` | `.claude/agents/fe-implementer.md` |
| (구현 후) PR 리뷰 | `pr-reviewer` | `pr-review-checklist` | `.claude/agents/pr-reviewer.md` |
| (주기) 룰 감사 | `harness-auditor` | `harness-audit` | `.claude/agents/harness-auditor.md` |

**관계**: 파이프라인이 순서와 산출물 스펙을 정의 → 에이전트가 실행 주체(페르소나+권한) → 스킬이 각 단계의 절차/템플릿/체크리스트를 제공.

호출 방식:
```
Agent(subagent_type="pipeline-runner", prompt="project-analysis 파이프라인 실행: <prd 산출물 경로>")
# 내부 흐름:
#   1~7단계 → project-analyst (project-analysis-flow + mermaid-diagrams 스킬 활용)
#   8단계   → ticket-splitter (ticket-breakdown 스킬 활용)
```

## 입력 (필수)
- **PRD 분석 산출물** — `.analysis/prd/YYYY-MM-DD-<slug>.md` (Exit Criteria 통과본)
- **대상 레포 목록** — `be-repos/`, `fe-repos/`, `offercent-repos/` 아래 어느 것인지
- **우선순위/데드라인**

## 단계별 상세

### 1. Background
- 비즈니스 맥락 요약 (왜 지금 이걸 하는가)
- 관련 OKR/KPI
- 기존 시스템과의 관계

### 2. Terminology
- 도메인 용어 사전 (한/영 병기)
- BC(Bounded Context) 경계 명확화
- 약어 금지 정책 준수 (`workspaceId` ✓, `ws` ✗)

### 3. Define Problem
- 구체적인 문제 정의 (사용자 시나리오 포함)
- 현재 시스템의 한계
- 성공 지표 (측정 가능)

### 4. Possible Solutions (최소 2개 필수)
- 각 대안별:
  - 개요
  - 장단점
  - 기술적 비용 (L/M/H)
  - 리스크
- 선택 근거 명시 (왜 이 대안이 다른 대안보다 나은가)

### 5. Detail Design

#### 5.1 Component Diagram (Mermaid)
- AS-IS — 현재 아키텍처
- TO-BE — 변경 후 아키텍처
- 추가되는/제거되는 컴포넌트 강조

#### 5.2 Sequence Diagram (Mermaid)
- 주요 유스케이스 2-3개
- 동기/비동기, Kafka 이벤트 포함

#### 5.3 ERD (Mermaid)
- 새 테이블/컬럼
- 관계 (단 FK는 애플리케이션 레벨 관리, DB FK 금지)
- 인덱스 전략

#### 5.4 API 스펙
- 표 또는 OpenAPI
- Request/Response 스키마
- 에러 코드 목록

#### 5.5 Kafka/이벤트 계약 (필요 시)
- 토픽명 (`event.<project>.<domain>` 형식)
- Avro/JSON 스키마
- 파티셔닝 키

### 6. Security Information
- 인증/인가 (어떤 권한이 필요한가)
- 데이터 보호 (PII 처리, 암호화, 마스킹)
- Rate Limiting / Abuse 방지
- Audit 로깅

### 7. TDD 전략
**이 섹션은 be-implementer/fe-implementer가 그대로 참조한다. 빈 껍데기 절대 금지.**

- 테스트 케이스 목록 (Given/When/Then)
- 단위/통합/E2E 구분
- 필요한 Testcontainers (MySQL/Redis/Kafka)
- 커버리지 목표 (기본 80%, FE/BE 중요 플로우는 95%)
- Mocking 범위와 실제 인프라 사용 경계

예시:
```
### TC-01 (FR-01): 공고 생성 성공
- Type: 통합 (Testcontainers: MySQL)
- Given: 인증된 사용자, 유효한 공고 입력
- When: POST /api/postings
- Then: 201 + DB에 저장 + Kafka event.closet.posting 발행
```

### 8. Ticket Breakdown (ticket-splitter가 수행)
- "1명/1일/1PR" 단위
- 종속성 명시
- 각 티켓에 대응 TC 번호 링크
- 포맷은 `.claude/agents/ticket-splitter.md` 참조

## 산출물 구조

폴더 경로: `.analysis/project-analysis/YYYY-MM-DD-<feature>/`

```
YYYY-MM-DD-<feature>/
├── 00-overview.md      # Background, Terminology, Problem, Possible Solutions
├── 01-design.md        # Detail Design (5.1~5.5) + Security
├── 02-tdd.md           # TDD 전략 + 테스트 케이스 목록
└── 03-tickets.md       # 티켓 분해 (ticket-splitter 산출)
```

각 파일 헤더에 PRD 원본 경로 + 분석 일자 + 담당자 명시.

## Exit Criteria
- [ ] 4개 파일 모두 존재하며 빈 섹션 없음
- [ ] Possible Solutions 최소 2개 비교 + 선택 근거 명확
- [ ] Mermaid 다이어그램 3종(Component AS-IS/TO-BE, Sequence, ERD) 포함
- [ ] 모든 FR/NFR이 최소 1개 TC에 연결됨
- [ ] 티켓 각각에 대응 TC 링크 + 1일 내 크기
- [ ] `harness-rules.json` 위반 가능성 있는 설계 요소 검토 완료 (예: DB FK, LocalDateTime 등)

Exit Criteria 미충족 시 pipeline-runner가 재작업 지시.

## 다음 단계
- 구현: `be-implementer` / `fe-implementer`가 `03-tickets.md`의 티켓 단위로 작업
- PR 리뷰: `pr-reviewer`가 `harness-rules.json` 기준 검수
- 주기 감사: `harness-auditor`가 전체 레포 스캔
