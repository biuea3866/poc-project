# [Ticket #6] 마이그레이션 검증 스크립트

## 개요
- TDD 참조: tdd.md 섹션 5.3 (마이그레이션), 6 (리스크 - MongoDB 마이그레이션 중 데이터 유실)
- 선행 티켓: #4 (MongoDB → MySQL 마이그레이션 배치), #5 (기존 MySQL 테이블 매핑)
- 크기: M

## 작업 내용

### 변경 사항

마이그레이션 완료 후 소스(MongoDB + 기존 MySQL)와 타겟(신규 MySQL) 간 데이터 정합성을 검증하는 스크립트를 구현한다. 검증 결과를 리포트로 출력하고, 불일치 건에 대한 상세 내역을 제공한다.

#### 검증 항목

**1. 건수 검증 (Record Count Verification)**

| 소스 | 타겟 | 검증 규칙 |
|------|------|----------|
| PaymentLogsOnGroup (MongoDB) | order | 소스 건수 = 타겟 건수 + 스킵 건수 |
| PaymentLogsOnGroup (MongoDB) | order_item | order와 1:1 (order 건수 = order_item 건수) |
| PaymentLogsOnGroup (MongoDB) | payment | order와 1:1 (order 건수 = payment 건수) |
| MessagePointLogsOnWorkspace (MongoDB) | credit_ledger (type=USE) | 소스 건수 = 타겟 건수 + 스킵 건수 |
| MessagePointChargeLogsOnWorkspace (MongoDB) | credit_ledger (type=CHARGE) | 소스 건수 = 타겟 건수 + 스킵 건수 |
| PlanOnGroup (MySQL) | subscription | 소스 건수 = 타겟 건수 + 스킵 건수 |
| CardInfoOnGroup (MySQL) | billing_key | 소스 건수 = 타겟 건수 + 스킵 건수 |
| CreditOnGroup (MySQL) | credit_balance | 소스 건수 = 타겟 건수 + 스킵 건수 |

**2. 금액 합계 검증 (Amount Sum Verification)**

| 검증 항목 | 소스 쿼리 | 타겟 쿼리 | 허용 오차 |
|----------|----------|----------|----------|
| 총 결제 금액 | SUM(PaymentLogsOnGroup.amount) | SUM(payment.amount) WHERE status='APPROVED' | 스킵 건 금액 차이만 허용 |
| 총 충전 금액 | SUM(MessagePointChargeLogsOnWorkspace.amount) | SUM(credit_ledger.amount) WHERE transaction_type='CHARGE' | 스킵 건 금액 차이만 허용 |
| 워크스페이스별 잔액 | CreditOnGroup.point_balance | credit_balance.balance | 완전 일치 |
| 워크스페이스별 결제 건수 | COUNT(PaymentLogsOnGroup) GROUP BY group_id | COUNT(order) GROUP BY workspace_id | 스킵 건 차이만 허용 |

**3. 샘플 데이터 Spot Check**

무작위 샘플링으로 개별 건 레벨 데이터 정합성을 검증한다.

| 검증 항목 | 샘플 수 | 검증 필드 |
|----------|---------|----------|
| PaymentLogs → Order | 100건 | order_number 매핑, workspace_id, total_amount, status, created_at |
| PaymentLogs → Payment | 100건 | payment_key, amount, status, approved_at |
| PlanOnGroup → Subscription | 50건 | workspace_id, product_id(코드 매핑), status, period_start/end |
| CardInfoOnGroup → BillingKey | 30건 | workspace_id, card_company, card_number_masked, gateway |
| CreditOnGroup → CreditBalance | 50건 | workspace_id, credit_type, balance |

**4. 무결성 검증 (Integrity Check)**

| 검증 항목 | 쿼리 | 기대 결과 |
|----------|------|----------|
| order_item 고아 건 | order_item WHERE order_id NOT IN (SELECT id FROM order) | 0건 |
| payment 고아 건 | payment WHERE order_id NOT IN (SELECT id FROM order) | 0건 |
| subscription의 product_id 유효성 | subscription WHERE product_id NOT IN (SELECT id FROM product) | 0건 |
| credit_ledger의 workspace 유효성 | credit_ledger에 대응하는 credit_balance 존재 | 모든 workspace 일치 |
| order status 유효값 | order WHERE status NOT IN ('CREATED','PENDING_PAYMENT','PAID','COMPLETED','CANCELLED','REFUND_REQUESTED','REFUNDED','PAYMENT_FAILED') | 0건 |

#### 검증 스크립트 구조

```
greeting_payment-server/
└── batch/
    └── verification/
        ├── MigrationVerificationJob.kt          ← Spring Batch Job (전체 검증 오케스트레이터)
        ├── step/
        │   ├── RecordCountVerificationStep.kt    ← 건수 검증
        │   ├── AmountSumVerificationStep.kt      ← 금액 합계 검증
        │   ├── SampleSpotCheckStep.kt            ← 샘플 데이터 검증
        │   └── IntegrityCheckStep.kt             ← 무결성 검증
        ├── report/
        │   ├── VerificationReport.kt             ← 검증 결과 데이터 모델
        │   ├── VerificationReportGenerator.kt    ← 리포트 생성기
        │   └── DiscrepancyDetail.kt              ← 불일치 상세 내역
        └── rollback/
            └── RollbackScriptGenerator.kt        ← 롤백 SQL 생성기
```

