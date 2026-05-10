---
description: 리팩토링 — 동작 동일성 강제, as-is/to-be diff 검증
argument-hint: <간단 설명 또는 대상>
---

# /refactor — 절차

**대상**: $ARGUMENTS


리팩토링 계획·실행·검증 절차. **as-is/to-be 동작 동일성 강제** — 운영 사고(잘못된 라인의 update / 다른 조회 쿼리) 재발 방지.

## 진입점

- 사용자: `/refactor <대상>` 또는 main-orchestrator 호출

## 단계

### 1. 동기·범위 정의
- 왜 지금 리팩토링하는가? (기능 추가 직전 / 장애 RCA / 부채 누적)
- 범위 한계: "이 디렉토리만", "이 도메인만"
- 비범위 명시 (touch 하지 않을 영역)

### 2. 현 상태 측정 (강제 산출물)

```
outputs/refactor/<YYYYMMDD>-<scope>/before.md
```

다음 중 적용 가능한 항목 모두:
- 코드 줄수 / 의존 그래프 / 테스트 커버리지
- harness-audit.py 전수 위반 카운트
- **DB 쿼리 영향 시 — 현재 SQL 전문 첨부** (Hibernate logger 또는 `EXPLAIN` 결과)
- **외부 API 호출 영향 시 — 현재 호출 시퀀스 + payload 샘플**
- 측정 방법 명시 (재측정 가능해야 함)

### 3. 목표 상태 정의
- 측정 가능한 목표 (위반 0건 / 커버리지 +N% / 의존 D 도 감소 등)
- **비기능 변경 없음 (동작 동일성) 명시 — "동작이 어떻게 같아야 하는가" 를 항목별로**

### 4. 단계 분해
- 한 PR = 한 가지 변형
- 각 PR 이 독립적으로 머지/롤백 가능
- TDD 적용: 테스트 먼저 (기존 동작 보존 검증)

### 5. 실행
- be-implementer / fe-implementer 가 PR 단위로 작업
- 각 PR 은 `commands/review-pr.md` 통과
- 마지막 단계까지 부분 머지 가능

### 6. 동작 동일성 검증 (★ 강제)

```
outputs/refactor/<YYYYMMDD>-<scope>/diff-verification.md
```

다음 산출물이 **모두** 존재해야 "검증 완료" 단언 가능:

#### 6-1. 코드 diff 의 의미 분석
PR diff 를 읽고 다음 표 작성:

| 변경 유형 | 영향 받는 동작 | 변경 전 동작 | 변경 후 동작 | 동일성 |
|-----------|----------------|--------------|--------------|--------|
| 메서드 이동 | OrderService.process | A | A | ✅ same |
| 쿼리 수정 | Order 조회 | `SELECT … WHERE status='X'` | `SELECT … WHERE status='X' AND deleted_at IS NULL` | ⚠ 다름 — 의도된 변경? |
| update 라인 이동 | Order 상태 갱신 | line 42 (트랜잭션 내) | line 58 (트랜잭션 외) | ❌ **다름** — 락 동작 변경 |

**한 행이라도 ❌ 가 있으면 리팩토링이 아니라 동작 변경이다.** 별도 PR 로 분리하거나 의도된 변경임을 ADR 에 명시.

#### 6-2. SQL 영향 검증 (DB 변경 시 강제)
- 변경 전·후 동일 입력에 대해 **EXPLAIN / 쿼리 plan diff 첨부**
- Hibernate `org.hibernate.SQL` DEBUG 레벨로 실제 실행되는 SQL diff 캡처
- update 쿼리: 영향 받는 row 수 비교 (변경 전·후 같은 row 수에 적용되어야 함)
- select 쿼리: 결과 row 수 + 핵심 컬럼 값 spot check

```sql
-- before
SELECT id, status, updated_at FROM orders WHERE user_id = ? AND status = 'PAID';

-- after
SELECT id, status, updated_at FROM orders WHERE user_id = ? AND status IN ('PAID', 'CONFIRMED');
```

위 같은 차이 발견 시: ❌ 동작 변경. 리팩토링 아님.

#### 6-3. 외부 API 호출 검증 (외부 의존 시 강제)
- 호출 endpoint / method / headers / body shape diff
- 응답 처리 로직 diff (특히 에러 응답 분기)

#### 6-4. 통합 테스트 동일성
- 리팩토링 전 git stash → 동일 테스트 실행 → 통과 확인
- 리팩토링 후 → 동일 테스트 실행 → 통과 확인
- 두 결과의 raw 출력 첨부 (텍스트로 "둘 다 통과" 단언 금지)

### 7. 측정값 재측정

```
outputs/refactor/<YYYYMMDD>-<scope>/after.md
```

§2 의 항목을 동일 방법으로 재측정. before/after 비교 표.

### 8. 회고 (강제 산출물)

```
outputs/refactor/<YYYYMMDD>-<scope>/retrospective.md
```

- 동일성 검증에서 ❌ 가 나온 라인이 있었나? 어떻게 처리했나?
- 발견된 안티패턴은 harness-rules 에 추가 (메타-피드백 트리거 후보)
- 동일 리팩토링 재발 방지 (convention-detective 자동 검출 가능?)

## 강제 산출물 — 5개

```
outputs/refactor/<YYYYMMDD>-<scope>/
├── plan.md                  (1·3·4단계)
├── before.md                (2단계 — 강제)
├── diff-verification.md     (6단계 — ★ 강제, 운영 사고 방지의 핵심)
├── after.md                 (7단계 — 강제)
└── retrospective.md         (8단계 — 강제)
```

5개 모두 존재하지 않으면 "리팩토링 완료" 단언 금지.

## 안전 장치

- 동작 변경 금지 (스타일/구조만) — `diff-verification.md` 에 ❌ 가 1건도 있으면 차단
- 기능 추가는 별도 티켓
- 한 PR 줄수 상한 (default M=400줄)
- DB 쿼리 변경 시 staging 에서 부하 비교 권장
- 메타-피드백 트리거: ❌ 가 있는데 머지된 PR 발견 시 process-reviewer 발화

## 참고

- 티켓 분해: `skills/ticket-breakdown/SKILL.md`
- 리뷰: `commands/review-pr.md`
- 메타-피드백: `commands/audit-feedback-loop.md`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`rules/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
