# Refactoring Pipeline

리팩토링 계획·실행 절차.

## 진입점

- 사용자: `/refactor <대상>` 또는 main-orchestrator 호출

## 단계

### 1. 동기·범위 정의
- 왜 지금 리팩토링하는가? (기능 추가 직전 / 장애 RCA 결과 / 부채 누적)
- 범위 한계: "이 디렉토리만", "이 도메인만"
- 비범위 명시 (touch 하지 않을 영역)

### 2. 현 상태 측정
- 현재 코드 줄수, 의존 그래프, 테스트 커버리지
- harness-audit.py 전수 위반 카운트
- 측정 방법 명시 (재측정 가능해야 함)

### 3. 목표 상태 정의
- 측정 가능한 목표 (위반 0건 / 커버리지 +N% / 의존 D 도 감소 등)
- 비기능 변경 없음 (동작 동일성)

### 4. 단계 분해
- 한 PR = 한 가지 변형
- 각 PR 이 독립적으로 머지/롤백 가능
- TDD 적용: 테스트 먼저 (기존 동작 보존 검증)

### 5. 실행
- be-implementer / fe-implementer 가 PR 단위로 작업
- 각 PR 은 `.analysis/pr-review/PIPELINE.md` 통과
- 마지막 단계까지 부분 머지 가능

### 6. 검증
- 측정값 재측정 → 목표 도달 여부 확인
- 미달 시: 추가 단계 또는 목표 재조정 (섣불리 종료 금지)

### 7. 회고
- 발견된 안티패턴은 harness-rules 에 추가 (메타-피드백)
- 동일 리팩토링 재발 방지 (예: convention-detective 가 자동 검출)

## 산출물

- `.analysis/refactoring/<YYYYMMDD>-<scope>/`
  - `plan.md` (동기 + 범위 + 목표 + 단계)
  - `before.md` / `after.md` (측정값)
  - `retrospective.md` (선택)

## 안전 장치

- 동작 변경 금지 (스타일/구조만)
- 기능 추가는 별도 티켓
- 한 PR 줄수 상한 (default M=400줄)

## 참고

- 티켓 분해 절차: `skills/ticket-breakdown/SKILL.md`
- 리뷰: `.analysis/pr-review/PIPELINE.md`
