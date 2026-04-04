# 프로젝트 분석 파이프라인

**PRD 기반** 프로젝트의 전체 사이클을 수행한다: PRD 분석 → 벤치마킹 → 기술 분석 → TDD 작성 → 구현 티켓 작성.

> PRD 없이 시작하는 리팩토링/기술 부채는 `be-refactoring` 파이프라인을 사용한다. → `.analysis/be-refactoring/CLAUDE.md`

## 공통 가이드 참조
> 아래 공통 규칙을 따릅니다. 파이프라인 특화 규칙은 이 문서에서 정의합니다.

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
- [티켓 작성법](../common/ticket-guide.md)
- [Jira 동기화](../common/jira-sync.md)
- [TDD 작성법](../common/tdd-template.md)
- [문서 싱크 체계](../common/document-sync.md)

## 실행 원칙

- Phase 1(PRD) → Phase 1.5(벤치마킹) → Phase 2(BE-Impl) 순서. Phase 내 에이전트는 최대한 병렬.
- 추측 금지. 반드시 관련 코드를 읽고 분석한다.
- 규모에 맞게 조절. 이슈 0건이어도 "검토 완료, 이슈 없음"으로 기록.
- **추상화 우선**: 테이블/클래스/기능 설계 시 통합·추상화를 먼저 시도한다. 분리는 추상화가 복잡하거나 불가능할 때만.
- **벤치마킹 근거**: 설계 결정에는 벤치마킹 제품의 패턴을 근거로 제시한다.
- **TDD**: 구현 시 테스트 먼저 작성(RED) → 구현(GREEN) → detekt 통과 → 커밋. 티켓 단위 브랜치.
- **추가 구현 요청 시 문서 먼저**: 구현 중 추가 요청이 생기면 1) detailed_design.md 등 디자인 문서 업데이트 2) _overview.md에 티켓 추가 3) 구현. 문서/티켓 없이 코드를 작성하지 않는다.
- **구현 후 의사결정 소급 반영**: 구현 과정에서 확정된 아키텍처/코드/테스트 결정은 detailed_design.md에 소급 기록한다.

## 에이전트 역할 정의

각 단계에서 전문가 역할의 에이전트를 스폰한다. 에이전트명은 `{phase}-{role}` 형식.
상세 역할 정의는 각 에이전트 파일을 참조한다. (`../agents/{agent-id}.md`)

### 페르소나 에이전트 (선택적 병행 스폰)

태스크 에이전트와 함께 스폰하여 **사람의 관점**으로 이중 점검한다.

| 에이전트 | 관점 | 병행 시점 |
|---------|------|----------|
| [`be-tech-lead`](../agents/be-tech-lead.md) | 아키텍처 일관성, 서비스 간 영향, 기술 전략 | Phase 1.5 아키텍처 설계, Phase 2 TDD 리뷰 |
| [`be-senior`](../agents/be-senior.md) | 프로덕션 안전성, 엣지 케이스, 성능 | Phase 2-2.5 TDD 리뷰, Phase 3 코드 리뷰 |
| [`be-ic`](../agents/be-ic.md) | TDD 구현, 디버깅, 컨벤션 준수 | Phase 3 구현 (impl-developer 대체 또는 병행) |

### Phase 1: PRD 분석 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`prd-functional`](../agents/prd-functional.md) | 기능 요구사항 분석가 | `*_기능요구사항.md` |
| [`prd-nonfunctional`](../agents/prd-nonfunctional.md) | 비기능 요구사항 분석가 | `*_비기능요구사항.md` |
| [`prd-ambiguity`](../agents/prd-ambiguity.md) | 모호성 검출 전문가 | `*_모호성.md` |
| [`prd-feasibility`](../agents/prd-feasibility.md) | 기술 실현성 분석가 | `*_기술실현성.md` |
| [`prd-scope`](../agents/prd-scope.md) | 작업 범위 산정가 | `*_작업범위.md` |
| [`prd-fe-dependency`](../agents/prd-fe-dependency.md) | FE 의존성 분석가 | `*_FE의존성.md` |
| [`prd-routing`](../agents/prd-routing.md) | 라우팅/인프라 분석가 | `*_라우팅분석.md` |
| [`prd-domain-arch`](../agents/prd-domain-arch.md) | 도메인 아키텍처 분석가 | `*_도메인아키텍처.md` |
| [`prd-migration`](../agents/prd-migration.md) | 데이터 마이그레이션 전문가 | `*_데이터마이그레이션.md` |

### Phase 1-2.5: 리뷰 게이트 에이전트

| 에이전트 | 역할 | 실행 모드 |
|---------|------|----------|
| [`review-completeness`](../agents/review-completeness.md) | 산출물 완전성 검증자 | foreground |