#### 검증 리포트 출력 형식

```
========================================
  Migration Verification Report
  Generated: 2026-04-15 14:30:00
========================================

1. RECORD COUNT VERIFICATION
┌─────────────────────────────┬────────┬────────┬────────┬────────┐
│ Source → Target              │ Source │ Target │ Skipped│ Result │
├─────────────────────────────┼────────┼────────┼────────┼────────┤
│ PaymentLogs → order          │ 120000 │ 119950 │     50 │ PASS   │
│ PaymentLogs → payment        │ 120000 │ 119950 │     50 │ PASS   │
│ PointLogs → credit_ledger    │  85000 │  85000 │      0 │ PASS   │
│ ChargeLogs → credit_ledger   │  12000 │  11998 │      2 │ PASS   │
│ PlanOnGroup → subscription   │   5000 │   5000 │      0 │ PASS   │
│ CardInfo → billing_key       │   4500 │   4500 │      0 │ PASS   │
│ CreditOnGroup → credit_bal   │   5000 │   5000 │      0 │ PASS   │
└─────────────────────────────┴────────┴────────┴────────┴────────┘

2. AMOUNT SUM VERIFICATION
┌─────────────────────────────┬──────────────┬──────────────┬────────┐
│ Check                        │ Source Sum   │ Target Sum   │ Result │
├─────────────────────────────┼──────────────┼──────────────┼────────┤
│ Total Payment Amount         │ 1,234,567,890│ 1,234,500,000│ PASS*  │
│ Total Charge Amount          │   456,789,000│   456,789,000│ PASS   │
│ Workspace Balance Match      │        5,000 │        5,000 │ PASS   │
└─────────────────────────────┴──────────────┴──────────────┴────────┘
* 차이: 67,890원 (스킵 50건의 합계와 일치)

3. SAMPLE SPOT CHECK (100건)
┌──────────────────┬────────┬────────┬────────┐
│ Check             │ Passed │ Failed │ Result │
├──────────────────┼────────┼────────┼────────┤
│ Order Spot Check  │    100 │      0 │ PASS   │
│ Payment Spot Check│    100 │      0 │ PASS   │
│ Subscription      │     50 │      0 │ PASS   │
│ BillingKey        │     30 │      0 │ PASS   │
│ CreditBalance     │     50 │      0 │ PASS   │
└──────────────────┴────────┴────────┴────────┘

4. INTEGRITY CHECK
┌──────────────────────────────┬────────┬────────┐
│ Check                         │ Issues │ Result │
├──────────────────────────────┼────────┼────────┤
│ Orphan order_items            │      0 │ PASS   │
│ Orphan payments               │      0 │ PASS   │
│ Invalid subscription product  │      0 │ PASS   │
│ Credit ledger-balance mismatch│      0 │ PASS   │
│ Invalid order status          │      0 │ PASS   │
└──────────────────────────────┴────────┴────────┘

========================================
  OVERALL RESULT: PASS
  Total Checks: 16, Passed: 16, Failed: 0
========================================
```

#### 불일치 건 상세 리포트

FAIL 항목이 있을 경우 별도 CSV 파일 생성:

```
discrepancy_report_{timestamp}.csv

source_collection, source_id, target_table, target_id, field, source_value, target_value, description
PaymentLogsOnGroup, 64a1b2c3..., order, 12345, total_amount, 50000, 45000, "금액 불일치"
```

#### 롤백 스크립트 준비

마이그레이션 롤백이 필요한 경우를 대비한 SQL 스크립트를 자동 생성한다.

```sql
-- rollback_migration_{timestamp}.sql
-- WARNING: 이 스크립트는 마이그레이션 데이터를 삭제합니다.

-- 마이그레이션 건만 삭제 (order_number LIKE 'MIG-%')
DELETE FROM payment WHERE order_id IN (SELECT id FROM `order` WHERE order_number LIKE 'MIG-%');
DELETE FROM order_item WHERE order_id IN (SELECT id FROM `order` WHERE order_number LIKE 'MIG-%');
DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM `order` WHERE order_number LIKE 'MIG-%');
DELETE FROM `order` WHERE order_number LIKE 'MIG-%';

-- credit_ledger에서 마이그레이션 건 삭제 (description LIKE 'MIG:%')
DELETE FROM credit_ledger WHERE description LIKE 'MIG:%';

-- subscription/billing_key/credit_balance는 전체 TRUNCATE (새 테이블이므로)
TRUNCATE TABLE subscription;
TRUNCATE TABLE billing_key;
TRUNCATE TABLE credit_balance;
```

