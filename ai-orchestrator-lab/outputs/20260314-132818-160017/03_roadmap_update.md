# Roadmap Update

## Goal
요구사항 분석 결과를 반영해 PRD, 요구사항, 릴리즈 로드맵을 최신화한다.

## Output Hint
최신 PRD, 요구사항 문서, 릴리즈 로드맵

## Owner
pm

## Lane
common

## Kind
planning

## Depends On
02_requirements_analysis

## Loop Target On Failure
-

## Input Snapshot
# AI Wiki PRD v2.1

## 제품 한 줄 정의
사용자는 기록에 집중하고, AI는 요약·태깅·임베딩·검색 준비를 담당한다.

## 문제 정의
- 개인 개발자는 메모를 남겨도 다시 찾기 어렵다.
- 문서 정리 비용이 높아 기록 습관이 끊긴다.
- 검색 가능한 지식 자산으로 전환하려면 후처리 자동화가 필요하다.

## MVP 목표
1. Markdown 문서를 계층형으로 저장하고 수정할 수 있다.
2. 문서를 ACTIVE로 전환하면 AI 파이프라인이 비동기로 실행된다.
3. 사용자는 SSE로 처리 상태를 실시간 확인할 수 있다.
4. 제목/본문/태그 기준으로 본인 ACTIVE 문서를 검색할 수 있다.

## 핵심 사용자
- 1차: 개인 개발자
- 2차: 소규모 기술 팀

## 핵심 시나리오
1. 사용자는 DRAFT 문서를 작성한다.
2. 문서를 ACTIVE로 전환하고 분석을 요청한다.
3. 시스템은 summary -> tagger -> embedding 순서로 처리한다.
4. 사용자는 결과 요약과 태그를 확인한다.
5. 사용자는 검색으로 관련 문서를 다시 찾는다.

## 상태 모델

### 문서 상태
- `DRAFT`
- `ACTIVE`
- `DELETED`

### AI 상태
- `NOT_STARTED`
- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

## 주요 제약
- `analyze`는 ACTIVE 문서에서만 허용한다.
- PROCESSING 중 재요청은 `409 Conflict`를 반환한다.
- ACTIVE 이후 수정은 revision을 생성한다.
- `updated_at` 기반 낙관적 잠금을 적용한다.
- 검색은 본인 ACTIVE 문서만 대상으로 한다.

## 백엔드 구현 컨벤션
- 객체지향, 클린 코드, 디자인 패턴 원칙에 기반해 작성한다.
- JPA Entity와 도메인 POJO를 분리한다.
- 전체 백엔드 구조는 헥사고날 아키텍처를 따른다.

## 오케스트레이션 요구사항
- PRD에서 모호성을 먼저 식별해야 한다.
- 모호성 응답을 반영해 로드맵과 아키텍처를 보강해야 한다.
- 구현 전 티켓이 독립 PR 단위로 분해되어야 한다.
- 보안, 상태 머신, 동시성 검토가 최종 단계에 포함되어야 한다.
- `be`와 `devops`는 협업해 Pinpoint 기반 애플리케이션 모니터링 시스템을 구축해야 한다.

## 운영/모니터링 요구사항
- 백엔드 애플리케이션은 Pinpoint 에이전트 연동 가능 구조를 제공해야 한다.
- DevOps는 Pinpoint 수집/조회 환경 구성을 담당해야 한다.
- `be`와 `devops`는 추적 대상, 배포 방식, 환경 변수, 운영 체크리스트를 함께 정의해야 한다.

## 기대 산출물
- 모호성 질문 목록
- 최신 PRD/로드맵
- 멀티 모듈 아키텍처 초안
- 구현 티켓 목록
- 리뷰 포인트
- 최종 기술 문서


## Dry Run Note
This file is a placeholder artifact. Replace this section with live LLM output.