### Phase 1.5: 벤치마킹 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`bench-researcher`](../agents/bench-researcher.md) | 벤치마킹 리서처 | `*_benchmarking.md` |
| [`bench-architect`](../agents/bench-architect.md) | 아키텍처 설계자 | `*_architecture.md` |

### Phase 2: BE 구현 설계 에이전트

| 에이전트 | 역할 | 산출물 |
|---------|------|--------|
| [`tech-arch`](../agents/tech-arch.md) | 아키텍처 분석가 | `architecture_analysis.md` |
| [`tech-data`](../agents/tech-data.md) | 데이터 분석가 | `data_analysis.md` |
| [`ticket-splitter`](../agents/ticket-splitter.md) | 티켓 분할 전문가 | `tickets/` |
| [`test-designer`](../agents/test-designer.md) | 테스트 설계자 | 각 티켓 TC |

### Phase 2-2.5: TDD 리뷰 게이트 에이전트

| 에이전트 | 역할 | 실행 모드 |
|---------|------|----------|
| [`review-tdd`](../agents/review-tdd.md) | TDD 품질 검증자 | foreground |

### Phase 3: 구현 에이전트

| 에이전트 | 역할 | 실행 모드 |
|---------|------|----------|
| [`impl-developer`](../agents/impl-developer.md) | 구현 개발자 | background (티켓 단위) |
| [`review-code`](../agents/review-code.md) | 코드 리뷰어 | background |
| [`impl-prd-checker`](../agents/impl-prd-checker.md) | PRD 대조 검증자 | foreground |
| [`impl-doc-sync`](../agents/impl-doc-sync.md) | 문서 동기화 담당 | background |

### 에이전트 스폰 규칙

1. **Phase 내 에이전트는 최대한 병렬** 스폰한다 (독립 작업이면 한 메시지에 여러 Agent 호출)
2. **리뷰 게이트 에이전트는 foreground**로 실행한다 (결과를 확인해야 다음 Phase 진입 가능)
3. **구현 에이전트(impl-developer)는 티켓 단위**로 스폰한다 (1티켓 = 1에이전트 또는 관련 티켓 묶음)
4. **리뷰 에이전트(review-code, impl-prd-checker)는 구현 묶음 완료 후** 스폰한다
5. **문서 동기화(impl-doc-sync)는 리뷰 통과 후** 스폰한다

## 입력 처리

| 유형 | 처리 |
|------|------|
| Confluence URL/페이지 ID | MCP `read_confluence_page`로 추출 |
| Jira 티켓 (GRT-XXXX) | MCP `read_jira_issue`로 추출 |
| 텍스트 | 그대로 PRD로 사용 |

## 출력 경로

> 파이프라인 실행 시 `prd/`와 `be/` 디렉토리가 자동 생성됩니다.

```
.analysis/project-analysis/results/{날짜}_{기능명}/
│
├── prd/                                          # Phase 1 + 1.5 산출물
│   ├── PRD_{날짜}_{기능명}_기능요구사항.md
│   ├── PRD_{날짜}_{기능명}_비기능요구사항.md
│   ├── PRD_{날짜}_{기능명}_모호성.md
│   ├── PRD_{날짜}_{기능명}_기술실현성.md
│   ├── PRD_{날짜}_{기능명}_작업범위.md
│   ├── PRD_{날짜}_{기능명}_FE의존성.md             # API 변경/이관 시
│   ├── PRD_{날짜}_{기능명}_라우팅분석.md            # API 변경/이관 시
│   ├── PRD_{날짜}_{기능명}_도메인아키텍처.md         # 기존 시스템 변경 시
│   ├── PRD_{날짜}_{기능명}_데이터마이그레이션.md      # 스키마/데이터 변경 시
│   ├── gap_analysis.md                           # Phase 1 최종 보고서
│   ├── {기능명}_benchmarking.md                   # Phase 1.5 벤치마킹
│   └── {기능명}_architecture.md                   # Phase 1.5 아키텍처 초기 설계
│
├── be/                                           # Phase 2 + 2-3 산출물
│   ├── architecture_analysis.md                  # 2-1 아키텍처 분석
│   ├── data_analysis.md                          # 2-1 데이터 분석
│   ├── tdd.md                                    # 2-2 TDD (기술 설계 문서)
│   ├── detailed_design.md                        # 2-2+ 상세 설계 + 확장 후보
│   └── tickets/                                  # 2-3 구현 티켓
│       ├── _overview.md
│       └── ticket_{NN}_{이름}.md
│
└── README.md                                     # 산출물 인덱스
```

## 출력 어조 <!-- 상세 규칙은 공통 가이드 참조 -->

- 해요체(~합니다). 핵심부터. 한 문장에 하나의 정보.
- 표·불릿 우선. "여러 곳" 대신 "3곳" 처럼 구체적으로.
- 코드 참조 시 `파일경로#메서드명` 형식. 약어는 처음 등장 시 풀네임.

