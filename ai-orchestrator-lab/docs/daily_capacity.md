# Daily Capacity Guide

## 목적

하루 작업량은 투입 시간보다 "검증 가능한 완결 단위"로 관리합니다.
이 프로젝트에서는 하루 작업량을 lane별 티켓 처리량과 `complexity_score` 합계로 판단합니다.

## 완료 정의

하루 작업량 산정에서 "완료"는 아래를 모두 만족해야 합니다.

1. 구현 완료
2. 테스트 작성 또는 갱신 완료
3. 정적 분석 통과 가능 상태
4. 리뷰 요청 가능 상태

코드만 작성된 상태는 완료로 보지 않습니다.

## 기본 기준

초기 기준은 아래처럼 시작합니다.

- `be`: 하루 `complexity_score` 총합 4 이하
- `fe`: 하루 `complexity_score` 총합 4 이하
- `devops`: 하루 `complexity_score` 총합 3 이하
- `cross-lane`: 하루 1개만 진행

`cross-lane`에는 Pinpoint 구축처럼 `be + devops`가 함께 움직여야 하는 작업이 포함됩니다.

## 티켓 크기 규칙

- 하나의 티켓은 하나의 PR로 끝나야 합니다.
- 하루 작업량에 포함되는 티켓은 acceptance criteria가 명확해야 합니다.
- `P0` 또는 `risk_score >= 4` 티켓은 기본적으로 단독 처리합니다.

## 버퍼 규칙

운영성 작업과 리뷰 대응을 위해 하루 capacity의 20~30%는 버퍼로 남깁니다.

- `MAJOR` 리뷰 대응
- CI 실패 수정
- Pinpoint/배포/환경 변수 같은 운영성 이슈
- Confluence/Jira 반영 작업

## 추천 일일 계획 예시

### Backend
- 핵심 티켓 1개 또는 소형 티켓 2개
- `complexity_score` 합계 4 이하

### Frontend
- 화면/상태 처리 티켓 1개 또는 소형 티켓 2개
- `complexity_score` 합계 4 이하

### DevOps
- 인프라/배포 티켓 1개
- `complexity_score` 합계 3 이하

### Cross-lane
- Pinpoint 구축 같은 공동 작업 1개

## 주간 보정 방법

1주일 동안 아래를 기록하고 기준을 조정합니다.

- 티켓 시작 시각
- 리뷰 요청 시각
- 실제 완료 시각
- `complexity_score`
- `risk_score`
- 리뷰 결과(`APPROVE`, `MINOR`, `MAJOR`)

이 데이터를 기준으로 lane별 평균 처리량을 재산정합니다.
