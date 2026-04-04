# BE 리팩토링 / 기술 부채 파이프라인

PRD 없이 개발자가 시작하는 기술 개선 작업을 수행한다.

## 공통 가이드 참조
> 아래 공통 규칙을 따릅니다. 파이프라인 특화 규칙은 이 문서에서 정의합니다.

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
- [티켓 작성법](../common/ticket-guide.md)
- [Jira 동기화](../common/jira-sync.md)
- [TDD 작성법](../common/tdd-template.md)
- [문서 싱크 체계](../common/document-sync.md)

## project-analysis와의 차이

| | project-analysis | be-refactoring |
|---|---|---|
| **시작점** | PRD (PM 요청) | 기술 결정 (개발자 판단) |
| **Phase 1** | PRD 분석 (기능/비기능/모호성) | 현황 분석 (코드/아키텍처/성능) |
| **벤치마킹** | 경쟁사/참조 SaaS | 기술 패턴/오픈소스/레퍼런스 |
| **산출물 위치** | `results/{날짜}_{기능명}/prd/` + `be/` | `results/{날짜}_{대상}/analysis/` + `plan/` |

## 실행 원칙

- Phase 1(현황 분석) → Phase 2(개선 계획) → Phase 3(구현) 순서
- 추측 금지. 반드시 실제 코드를 읽고 분석
- **변경 전후 비교 필수**: AS-IS 메트릭(코드량, 복잡도, 테스트 커버리지) → TO-BE 목표
- **위험 최소화**: 기존 동작 보존. 리팩토링은 동작 변경 없이 구조만 개선

## 입력 유형

| 유형 | 예시 |
|------|------|
| 기술 스택 전환 | Node.js → Kotlin, MongoDB → MySQL |
| 레거시 정리 | 이벤트 클래스 폭발 → 통합 패턴, deprecated 코드 제거 |
| 성능 개선 | 쿼리 최적화, 캐시 도입, N+1 해소 |
| 테스트 부채 | 테스트 0건 → 커버리지 확보 |
| 아키텍처 개선 | 모놀리스 분리, Hexagonal 전환, 모듈 분리 |
| 코드 품질 | detekt 위반 해소, nullable 정리, 컨벤션 통일 |

## 출력 경로

```
.analysis/be-refactoring/results/{날짜}_{대상}/
│
├── analysis/                              # Phase 1 현황 분석
│   ├── current_state.md                   # 현재 코드 구조, 문제점, 의존 관계
│   ├── impact_assessment.md               # 변경 영향 범위, 호출자, 의존 서비스
│   ├── risk_assessment.md                 # 리스크 매트릭스, 기술 부채 수치화
│   └── tech_reference.md                  # 참조 패턴/오픈소스/레퍼런스 (벤치마킹 대응)
│
├── plan/                                  # Phase 2 개선 계획
│   ├── migration_plan.md                  # 마이그레이션 전략, 단계별 계획, 롤백
│   ├── tdd.md                             # 기술 설계 문서 (project-analysis와 동일 템플릿)
│   ├── detailed_design.md                 # 상세 설계 + 확장 후보
│   └── tickets/                           # 구현 티켓
│       ├── _overview.md
│       └── ticket_{NN}_{이름}.md
│
└── README.md                              # 산출물 인덱스
```

## 에이전트 역할

### 페르소나 에이전트 (선택적 병행 스폰)

| 에이전트 | 관점 | 병행 시점 |
|---------|------|----------|
| [`be-tech-lead`](../agents/be-tech-lead.md) | 아키텍처 일관성, 서비스 간 영향, 되돌리기 비용 판단 | Phase 1 현황 보고서 리뷰, Phase 2 마이그레이션 계획 리뷰 |
| [`be-senior`](../agents/be-senior.md) | 프로덕션 안전성, 롤백 가능성, 하위 호환성 | Phase 2-R.5 리뷰 게이트, Phase 3 코드 리뷰 |
| [`be-ic`](../agents/be-ic.md) | TDD 구현, 디버깅, 컨벤션 준수 | Phase 3 구현 |

### Phase 1: 현황 분석 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`refactor-code-analyst`](../agents/refactor-code-analyst.md) | 코드 현황 분석가 | `current_state.md` |
| [`refactor-impact-analyst`](../agents/refactor-impact-analyst.md) | 영향 범위 분석가 | `impact_assessment.md` |
| [`refactor-risk-analyst`](../agents/refactor-risk-analyst.md) | 리스크 분석가 | `risk_assessment.md` |
| [`refactor-researcher`](../agents/refactor-researcher.md) | 기술 레퍼런스 조사 | `tech_reference.md` |

### Phase 1-R.5: 리뷰 게이트

| 체크 항목 | 통과 기준 |
|----------|----------|
| 현황 코드 근거 | 실제 파일 경로 + 코드 참조가 있는지 |
| 영향 범위 완전성 | 호출자 전수 조사, FE/BE/인프라 모두 확인 |
| 리스크 수치화 | 감에 의한 판단이 아닌 코드 메트릭 기반 |
| 레퍼런스 적합성 | 참조 패턴이 현재 상황에 맞는지 (과도하지 않은지) |

### Phase 2: 개선 계획 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`refactor-planner`](../agents/refactor-planner.md) | 마이그레이션 설계자 | `migration_plan.md` |
| [`ticket-splitter`](../agents/ticket-splitter.md) | 티켓 분할 전문가 | `tickets/` |