---

# Phase 1: PRD 분석

## 1-1. 사전 준비 (팀장)

1. PRD 내용 추출
2. `analysis/architecture/00_ARCHITECTURE_OVERVIEW.md` + `analysis/perspectives/` 관련 파일 읽기
3. 영향받는 서비스의 기존 코드 파악
4. 위 컨텍스트를 에이전트 prompt에 포함

## 1-2. 병렬 분석 (에이전트, background)

PRD 내용에 따라 필요한 에이전트만 선택하여 **한 번에** 스폰한다.
각 에이전트에게 PRD 전문 + 시스템 컨텍스트 + 산출물 저장 경로를 prompt에 포함한다.

| 에이전트 | 트리거 조건 | 역할 요약 | 산출물 |
|---------|-----------|---------|--------|
| [`prd-functional`](../agents/prd-functional.md) | 항상 | 기능별 완전성, Happy/Unhappy path, 우선순위, 누락 기능 | `{날짜}_{기능명}_기능요구사항.md` |
| [`prd-nonfunctional`](../agents/prd-nonfunctional.md) | 항상 | 성능/보안/확장성/마이그레이션/멱등성 수치화 | `{날짜}_{기능명}_비기능요구사항.md` |
| [`prd-ambiguity`](../agents/prd-ambiguity.md) | 항상 | 모호성 검출, PM 확인 질문 목록 | `{날짜}_{기능명}_모호성.md` |
| [`prd-feasibility`](../agents/prd-feasibility.md) | 항상 | 영향 서비스, 코드 패턴, 기술 리스크 + 대안 | `{날짜}_{기능명}_기술실현성.md` |
| [`prd-scope`](../agents/prd-scope.md) | 항상 | FE/BE/인프라 변경 범위, QA 시나리오 | `{날짜}_{기능명}_작업범위.md` |
| [`prd-fe-dependency`](../agents/prd-fe-dependency.md) | API 변경/이관 시 | FE 레포 전수 조사, JSON 호환성 | `{날짜}_{기능명}_FE의존성.md` |
| [`prd-routing`](../agents/prd-routing.md) | API 변경/이관 시 | Gateway 라우팅, 인증 필터, K8s 구조 | `{날짜}_{기능명}_라우팅분석.md` |
| [`prd-domain-arch`](../agents/prd-domain-arch.md) | 기존 시스템 변경 시 | 도메인 구조, 이벤트 흐름, 확장 설계 | `{날짜}_{기능명}_도메인아키텍처.md` |
| [`prd-migration`](../agents/prd-migration.md) | 스키마/데이터 변경 시 | 마이그레이션 계획, 호환성, 롤백 | `{날짜}_{기능명}_데이터마이그레이션.md` |
| **디자인 검증** | Figma 제공 시 | 디자인↔PRD 불일치, 상태 매핑, 필드 불일치 | `{날짜}_{기능명}_디자인검증.md` |

## 1-2.5. 리뷰 게이트 (1-3 진입 전)

에이전트 분석 결과를 종합하기 전, 산출물 품질을 검증한다.

| 체크 항목 | 통과 기준 |
|----------|----------|
| 에이전트 산출물 완전성 | 스폰한 에이전트 전부 완료, 빈 파일 없음 |
| 코드 탐색 근거 | "추측"이 아닌 실제 코드 경로 참조가 있는지 |
| 누락 에이전트 | PRD 내용에 따라 필요한 에이전트가 빠지지 않았는지 (FE 의존성, 라우팅 등) |
| 상호 모순 | 에이전트 간 분석 결과가 충돌하지 않는지 |

---

## 1-3. Gap 분석 보고서 (팀장)

에이전트 결과를 종합하여 `gap_analysis.md` 작성.

**구조**:
```
# 요구사항 Gap 분석
> PRD: {제목/링크} | 분석일: {날짜}

## 1. 애매한 요구사항
| # | PRD 원문 | 애매한 점 | 해석 옵션 | 추천 |

## 2. 누락된 요구사항
| # | 누락 항목 | 필요 이유 | 영향도 | 제안 |

## 3. 추가 고려사항
| # | 고려사항 | 카테고리 | 상세 | 제안 | 벤치마킹 참조 |

## 4. 디자인 ↔ PRD 불일치 (디자인 제공 시)
| # | 디자인 내용 | PRD 내용 | 불일치 유형 | 제안 |

## 5. 영향 범위 요약
| 파트 | 예상 작업 항목 | 비고 |

## 6. FE 영향 요약 (API 변경/이관 시)
| FE 서비스 | 호출 API 수 | 코드 변경 | 환경변수 변경 | 배포 필요 | 핵심 리스크 |

## 7. 라우팅/인프라 요약
| 경로 | 현재 라우팅 | 전환 후 라우팅 | 변경 필요 |

## 8. 의사결정 필요 항목
| # | 질문 | 대상 | 우선순위 | 디폴트 제안 |
```

