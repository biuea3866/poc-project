# [Ticket #4] MongoDB → MySQL 데이터 마이그레이션 배치

## 개요
- TDD 참조: tdd.md 섹션 5.3 (마이그레이션), 2.2 (현재 데이터 모델)
- 선행 티켓: #4a (듀얼라이트 레이어)
- 크기: L

## 작업 내용

### 변경 사항

MongoDB에 저장된 결제 이력과 크레딧 이력 데이터를 새 MySQL 스키마로 이관하는 Spring Batch 기반 마이그레이션 배치를 구현한다.

#### 마이그레이션 대상

| 소스 (MongoDB) | 타겟 (MySQL) | 매핑 |
|---------------|-------------|------|
| PaymentLogsOnGroup | order + order_item + payment | 1:N 분해 (1 로그 → 1 order + 1 order_item + 1 payment) |
| MessagePointLogsOnWorkspace | credit_ledger | 1:1 매핑 (SMS 사용 이력 → CHARGE/USE 분류) |
| MessagePointChargeLogsOnWorkspace | credit_ledger | 1:1 매핑 (SMS 충전 이력 → CHARGE 타입) |

#### 배치 Job 설계

**Job 1: PaymentLogsMigrationJob**
- Step 1: `PaymentLogsOnGroup` → `order` + `order_item` + `payment`
  - Reader: `MongoPagingItemReader<PaymentLogsOnGroup>` (정렬: createdAt ASC)
  - Processor: `PaymentLogsToOrderProcessor`
    - PaymentLogsOnGroup 1건 → (Order + OrderItem + Payment) 변환
    - order_number: `MIG-{yyyyMMdd}-{MongoDB _id 8자리}` (마이그레이션 식별)
    - order_type: 기존 로그 유형에 따라 분류 (NEW, RENEWAL, PURCHASE 등)
    - status: 기존 결제 상태 매핑 (완료 → COMPLETED, 실패 → PAYMENT_FAILED, 취소 → CANCELLED)
    - 금액 매핑: price → total_amount, original_amount 동일, vat_amount 계산 (total / 11)
    - product_code: PlanOnGroup 타입 기반 역매핑 (BASIC → PLAN_BASIC 등)
    - payment_key: 기존 PG 응답에서 추출
    - payment_method: billingKey 존재 여부로 판단
  - Writer: `JpaItemWriter` (order → orderItem → payment 순차 저장)

**Job 2: MessagePointLogsMigrationJob**
- Step 1: `MessagePointLogsOnWorkspace` → `credit_ledger`
  - Reader: `MongoPagingItemReader<MessagePointLogsOnWorkspace>` (정렬: createdAt ASC)
  - Processor: `MessagePointLogsToLedgerProcessor`
    - credit_type: `SMS`
    - transaction_type: 로그 유형에 따라 USE 분류
    - amount: 음수 (사용량)
    - balance_after: 사용 후 잔액 (로그에 기록된 값 사용)
  - Writer: `JpaItemWriter<CreditLedger>`

- Step 2: `MessagePointChargeLogsOnWorkspace` → `credit_ledger`
  - Reader: `MongoPagingItemReader<MessagePointChargeLogsOnWorkspace>` (정렬: createdAt ASC)
  - Processor: `MessagePointChargeLogsToLedgerProcessor`
    - credit_type: `SMS`
    - transaction_type: CHARGE
    - amount: 양수 (충전량)
    - balance_after: 충전 후 잔액
    - order_id: PaymentLogsMigrationJob에서 생성된 order와 매핑 (가능한 경우)
  - Writer: `JpaItemWriter<CreditLedger>`

#### 배치 설정

```
chunk-size: 500
skip-limit: 100 (건당 에러 허용 한도)
retry-limit: 3 (재시도 횟수)
skip-policy: AlwaysSkipItemSkipPolicy (에러 건 스킵 후 계속 진행)
retry-policy: SimpleRetryPolicy (DeadlockLoserDataAccessException, TransientDataAccessException)
```

#### 에러 처리 전략
1. **SkipListener**: 스킵된 건의 MongoDB _id + 에러 메시지를 `migration_error_log` 테이블에 기록
2. **ChunkListener**: 청크 단위 처리 현황 로깅 (처리 건수, 스킵 건수, 소요 시간)
3. **JobExecutionListener**: Job 시작/종료 시 총 건수, 성공/실패/스킵 건수 집계 로깅
4. **중복 방지**: idempotency_key로 이미 이관된 건 스킵 (order.idempotency_key = `MIG-{MongoDB_id}`)

#### 진행률 로깅
- 1000건마다 진행률 출력: `[Job1/Step1] Processed: 15000/120000 (12.5%), Skipped: 3, Elapsed: 45s`
- Job 완료 시 요약 리포트:
  ```
  ===== Migration Summary =====
  Job: PaymentLogsMigrationJob
  Total Read: 120,000
  Total Written: 119,950
  Total Skipped: 50
  Duration: 12m 30s
  =============================
  ```

