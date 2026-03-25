# [Ticket #5] 기존 MySQL 테이블 매핑

## 개요
- TDD 참조: tdd.md 섹션 5.3 (마이그레이션), 2.2 (현재 데이터 모델), 4.1.1 (Product 스키마)
- 선행 티켓: #2 (JPA 엔티티 + Repository)
- 크기: M

## 작업 내용

### 변경 사항

기존 payment-server MySQL 테이블(PlanOnGroup, CardInfoOnGroup, CreditOnGroup)의 데이터를 새 스키마(subscription, billing_key, credit_balance)로 매핑하고, 상품 카탈로그 Seed 데이터를 투입한다.

#### 1. 상품 카탈로그 Seed 데이터

Flyway 마이그레이션 스크립트로 초기 상품 데이터를 INSERT한다.

**product 테이블 Seed**

| code | name | product_type | 설명 |
|------|------|-------------|------|
| PLAN_FREE | 무료 플랜 | SUBSCRIPTION | 기본 무료 |
| PLAN_BASIC | Basic 플랜 | SUBSCRIPTION | 유료 기본 |
| PLAN_STANDARD | Standard 플랜 | SUBSCRIPTION | 유료 중급 |
| PLAN_BUSINESS | Business 플랜 | SUBSCRIPTION | 유료 고급 |
| SMS_PACK_100 | SMS 100건 팩 | CONSUMABLE | SMS 포인트 100건 |
| SMS_PACK_500 | SMS 500건 팩 | CONSUMABLE | SMS 포인트 500건 |
| SMS_PACK_1000 | SMS 1000건 팩 | CONSUMABLE | SMS 포인트 1000건 |
| SMS_PACK_5000 | SMS 5000건 팩 | CONSUMABLE | SMS 포인트 5000건 |
| SMS_PACK_10000 | SMS 10000건 팩 | CONSUMABLE | SMS 포인트 10000건 |

**product_metadata Seed** (플랜 레벨 등 메타 정보)

| product_code | meta_key | meta_value |
|-------------|----------|-----------|
| PLAN_FREE | plan_level | 0 |
| PLAN_BASIC | plan_level | 1 |
| PLAN_STANDARD | plan_level | 2 |
| PLAN_BUSINESS | plan_level | 3 |
| SMS_PACK_100 | sms_count | 100 |
| SMS_PACK_500 | sms_count | 500 |
| SMS_PACK_1000 | sms_count | 1000 |
| SMS_PACK_5000 | sms_count | 5000 |
| SMS_PACK_10000 | sms_count | 10000 |

**product_price Seed** (현재 유효 가격 - 실제 금액은 기존 코드 상수에서 추출)

| product_code | price | billing_interval_months | valid_from | valid_to |
|-------------|-------|----------------------|-----------|---------|
| PLAN_FREE | 0 | 1 | 2026-04-01 | NULL |
| PLAN_BASIC | (기존 상수) | 1 | 2026-04-01 | NULL |
| PLAN_BASIC | (기존 상수 * 10) | 12 | 2026-04-01 | NULL |
| PLAN_STANDARD | (기존 상수) | 1 | 2026-04-01 | NULL |
| PLAN_STANDARD | (기존 상수 * 10) | 12 | 2026-04-01 | NULL |
| PLAN_BUSINESS | (기존 상수) | 1 | 2026-04-01 | NULL |
| PLAN_BUSINESS | (기존 상수 * 10) | 12 | 2026-04-01 | NULL |
| SMS_PACK_* | (기존 상수) | NULL | 2026-04-01 | NULL |

> 실제 가격은 기존 payment-server 코드의 Plan enum / Price 상수에서 추출하여 확정

#### 2. PlanOnGroup → subscription 매핑