---

# Phase 1.5: 벤치마킹 리서치

Phase 1 완료 후, BE 설계에 앞서 벤치마킹을 수행한다.
벤치마킹 결과는 Phase 1 산출물(gap_analysis, 아키텍처, 작업범위 등)에 소급 반영한다.

## 1.5-1. 벤치마킹 수행 (에이전트, foreground)

PRD 도메인과 관련된 제품들을 **3개 카테고리**로 조사한다:

| 카테고리 | 대상 | 분석 항목 |
|---------|------|---------|
| **직접 경쟁사** | 동일 도메인의 B2B SaaS 5개+ | 기능 설정 구조, 멀티채널 전략, Default ON/OFF 철학, 핵심 패턴 |
| **참조 B2B SaaS** | 해당 기능 영역이 정교한 제품 5개+ | 아키텍처 패턴, UX 패턴, 확장성 전략 |
| **인프라/플랫폼** | 해당 기능의 전문 인프라 제품 3개+ | 데이터 모델, API 설계, 추상화 패턴, 오픈소스 아키텍처 |

**산출물**: `{기능명}_benchmarking.md`

각 제품별 분석 구조:
```
### {제품명}

**{기능} 설정 구조**
- {핵심 설계 패턴 설명}

**멀티채널/확장 전략**
- {전략 설명}

**Default 철학**
- {기본값 정책}

**Greeting에 대한 시사점**
- {구체적 적용 방안}
```

리포트 말미에 반드시 포함:
1. **크로스커팅 분석**: 주요 패턴 유형별 비교 (어떤 모델이 있고, 각각 장단점)
2. **Greeting 권장 하이브리드 모델**: 여러 제품의 패턴을 조합한 최적 모델 제안
3. **채택/미채택 패턴 표**: 각 패턴의 채택 여부와 이유

## 1.5-2. 아키텍처 설계 (팀장)

벤치마킹 결과를 기반으로 `{기능명}_architecture.md`를 작성한다.

**필수 반영 사항**:
- 모든 설계 결정에 벤치마킹 참조 제품을 명시 (예: "Novu 3계층 Preference 참조")
- 채택한 패턴과 미채택 패턴의 이유를 표로 정리
- 테이블/클래스/enum 설계 시 확장성 고려 (추상화 우선 원칙)

## 1.5-2.5. 리뷰 게이트 (1.5-3 진입 전)

아키텍처 설계를 산출물에 반영하기 전, 설계 품질을 검증한다.

| 체크 항목 | 통과 기준 |
|----------|----------|
| 벤치마킹 근거 | 모든 설계 결정에 참조 제품이 명시되어 있는지 |
| 채택/미채택 표 | 각 패턴의 채택 여부와 이유가 표로 정리되어 있는지 |
| 추상화 우선 원칙 | 통합 가능한 것이 불필요하게 분리되지 않았는지 |
| Phase 1 산출물과 일관성 | 벤치마킹 결과가 gap_analysis, 작업범위와 충돌하지 않는지 |
| TDD 연계 | 아키텍처 결정이 TDD Detail Design에 반영 가능한 수준인지 |

---

## 1.5-3. Phase 1 산출물 소급 업데이트 (팀장)

벤치마킹 결과를 Phase 1 산출물에 반영한다:

| 대상 파일 | 업데이트 내용 |
|----------|-------------|
| `gap_analysis.md` | 추가 고려사항에 벤치마킹 기반 항목 추가, 결론에 핵심 설계 결정 추가, 산출물 목록에 벤치마킹/아키텍처 파일 추가 |
| `*_작업범위.md` | BE 변경 범위를 벤치마킹 기반 통합 아키텍처로 갱신, 공수 재산정 |
| `*_도메인아키텍처.md` | 개별 테이블/클래스 안을 통합 아키텍처 참조로 변경 |
| `*_기술실현성.md` | 벤치마킹에서 발견된 추가 리스크/대안 반영 |

---

# Phase 2: BE 구현 설계

Phase 1의 `gap_analysis.md` + Phase 1.5의 `{기능명}_architecture.md`를 입력으로 사용한다.

## 2-0. 코드베이스 최신화 (2-1 진입 전 필수)

기술 스택 분석은 실제 코드를 읽으므로, 분석 대상 코드가 최신이어야 한다.

```bash
# 대상 레포의 dev/main 브랜치 최신화
cd {대상 레포} && git checkout dev && git pull origin dev

# worktree 사용 시
bin/worktree-create.sh {repo} {branch}
```

**체크리스트:**
- [ ] 분석 대상 레포 전체 pull 완료
- [ ] 빌드 성공 확인 (`./gradlew compileKotlin`)
- [ ] 기존 테스트 통과 확인 (`./gradlew test`)

---

