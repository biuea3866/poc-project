# 에이전트 정의

파이프라인에서 스폰하는 에이전트의 역할 정의를 관리합니다.

## 페르소나 (역할 기반)

태스크가 아닌 **사람의 관점**으로 점검하는 에이전트. 단독 자문 또는 태스크 에이전트와 병행 스폰 가능.

| 에이전트 | 설명 | 관점 |
|---------|------|------|
| [be-senior](be-senior.md) | 시니어 백엔드 엔지니어 | 프로덕션 안전성, 엣지 케이스, 성능, 운영 가시성 |
| [be-tech-lead](be-tech-lead.md) | 백엔드 테크 리드 | 아키텍처 일관성, 서비스 간 영향, 기술 전략, 부채 관리 |
| [be-ic](be-ic.md) | 백엔드 IC 개발자 | TDD 구현, 디버깅, 컨벤션 준수, 코드 작성 |

## PRD 분석

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [prd-functional](prd-functional.md) | 기능 요구사항 분석가 | project-analysis |
| [prd-nonfunctional](prd-nonfunctional.md) | 비기능 요구사항 분석가 | project-analysis |
| [prd-ambiguity](prd-ambiguity.md) | 모호성 검출 전문가 | project-analysis |
| [prd-feasibility](prd-feasibility.md) | 기술 실현성 분석가 | project-analysis |
| [prd-scope](prd-scope.md) | 작업 범위 산정가 | project-analysis |
| [prd-fe-dependency](prd-fe-dependency.md) | FE 의존성 분석가 | project-analysis |
| [prd-routing](prd-routing.md) | 라우팅/인프라 분석가 | project-analysis |
| [prd-domain-arch](prd-domain-arch.md) | 도메인 아키텍처 분석가 | project-analysis |
| [prd-migration](prd-migration.md) | 데이터 마이그레이션 전문가 | project-analysis |

## 벤치마킹

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [bench-researcher](bench-researcher.md) | 벤치마킹 리서처 | project-analysis |
| [bench-architect](bench-architect.md) | 아키텍처 설계자 | project-analysis |

## 설계

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [tech-arch](tech-arch.md) | 아키텍처 분석가 | project-analysis |
| [tech-data](tech-data.md) | 데이터 분석가 | project-analysis |
| [ticket-splitter](ticket-splitter.md) | 티켓 분할 전문가 | project-analysis, be-refactoring |
| [test-designer](test-designer.md) | 테스트 설계자 | project-analysis |

## 리뷰

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [review-completeness](review-completeness.md) | 산출물 완전성 검증자 | project-analysis |
| [review-tdd](review-tdd.md) | TDD 품질 검증자 | project-analysis |
| [review-code](review-code.md) | 코드 품질/패턴/보안 리뷰어 | project-analysis, pr-review |

## 구현

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [impl-developer](impl-developer.md) | 구현 개발자 | project-analysis |
| [impl-prd-checker](impl-prd-checker.md) | PRD 대조 검증자 | project-analysis |
| [impl-doc-sync](impl-doc-sync.md) | 문서 동기화 담당 | project-analysis |

## 리팩토링

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [refactor-code-analyst](refactor-code-analyst.md) | 코드 현황 분석가 | be-refactoring |
| [refactor-impact-analyst](refactor-impact-analyst.md) | 영향 범위 분석가 | be-refactoring |
| [refactor-risk-analyst](refactor-risk-analyst.md) | 리스크 분석가 | be-refactoring |
| [refactor-researcher](refactor-researcher.md) | 기술 레퍼런스 조사 | be-refactoring |
| [refactor-planner](refactor-planner.md) | 마이그레이션 설계자 | be-refactoring |

## 기술 부채

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [debt-scanner](debt-scanner.md) | 부채 자동 검출 | be-tech-debt |
| [debt-prioritizer](debt-prioritizer.md) | 우선순위 판정 | be-tech-debt |
| [debt-fixer](debt-fixer.md) | 부채 수정 개발자 | be-tech-debt |
| [debt-reviewer](debt-reviewer.md) | 수정 리뷰어 | be-tech-debt |

## CS 문의 대응

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [cs-be-tracer](cs-be-tracer.md) | BE 코드 추적자 | cs-log |
| [cs-fe-tracer](cs-fe-tracer.md) | FE 코드 추적자 | cs-log |
| [cs-system-analyst](cs-system-analyst.md) | 시스템 컨텍스트 분석가 | cs-log |
| [cs-issue-searcher](cs-issue-searcher.md) | 유사 이슈 탐색자 | cs-log |

## PR 리뷰

| 에이전트 | 설명 | 사용 파이프라인 |
|---------|------|----------------|
| [pr-impact-analyst](pr-impact-analyst.md) | 영향 범위 분석가 | pr-review |