| PlanOnGroup 필드 | subscription 필드 | 변환 규칙 |
|-----------------|------------------|----------|
| id | - | 신규 ID 할당 |
| group_id (= workspace_id) | workspace_id | 직접 매핑 |
| plan_type | product_id | plan_type → product.code 매핑 후 product.id 참조 |
| - | status | ACTIVE (현재 유효한 플랜) |
| start_date | current_period_start | 직접 매핑 |
| end_date | current_period_end | 직접 매핑 |
| billing_cycle | billing_interval_months | MONTHLY→1, YEARLY→12 |
| auto_renewal | auto_renew | 직접 매핑 (TINYINT) |
| - | retry_count | 0 (초기값) |
| - | last_order_id | NULL (마이그레이션 이후 주문부터 기록) |
| - | version | 0 |

**배치 처리**:
- Reader: `JdbcPagingItemReader<PlanOnGroup>` (기존 MySQL 테이블 조회)
- Processor: `PlanOnGroupToSubscriptionProcessor`
  - product.code 매핑: FREE→PLAN_FREE, BASIC→PLAN_BASIC, STANDARD→PLAN_STANDARD, BUSINESS→PLAN_BUSINESS
  - 만료된 플랜(end_date < now): status = EXPIRED
  - 해지된 플랜: status = CANCELLED
- Writer: `JpaItemWriter<Subscription>`

#### 3. CardInfoOnGroup → billing_key 매핑

| CardInfoOnGroup 필드 | billing_key 필드 | 변환 규칙 |
|--------------------|-----------------|----------|
| group_id | workspace_id | 직접 매핑 |
| billing_key | billing_key_value | **재암호화** (기존 암호화 → 복호화 → 신규 암호화) |
| card_company | card_company | 직접 매핑 |
| card_number | card_number_masked | 마스킹 형식 확인/변환 (****-****-****-1234) |
| email | email | 직접 매핑 |
| - | is_primary | 1 (workspace당 1개이므로 기본 카드) |
| - | gateway | 'TOSS' |

**암호화 마이그레이션**:
- 기존 CardInfoOnGroup의 billing_key는 이미 암호화되어 있음
- 동일한 암호화 키/방식을 사용하면 직접 복사 가능
- 암호화 방식이 변경되는 경우: 기존 키로 복호화 → 신규 키로 재암호화
- **주의**: 복호화된 빌링키가 로그에 남지 않도록 처리

**배치 처리**:
- Reader: `JdbcPagingItemReader<CardInfoOnGroup>`
- Processor: `CardInfoToBillingKeyProcessor` (암호화 마이그레이션 포함)
- Writer: `JpaItemWriter<BillingKey>`

#### 4. CreditOnGroup → credit_balance 매핑

| CreditOnGroup 필드 | credit_balance 필드 | 변환 규칙 |
|-------------------|-------------------|----------|
| group_id | workspace_id | 직접 매핑 |
| - | credit_type | 'SMS' (현재 SMS만 존재) |
| point_balance | balance | 직접 매핑 |
| - | version | 0 |

**배치 처리**:
- Reader: `JdbcPagingItemReader<CreditOnGroup>`
- Processor: `CreditOnGroupToBalanceProcessor`
- Writer: `JpaItemWriter<CreditBalance>`

#### Flyway Seed 스크립트

```
greeting-db-schema/
└── payment-server/
    └── migration/
        └── V8__seed_product_catalog.sql
```