## 2-1. 기술 스택 분석 (에이전트 2명, background)

| 에이전트 | 역할 요약 | 산출물 |
|---------|---------|--------|
| [`tech-arch`](../agents/tech-arch.md) | Hexagonal 레이어, API 패턴, Kafka, 공유 라이브러리 | `architecture_analysis.md` |
| [`tech-data`](../agents/tech-data.md) | MySQL 스키마, Flyway, MongoDB, Redis, 인덱스/쿼리 | `data_analysis.md` |

## 2-2. TDD 작성 (팀장)

Phase 1 결과 + Phase 1.5 아키텍처 설계 + 2-1 분석 결과를 종합하여 `tdd.md`를 직접 작성한다.

> 템플릿: https://doodlin.atlassian.net/wiki/spaces/GREETING/pages/1002438983/TDD

**구조**:
```
# {기능명} TDD

## Background
## Overview
## Terminology
## Define Problem
## Possible Solutions
  ### 벤치마킹 참조 제품 (제품명, 카테고리, 참조 URL, 참조 패턴)
  ### 방안 비교 (각 방안을 풀어쓰기: "무엇인가 → 왜 채택/미채택했나". 표만 나열 금지)
## Detail Design
  ### 클래스 역할 정의
    #### 도메인 모델 (클래스명, 역할, 핵심 책임)
    #### 서비스 클래스 (클래스명, 역할, 입력→출력, 의존)
  ### AS-IS, TO-BE
  ### Component Diagram (Mermaid)
  ### Sequence Diagram (Mermaid)
## ERD (Mermaid. DDL 전문은 티켓에. TDD에는 요약만)
## FE 영향 분석 (API 변경/이관 시 필수)
  ### FE 의존 서비스 (어떤 FE가 어떤 API를 호출하는지)
  ### 전환 전략 (FE 코드 변경 유무, 환경변수 변경 유무, JSON 호환성)
  ### 핵심 리스크 (직렬화 차이, Swagger 호환, 응답 구조)
## Security Information (Optional)
## Milestone
## Testing Plan
## Release Scenario (배포 순서, 마이그레이션 선/후 조건, 롤백 플랜)
## Project Information
## Document History
```

### detailed_design.md 필수 섹션

위 TDD에 더해, detailed_design.md에는 아래 섹션이 추가로 필수:

```
## 확장 후보 (Extension Candidates)
> 벤치마킹에서 채택했으나 현재 PRD 범위에서 과도하여 제거한 기능.
> 향후 필요 시 복원할 설계 근거를 남긴다.

### E-N. {기능명}
- 벤치마킹 참조: {제품}
- 무엇인가: {설명}
- 구현 방법: {코드 수준 설계}
- 제거한 이유: {왜 과도한지}
- 복원 조건: {어떤 상황이 되면 필요한지}
```

**다이어그램 규칙**: Mermaid 사용. flowchart는 항상 `LR` 방향. <!-- 상세 규칙은 공통 가이드 참조 -->

## 2-2.5. 리뷰 게이트 (2-3 진입 전)

TDD가 완성된 후, 티켓 분할에 들어가기 전 TDD 품질을 검증한다.

| 체크 항목 | 통과 기준 |
|----------|----------|
| 클래스 역할 정의 | 도메인 모델, 서비스 클래스 역할이 모두 기술되어 있는지 |
| Possible Solutions | 벤치마킹 참조 URL + 방안 풀어쓰기가 있는지 (표만 나열 금지) |
| 다이어그램 | Component Diagram + Sequence Diagram이 코드 구조와 일치하는지 |
| ERD | 신규/변경 테이블이 모두 포함되어 있는지 |
| FE 영향 분석 | API 변경/이관 시 FE 의존 서비스, 전환 전략, 리스크가 있는지 |
| PRD 완전 커버 | PRD 요구사항이 TDD에 모두 반영되어 있는지 (누락 0건) |
| Release Scenario | 배포 순서, FeatureFlag, 롤백 플랜이 있는지 |

---

## 2-3. 구현 티켓 작성 (에이전트 2명, background)

| 에이전트 | 역할 요약 | 산출물 |
|---------|---------|--------|
| [`ticket-splitter`](../agents/ticket-splitter.md) | 독립 배포 단위 분할, 레이어별 분리, 의존관계 도출 | `tickets/` |
| [`test-designer`](../agents/test-designer.md) | 티켓별 TC Given/When/Then, Unit/Integration/E2E 레벨 | 각 티켓 TC |

### 필수 티켓 체크리스트

티켓 분할 완료 후, 아래 항목이 누락되지 않았는지 확인한다. 해당되는 항목은 반드시 티켓으로 포함한다.