### Phase 2-R.5: 리뷰 게이트

| 체크 항목 | 통과 기준 |
|----------|----------|
| 기존 동작 보존 | AS-IS 동작이 변경되지 않는 계획인지 |
| 롤백 가능 | 각 단계에서 5분 이내 롤백 가능한지 |
| 과도 설계 체크 | 현재 필요한 것만 구현하는지 (확장 후보는 별도 기록) |
| 테스트 계획 | 리팩토링 전후 동등성 검증 방법이 있는지 |

### Phase 3: 구현

> project-analysis Phase 3과 동일한 사이클.
> 코드베이스 최신화 → TDD 사이클(RED→GREEN) → 리뷰 게이트 → PR

---

# Phase 1: 현황 분석

## 1-1. 사전 준비 (팀장)

1. 리팩토링/부채 대상 식별 (어떤 코드를, 왜 바꾸려는지)
2. 대상 레포/모듈의 코드 파악
3. 에이전트 prompt에 대상 코드 경로 + 개선 목표 포함

## 1-2. 병렬 분석 (에이전트, background)

| 에이전트 | 트리거 조건 | 산출물 |
|---------|-----------|--------|
| [`refactor-code-analyst`](../agents/refactor-code-analyst.md) | 항상 | `current_state.md` |
| [`refactor-impact-analyst`](../agents/refactor-impact-analyst.md) | 항상 | `impact_assessment.md` |
| [`refactor-risk-analyst`](../agents/refactor-risk-analyst.md) | 항상 | `risk_assessment.md` |
| [`refactor-researcher`](../agents/refactor-researcher.md) | 개선 방향이 명확하지 않을 때 | `tech_reference.md` |

## 1-3. 현황 보고서 (팀장)

에이전트 결과를 종합. `current_state.md`에 AS-IS 요약 + 문제점 + 개선 방향을 추가.

---

# Phase 2: 개선 계획

Phase 1 분석 결과를 입력으로 사용한다.

## 2-0. 코드베이스 최신화

> project-analysis 2-0과 동일.

## 2-1. 마이그레이션 계획

`migration_plan.md` 작성:
- AS-IS → TO-BE 구조
- 단계별 전환 (Phase A → B → C)
- 각 단계 FeatureFlag
- 롤백 시나리오
- 무중단 전환 전략 (Dual Write, Shadow Traffic 등 해당 시)

## 2-2. TDD + 상세 설계

> project-analysis 2-2와 동일 템플릿. 단, "Possible Solutions"에서 벤치마킹 대신 `tech_reference.md` 참조.

## 2-3. 구현 티켓

> project-analysis 2-3과 동일. 필수 티켓 체크리스트 적용.

---

# Phase 3: 구현

> project-analysis Phase 3과 동일.
> 3-0 코드 최신화 → 3-1 티켓 단위 TDD 사이클 → 3-2 리뷰 게이트 → 3-3 PR 체크리스트

---

## Phase 1 (현황 분석) 시 시도할 것

| 시도 | 설명 |
|------|------|
| **코드 고고학** | "왜 이렇게 짰을까"를 추론. 당시 제약 조건, 기술 스택, 팀 상황을 이해하면 더 나은 판단 가능 |
| **메트릭 수치화** | "복잡하다" → Cyclomatic Complexity 수치. "느리다" → P95 레이턴시. 감이 아닌 데이터로 판단 |
| **아키텍처 다이어그램** | 현재 시스템을 다이어그램으로 그려보기. 그릴 수 없으면 이해가 부족한 것 |

## Phase 2 (계획) 시 시도할 것

| 시도 | 설명 |
|------|------|
| **Strangler Fig 패턴** | 레거시를 한번에 교체하지 않고, 새 코드가 점진적으로 레거시를 감싸며 대체. 실전 경험 |
| **무중단 마이그레이션 설계** | Dual Write, Shadow Traffic, FeatureFlag, Canary — 실서비스 무중단 전환 경험 |
| **하위 호환성 설계** | API/이벤트/DB 스키마의 하위 호환을 깨지 않으면서 구조를 바꾸는 기술 |

## Phase 3 (구현) 시 시도할 것

| 시도 | 설명 |
|------|------|
| **동등성 테스트** | AS-IS와 TO-BE가 동일한 결과를 내는지 자동 검증. Shadow Traffic diff 경험 |
| **점진적 전환 실습** | Flag ON/OFF로 한 워크스페이스씩 전환. 실서비스 릴리즈 경험 |
| **롤백 실습** | 의도적으로 롤백 시나리오를 실행해보기. "5초 내 롤백 가능"을 실제로 확인 |

---

# 출력 어조 <!-- 상세 규칙은 공통 가이드 참조 -->

- 해요체(~합니다). 핵심부터. 한 문장에 하나의 정보.
- 표/불릿 우선. 코드 참조 시 `파일경로#메서드명` 형식.

# Mermaid 규칙 <!-- 상세 규칙은 공통 가이드 참조 -->

- subgraph 그룹핑, LR 방향 통일, 노드 15개 이하, & 체이닝 금지

# 티켓 구조

> project-analysis ticket_N_*.md와 동일:
> 작업 내용(설계 의도) + 다이어그램(처리 흐름 + 클래스 의존) + 테스트 케이스 + AC