#### 배치 설정
```
chunk-size: 500
skip-limit: 50
retry-limit: 3
Job 순서: Seed 데이터 (Flyway) → PlanOnGroup → CardInfoOnGroup → CreditOnGroup
```

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-db-schema | payment-server/migration | V8__seed_product_catalog.sql | 신규 |
| greeting_payment-server | batch/job | PlanOnGroupMigrationJob.kt | 신규 |
| greeting_payment-server | batch/job | CardInfoMigrationJob.kt | 신규 |
| greeting_payment-server | batch/job | CreditOnGroupMigrationJob.kt | 신규 |
| greeting_payment-server | batch/processor | PlanOnGroupToSubscriptionProcessor.kt | 신규 |
| greeting_payment-server | batch/processor | CardInfoToBillingKeyProcessor.kt | 신규 |
| greeting_payment-server | batch/processor | CreditOnGroupToBalanceProcessor.kt | 신규 |
| greeting_payment-server | batch/mapper | PlanTypeToProductCodeMapper.kt | 신규 |
| greeting_payment-server | batch/crypto | BillingKeyEncryptionMigrator.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T5-01 | Seed 데이터 투입 | 빈 product 테이블 | V8 Flyway 실행 | product 9건, product_metadata 9건, product_price 최소 9건 생성됨 |
| T5-02 | PlanOnGroup BASIC → Subscription | PlanOnGroup(plan_type=BASIC, active) | process() | Subscription(product_id=PLAN_BASIC의 id, status=ACTIVE) |
| T5-03 | PlanOnGroup 만료 건 | PlanOnGroup(end_date < now) | process() | Subscription(status=EXPIRED) |
| T5-04 | PlanOnGroup 월간/연간 구분 | PlanOnGroup(billing_cycle=YEARLY) | process() | Subscription(billing_interval_months=12) |
| T5-05 | CardInfoOnGroup → BillingKey | CardInfoOnGroup(billing_key=암호화값) | process() | BillingKey(billing_key_value=암호화값, is_primary=1, gateway=TOSS) |
| T5-06 | CardInfo 마스킹 형식 | CardInfoOnGroup(card_number=1234567890121234) | process() | card_number_masked = '****-****-****-1234' |
| T5-07 | CreditOnGroup → CreditBalance | CreditOnGroup(point_balance=5000) | process() | CreditBalance(credit_type=SMS, balance=5000, version=0) |
| T5-08 | 전체 매핑 Job 실행 | PlanOnGroup 50건, CardInfo 40건, Credit 50건 | 3개 Job 순차 실행 | subscription 50건, billing_key 40건, credit_balance 50건 |
| T5-09 | Product code 유일성 | Seed 실행 완료 | product.code 중복 확인 | 모든 code 유일함 |
| T5-10 | 연간 가격 Seed | PLAN_BASIC 연간 가격 | product_price 조회 | billing_interval_months=12인 가격 레코드 존재 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T5-E01 | PlanOnGroup 알 수 없는 plan_type | plan_type이 매핑 테이블에 없는 값 | process() | 에러 로그 + 스킵 |
| T5-E02 | CardInfo 빈 billing_key | billing_key가 NULL/빈 문자열 | process() | 에러 로그 + 스킵 |
| T5-E03 | Credit 잔액 음수 | point_balance < 0 | process() | balance = 0으로 보정, 경고 로그 |
| T5-E04 | 중복 실행 방지 - Subscription | 이미 매핑된 workspace의 구독 존재 | 재실행 | UNIQUE 제약(workspace_id, status)으로 중복 방지 |
| T5-E05 | 빌링키 암호화 실패 | 암호화 키 오류 | 재암호화 시도 | 에러 로그 + 스킵 (빌링키 평문 노출 방지) |
| T5-E06 | Seed 중복 실행 | V8 이미 적용 완료 | Flyway 재실행 | 스킵 (Flyway 멱등성) |
| T5-E07 | PlanOnGroup 없는 workspace | CardInfo는 있지만 PlanOnGroup 없는 workspace | CardInfo Job 실행 | billing_key는 정상 생성 (독립적) |

## 기대 결과 (AC)
- [ ] 상품 카탈로그 Seed 데이터 투입됨 (product 9건 + 메타데이터 + 가격)
- [ ] PLAN_FREE, PLAN_BASIC, PLAN_STANDARD, PLAN_BUSINESS 상품이 SUBSCRIPTION 타입으로 등록됨
- [ ] SMS_PACK_100/500/1000/5000/10000 상품이 CONSUMABLE 타입으로 등록됨
- [ ] PlanOnGroup 전체 건수가 subscription 테이블에 매핑됨 (상태 구분: ACTIVE/EXPIRED/CANCELLED)
- [ ] CardInfoOnGroup → billing_key 이관 시 암호화가 유지/재적용됨
- [ ] 빌링키 평문이 로그에 노출되지 않음
- [ ] CreditOnGroup → credit_balance 매핑됨 (credit_type=SMS)
- [ ] 소스/타겟 건수 일치 확인 (정상 처리 + 스킵 = 소스 건수)
- [ ] product_price에 월간/연간 가격이 모두 등록됨 (구독 상품)