| 항목 | 트리거 조건 | 티켓 내용 |
|------|-----------|---------|
| **FE JSON 호환성** | API 응답 구조가 변경되거나, 기술 스택 전환(NestJS→Spring 등) 시 | Response DTO snake_case/camelCase, null 처리, Date 포맷, Swagger 스펙, CORS |
| **Dual Write** | 데이터 저장소 이관(MongoDB→MySQL 등) 시 | 구/신 저장소 동시 기록 로직, FeatureFlag, MongoDB Read OutputPort |
| **배치 이관** | 기존 데이터 마이그레이션 필요 시 | Spring Batch Job, Admin API(Retool), 건수 검증 Job |
| **Shadow Traffic** | API 서비스 전환(Node→Spring 등) 시 | 구/신 서비스 응답 diff 비교, 샘플링, 로깅 |
| **트래픽 전환** | 서비스 라우팅 변경 시 | Canary 전환 단계, K8s/Ingress 변경, 모니터링 항목, 롤백 시나리오 |
| **정리** | 레거시 서비스/데이터 제거 시 | 구 서비스 제거, 데이터 아카이브, FeatureFlag 제거, 임시 코드 제거 |
| **Shadow Consumer** | Kafka Consumer 이관 시 | 새 Consumer Group 병렬 소비, Dry Run, 결과 비교 |

## 2-4. 최종 정리 (팀장)

에이전트 결과를 종합하여 `tickets/` 생성.

**_overview.md 구조**:
```
# 구현 티켓 요약
## PRD 참조
## 벤치마킹 참조 (아키텍처 설계의 핵심 결정 요약)
## 티켓 목록
| # | 티켓 | 레이어 | 의존성 | 예상 크기 |
## 의존 관계도 (Mermaid graph LR)
## 배포 순서
```

**ticket_N_*.md 구조**:
```
# [GRT-XXXX] {티켓 제목}

## 개요
- Jira / TDD 참조 / 선행 티켓 / 예상 크기(S/M)

## 작업 내용
### 변경 사항 (설계 의도 수준. 코드는 코드베이스가 기준)

## 다이어그램
### 처리 흐름 (sequenceDiagram — 해당 컴포넌트의 호출 흐름)
### 클래스 의존 (flowchart LR — 해당 티켓 클래스의 의존 관계)
> Mermaid 가시성 규칙: subgraph 그룹핑, LR 방향 통일, 노드 15개 이하, & 체이닝 금지 <!-- 상세 규칙은 공통 가이드 참조 -->

## 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |

## 영향 범위 (직접/간접/하위 호환성)

## 테스트 케이스
### 정상 | 예외/엣지
| ID | 테스트명 | Given | When | Then |

## AC (Acceptance Criteria)
## 체크리스트 (빌드/테스트/API문서/Flyway/하위호환)
```

---

# Phase 3: 구현

## 3-0. 코드베이스 최신화