**RollbackScriptGenerator**:
- 마이그레이션 Job 실행 시 자동으로 롤백 스크립트 생성
- 마이그레이션 건 식별자 (MIG- prefix, description 등) 기반 선택적 삭제
- 생성 경로: `/var/log/payment-server/migration/rollback_migration_{timestamp}.sql`

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | batch/verification | MigrationVerificationJob.kt | 신규 |
| greeting_payment-server | batch/verification/step | RecordCountVerificationStep.kt | 신규 |
| greeting_payment-server | batch/verification/step | AmountSumVerificationStep.kt | 신규 |
| greeting_payment-server | batch/verification/step | SampleSpotCheckStep.kt | 신규 |
| greeting_payment-server | batch/verification/step | IntegrityCheckStep.kt | 신규 |
| greeting_payment-server | batch/verification/report | VerificationReport.kt | 신규 |
| greeting_payment-server | batch/verification/report | VerificationReportGenerator.kt | 신규 |
| greeting_payment-server | batch/verification/report | DiscrepancyDetail.kt | 신규 |
| greeting_payment-server | batch/verification/rollback | RollbackScriptGenerator.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T6-01 | 건수 검증 - 완전 일치 | MongoDB 100건, MySQL order 100건 | RecordCountVerificationStep 실행 | PASS, 불일치 0건 |
| T6-02 | 건수 검증 - 스킵 포함 일치 | MongoDB 100건, MySQL 97건, skip 3건 | RecordCountVerificationStep 실행 | PASS (100 = 97 + 3) |
| T6-03 | 금액 합계 일치 | 소스/타겟 결제 금액 합계 동일 | AmountSumVerificationStep 실행 | PASS |
| T6-04 | 워크스페이스별 잔액 일치 | CreditOnGroup.balance = credit_balance.balance (모든 workspace) | AmountSumVerificationStep 실행 | PASS |
| T6-05 | 샘플 Spot Check 전체 통과 | 100건 샘플링, 모든 필드 일치 | SampleSpotCheckStep 실행 | PASS, 100/100 |
| T6-06 | 무결성 검증 통과 | 고아 건 0건, 유효하지 않은 참조 0건 | IntegrityCheckStep 실행 | PASS |
| T6-07 | 전체 검증 Job 실행 | 마이그레이션 완료 상태 | MigrationVerificationJob 실행 | 4단계 모두 PASS, OVERALL: PASS |
| T6-08 | 리포트 출력 형식 | 검증 완료 | VerificationReportGenerator 실행 | 정의된 테이블 형식으로 콘솔 출력 |
| T6-09 | 롤백 스크립트 생성 | 마이그레이션 완료 | RollbackScriptGenerator 실행 | 유효한 SQL 파일 생성 |
| T6-10 | 불일치 없을 때 CSV 미생성 | 모든 검증 PASS | 검증 완료 | discrepancy CSV 파일 생성되지 않음 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T6-E01 | 건수 불일치 감지 | MongoDB 100건, MySQL 95건, skip 3건 | RecordCountVerificationStep | FAIL (2건 누락), 불일치 리포트에 기록 |
| T6-E02 | 금액 합계 불일치 감지 | 소스 합계 != 타겟 합계 + 스킵 합계 | AmountSumVerificationStep | FAIL, 차이 금액 리포트 |
| T6-E03 | Spot Check 필드 불일치 | 샘플 100건 중 2건 금액 불일치 | SampleSpotCheckStep | FAIL (98/100), 불일치 건 CSV 출력 |
| T6-E04 | 고아 order_item 감지 | order_item.order_id가 존재하지 않는 order 참조 | IntegrityCheckStep | FAIL, 고아 건 ID 목록 리포트 |
| T6-E05 | 롤백 스크립트 실행 검증 | 롤백 SQL 생성됨 | 빈 DB에 롤백 SQL 실행 | 문법 에러 없이 실행됨 (대상 없어도 에러 안 남) |
| T6-E06 | 대량 데이터 검증 성능 | 10만 건 이상 | 전체 검증 | 10분 이내 완료 |
| T6-E07 | MongoDB 연결 실패 | MongoDB 접속 불가 | 검증 Job 실행 | 명확한 에러 메시지 + Job FAILED |

## 기대 결과 (AC)
- [ ] 건수 검증: 소스(MongoDB/기존 MySQL) vs 타겟(신규 MySQL) 레코드 수 비교 통과
- [ ] 금액 합계 검증: 총 결제 금액, 총 충전 금액 일치 확인
- [ ] 워크스페이스별 크레딧 잔액 완전 일치 확인
- [ ] 샘플 Spot Check: 무작위 추출 건의 개별 필드 정합성 검증 통과
- [ ] 무결성 검증: 고아 레코드, 유효하지 않은 참조 0건
- [ ] 불일치 발견 시 상세 CSV 리포트가 생성됨 (source_id, target_id, 불일치 필드/값)
- [ ] 검증 결과가 정의된 테이블 형식으로 콘솔에 출력됨
- [ ] 롤백 스크립트가 자동 생성되어 마이그레이션 데이터 선택적 삭제 가능
- [ ] 롤백 스크립트가 문법적으로 유효한 SQL임
- [ ] 전체 검증이 10만 건 이상 데이터에서 10분 이내 완료