#### 실행 방식
- Spring Boot 실행 인자로 Job 선택: `--spring.batch.job.names=PaymentLogsMigrationJob`
- 순서: Job 1 (PaymentLogs) → Job 2 (MessagePointLogs) - order_id 참조 의존성
- 배치 전용 프로파일: `spring.profiles.active=migration`

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | batch/config | MigrationBatchConfig.kt | 신규 |
| greeting_payment-server | batch/job | PaymentLogsMigrationJob.kt | 신규 |
| greeting_payment-server | batch/job | MessagePointLogsMigrationJob.kt | 신규 |
| greeting_payment-server | batch/processor | PaymentLogsToOrderProcessor.kt | 신규 |
| greeting_payment-server | batch/processor | MessagePointLogsToLedgerProcessor.kt | 신규 |
| greeting_payment-server | batch/processor | MessagePointChargeLogsToLedgerProcessor.kt | 신규 |
| greeting_payment-server | batch/listener | MigrationSkipListener.kt | 신규 |
| greeting_payment-server | batch/listener | MigrationChunkListener.kt | 신규 |
| greeting_payment-server | batch/listener | MigrationJobListener.kt | 신규 |
| greeting_payment-server | batch/mapper | PaymentStatusMapper.kt | 신규 |
| greeting_payment-server | batch/mapper | ProductCodeMapper.kt | 신규 |
| greeting_payment-server | resources | application-migration.yml | 신규 |
| greeting-db-schema | payment-server/migration | V7__create_migration_error_log.sql | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T4-01 | PaymentLogs 단건 변환 | PaymentLogsOnGroup 1건 (결제 완료) | PaymentLogsToOrderProcessor.process() | Order(COMPLETED) + OrderItem(가격 스냅샷) + Payment(APPROVED) 반환 |
| T4-02 | PaymentLogs 실패 건 변환 | PaymentLogsOnGroup 1건 (결제 실패) | process() | Order(PAYMENT_FAILED) + Payment(FAILED) 반환 |
| T4-03 | PaymentLogs 취소 건 변환 | PaymentLogsOnGroup 1건 (환불) | process() | Order(REFUNDED) + Payment(CANCELLED) 반환 |
| T4-04 | MessagePointLogs 사용 건 변환 | MessagePointLogsOnWorkspace 1건 | process() | CreditLedger(type=USE, amount=-N) 반환 |
| T4-05 | MessagePointChargeLogs 충전 건 변환 | MessagePointChargeLogsOnWorkspace 1건 | process() | CreditLedger(type=CHARGE, amount=+N) 반환 |
| T4-06 | 배치 Job 1 전체 실행 | MongoDB에 PaymentLogs 100건 | PaymentLogsMigrationJob 실행 | order 100건 + order_item 100건 + payment 100건 생성됨 |
| T4-07 | 배치 Job 2 전체 실행 | MongoDB에 포인트 로그 200건 | MessagePointLogsMigrationJob 실행 | credit_ledger 200건 생성됨 |
| T4-08 | 청크 사이즈 동작 확인 | MongoDB에 1500건 | Job 실행 (chunk=500) | 3 청크로 분할 처리됨 |
| T4-09 | 진행률 로깅 | 5000건 처리 중 | Job 실행 | 1000건 단위 로그 출력 확인 |
| T4-10 | Job 완료 요약 리포트 | Job 실행 완료 | JobExecutionListener.afterJob() | 총 건수/성공/스킵/소요시간 로그 출력 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T4-E01 | 변환 불가 건 스킵 | PaymentLogs 중 필수 필드 누락 1건 | Job 실행 | 해당 건 스킵, migration_error_log에 기록, 나머지 정상 처리 |
| T4-E02 | 스킵 한도 초과 | 에러 건 101건 (skip-limit=100) | Job 실행 | Job FAILED 상태로 종료 |
| T4-E03 | 재시도 성공 | DB 일시적 연결 실패 후 복구 | 트랜잭션 에러 발생 | 최대 3회 재시도 후 성공 |
| T4-E04 | 중복 실행 방지 | 이미 마이그레이션된 건 (idempotency_key 존재) | 같은 Job 재실행 | 중복 건 스킵, 신규 건만 처리 |
| T4-E05 | 금액 0원 건 처리 | PaymentLogs에 무료 플랜 전환 건 (amount=0) | process() | Order(total_amount=0) + Payment(amount=0, method=MANUAL) |
| T4-E06 | MongoDB 빈 컬렉션 | PaymentLogs 0건 | Job 실행 | Job COMPLETED, 처리 건수 0 |
| T4-E07 | 대량 데이터 메모리 | MongoDB에 50만 건 | Job 실행 (chunk=500) | OOM 없이 완료, 페이징 처리 확인 |
| T4-E08 | order_number 생성 유일성 | 같은 날짜 마이그레이션 건 다수 | 변환 처리 | MongoDB _id 기반으로 유일한 order_number 생성 |

## 기대 결과 (AC)
- [ ] PaymentLogsOnGroup → order + order_item + payment 변환 배치가 정상 실행됨
- [ ] MessagePointLogsOnWorkspace → credit_ledger 변환 배치가 정상 실행됨
- [ ] MessagePointChargeLogsOnWorkspace → credit_ledger 변환 배치가 정상 실행됨
- [ ] chunk size 500으로 페이징 처리되어 대량 데이터에서도 메모리 안전함
- [ ] 변환 실패 건은 스킵되고 migration_error_log에 기록됨 (skip-limit: 100)
- [ ] 일시적 DB 에러에 대해 최대 3회 재시도함
- [ ] 1000건 단위 진행률 로깅 + Job 완료 시 요약 리포트 출력
- [ ] idempotency_key 기반 중복 실행 방지 (재실행 안전)
- [ ] 결제 상태, 주문 유형, 상품 코드 매핑이 정확함
- [ ] Job 1(PaymentLogs) → Job 2(MessagePointLogs) 순서로 실행 가능