> 2-0과 동일한 프로세스. 티켓 묶음 시작 전, 리뷰 게이트 통과 후, PR 전 — 총 3회 이상 실행.
> 상세: [2-0. 코드베이스 최신화](#2-0-코드베이스-최신화-2-1-진입-전-필수) 참조.

## 3-1. 티켓 단위 구현 사이클

각 티켓은 아래 사이클을 반복한다. **작성 → 리뷰 → 검증**을 건너뛰지 않는다.

```
┌─────────────────────────────────────────────────────┐
│  1. 테스트 작성 (RED)                                 │
│     └ 티켓의 TC를 Kotest BehaviorSpec으로 작성         │
│     └ 실행 → 실패 확인                                │
│                                                     │
│  2. 구현 (GREEN)                                     │
│     └ 테스트를 통과시키는 최소 코드 작성                  │
│     └ 실행 → 통과 확인                                │
│                                                     │
│  3. 자체 리뷰                                         │
│     └ 컨벤션 체크 (detekt, nullable, 캡슐화)            │
│     └ 의존 방향 체크 (포트 우회 없는지)                   │
│     └ 테스트 커버리지 체크 (정상 + 예외/엣지)             │
│                                                     │
│  4. 검증                                              │
│     └ detekt 통과: ./gradlew :domain:detekt           │
│     └ 전체 테스트 통과: ./gradlew :domain:test          │
│     └ 빌드 성공: ./gradlew :domain:compileKotlin       │
│                                                     │
│  5. 커밋                                              │
│     └ 티켓 단위 브랜치: feature/{ticket-id}             │
│     └ 커밋 메시지: feat/refactor/fix: {티켓 제목}       │
│                                                     │
│  6. 문서 소급 반영                                      │
│     └ 구현 중 변경된 설계 → detailed_design.md 업데이트  │
│     └ 신규 의사결정 → 의사결정 로그에 추가               │
└─────────────────────────────────────────────────────┘
```

## 3-2. Phase별 리뷰 게이트

각 Phase 완료 시 다음 Phase로 넘어가기 전 리뷰한다.

| Phase | 리뷰 항목 | 통과 기준 |
|-------|----------|----------|
| 구현 완료 | 전체 테스트 통과 + detekt 통과 | 실패 0건 |
| 코드 컨벤션 | nullable, 캡슐화, 포트 우회, @Comment | 위반 0건 또는 의도적 @Suppress |
| 문서 동기화 | detailed_design.md, tdd.md, 티켓이 코드와 일치 | 불일치 0건 |
| PRD 대조 | prd_gap_check 재실행 — 구현 누락/불일치 | Gap 0건 (FE 전용 제외) |
| 코드베이스 최신 | rebase 후 충돌 없음 + 테스트 통과 | 충돌 0건 |

## 3-3. PR 제출 전 최종 체크리스트

```
□ dev 브랜치 최신 rebase
□ ./gradlew :domain:detekt 통과
□ ./gradlew :domain:test 전체 통과
□ 신규 테이블 → TestContainers TABLE_SCRIPTS에 init SQL 추가
□ Swagger 어노테이션 (@Operation, @Schema) 확인
□ 문서 업데이트 완료 (detailed_design.md, tdd.md)
□ 티켓 AC 전부 충족
□ PR 설명에 변경 요약 + 테스트 방법 기재
```

---

# 기술 성장 포인트

> 각 Phase에서 단순히 기능을 구현하는 것을 넘어, BE 엔지니어로서 깊이를 더할 수 있는 시도를 기록한다.
> 산출물의 마지막에 `## 기술 성장 포인트` 섹션으로 작성.

## Phase 1 (PRD 분석) 시 시도할 것

| 시도 | 설명 | 산출물 |
|------|------|--------|
| **도메인 모델링** | PRD를 읽으면서 Bounded Context, Aggregate, Entity, Value Object를 식별. DDD 관점으로 도메인 경계를 그려보기 | gap_analysis.md에 "도메인 모델 후보" 섹션 |
| **비기능 요구사항 수치화** | "빨라야 한다" → "P95 < 200ms", "많은 데이터" → "일 100만 건" 등 구체적 메트릭으로 변환 | 비기능요구사항.md |
| **장애 시나리오 사전 정의** | 이 기능이 장애나면 어디까지 영향이 가는지, 어떻게 감지하는지 미리 설계 | 기술실현성.md |

## Phase 1.5 (벤치마킹) 시 시도할 것

| 시도 | 설명 | 산출물 |
|------|------|--------|
| **오픈소스 코드 리딩** | 벤치마킹 제품 중 오픈소스(Novu 등)가 있으면 실제 코드를 읽고 패턴 분석 | benchmarking.md에 코드 참조 |
| **트레이드오프 분석** | 채택한 패턴의 단점을 명확히. "이 패턴의 한계는 X이고, Y 상황이 되면 Z로 전환해야 한다" | architecture.md |
| **과도 설계 경계 설정** | "이건 지금 필요하고, 이건 나중에" 경계를 명확히 → 확장 후보(Extension Candidates)로 기록 | architecture.md |

## Phase 2 (설계) 시 시도할 것

| 시도 | 설명 | 산출물 |
|------|------|--------|
| **디자인 패턴 적용 근거** | Strategy, Template Method 등을 적용할 때 "왜 이 패턴인지" OCP/SRP 관점으로 설명 | tdd.md Detail Design |
| **테스트 전략 설계** | 단위/통합/E2E 경계를 왜 그렇게 나눴는지. Mock vs Real DB 선택 근거 | tdd.md Testing Plan |
| **장애 대응 설계** | Circuit Breaker, Retry, DLQ, 멱등성 등 장애 내성 패턴을 설계 시점에 고려 | detailed_design.md |
| **성능 예측** | 예상 TPS, 쿼리 실행 계획, 인덱스 전략을 설계 시점에 고려 | detailed_design.md |

## Phase 3 (구현) 시 시도할 것

| 시도 | 설명 | 기록 |
|------|------|------|
| **TDD 철저 실천** | RED→GREEN→Refactor. 테스트 없이 코드 작성하지 않기 | 커밋 히스토리로 증명 |
| **코드 리뷰 셀프 체크** | 구현 후 "내가 리뷰어라면 뭘 지적할까" 관점으로 자체 리뷰 | PR 설명 |
| **성능 측정** | 구현 후 실제 쿼리 실행 시간, API 응답 시간 측정. 예측과 비교 | PR 설명 또는 Notion |
| **의사결정 기록** | "왜 이렇게 구현했는지" — 대안이 있었는데 이걸 선택한 이유 기록 | detailed_design.md 의사결정 로그 |

---

# 설계 원칙

> 모든 산출물에 반드시 적용한다.

## 추상화 우선

- **통합 가능한 것은 하나로**. 유사한 테이블/클래스/기능은 먼저 통합 추상화를 시도한다.
- 추상화가 복잡하거나 불가능할 때만 분리한다.
- 신규 설계 시 "이 테이블/클래스를 일반화할 수 있는가?" 질문을 먼저 던진다.
- 예: 스코프별 별도 테이블(ProcessAlertConfigs, OpeningAlertConfigs...) 대신 → 통합 테이블 + scope_type 컬럼.
- 예: 채널별 별도 이벤트 클래스(MailEvent, SlackEvent...) 대신 → 통합 이벤트 + Strategy Pattern.

## 벤치마킹 근거

- 설계 결정에는 벤치마킹 제품의 패턴을 근거로 명시한다.
- 채택/미채택 패턴을 표로 정리하고, 각각의 이유를 기술한다.
- 클래스/테이블 설계 시 주석 또는 문서에 참조 제품을 표기한다.
  - 예: "Novu 3계층 Preference 참조", "MagicBell Priority Queue 참조"

## DB
- FK 제약 없음 (앱 레벨 관리, 주석으로 참조 표시)
- JSON/ENUM/BOOLEAN 타입 없음 → `TEXT` / `VARCHAR(30)` + 앱 enum / `TINYINT(1)`
- 시간: `DATETIME(6)`. Soft Delete: `deleted_at DATETIME(6)`
- 모든 컬럼/테이블 COMMENT 필수
- DDL 전문은 티켓에만. TDD에는 요약 + 참조 링크

## 도메인 모델
- 비즈니스 로직은 엔티티 메서드에 캡슐화. Service에 흩뿌리지 않음
- 상태 전이는 enum 내부 `canTransitionTo()`, `validateTransitionTo()`
- Service는 얇게: 엔티티 호출 → 저장 → 이벤트 발행. if/else 없음
- BC 분리: 각 Service는 자기 BC의 Repository만 의존

## 레이어
- Controller → Facade → Service. 여러 Service 직접 호출 금지
- Facade = 순수 오케스트레이터 (HTTP 호출 순서만). 비즈니스 로직/DB 접근 없음
- 복잡한 분기/확장이 필요한 곳은 적절한 디자인 패턴(Strategy, Template Method, Factory 등) 적용. when/if 나열 대신
- **`@Transactional`은 Service/Facade에서만.** Repository 레이어에 트랜잭션 경계를 두지 않음. QueryDSL update도 Service에서 @Transactional 내에서 호출

## Repository
- **`@Query` 미사용** → QueryDSL로 작성. JPA 메서드명 쿼리는 허용
- QueryDSL custom repository: `{Entity}QueryDslRepository` 인터페이스 + `{Entity}QueryDslRepositoryImpl` 구현
- JPA Repository에서 QueryDSL custom repository 상속: `interface FooJpaRepository : JpaRepository<Foo, Long>, FooQueryDslRepository`

## Controller
- **Swagger 어노테이션 필수**: `@Operation`, `@ApiResponses`, `@Parameter`, `@Schema` 전부 추가
- Request/Response DTO에도 `@Schema` 어노테이션 추가
- FQCN import 금지. 항상 정상 import 사용

## 스케줄러
- `@Scheduled` 미사용 → Spring Batch (Tasklet 또는 Chunk 기반)
- Batch Job은 트리거만, 로직은 Facade에. Admin API 제공 (Retool 수동 실행)

## 이벤트
- 내부(Spring Event): 상태 변경마다 DB 이력. 외부(Kafka): 터미널 상태에서 1회만
- 단일 이벤트 + 페이로드로 소비자 분기. 이벤트 발행은 도메인 서비스에서만

## Feature Flag
- `SimpleRuntimeConfig` 패턴 (`BooleanFeatureKey` + `FeatureFlagService` + `simple_runtime_config` 테이블)
- DB 기반 런타임 제어. `FeatureContext.ALL` 또는 `.workspace(id)`

## 테스트
- Kotest BehaviorSpec (Given/When/Then). TestContainers 싱글턴 + `withReuse(true)`
- **TDD 필수**: 티켓 작업 시 테스트 먼저 작성(RED) → 구현(GREEN) → detekt 통과 → 커밋
- **Given별 Mock 격리**: BehaviorSpec에서 Given 블록 간 mock 상태 누적 방지. data class Mocks 패턴 또는 Given 내 지역 mock 생성
- **통합 테스트에서 QueryDSL update**: TransactionTemplate으로 감싸서 실행 (Service에 @Transactional이 있으므로)
- **신규 테이블 테스트**: TestContainers TABLE_SCRIPTS에 `init_{도메인}.sql` 추가

## 무중단 마이그레이션
- Dual Write: Phase A(스키마)→B(듀얼라이트)→C(배치이관)→D(Shadow Read)→E(읽기전환)→F(제거)
- 각 Phase를 Feature Flag로 on/off. 5분 내 롤백
- 듀얼라이트 중 MongoDB = Source of Truth
