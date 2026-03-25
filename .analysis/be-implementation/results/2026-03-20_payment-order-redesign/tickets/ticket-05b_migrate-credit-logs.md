# [Ticket #5b] 크레딧 이력 배치 이관 (MongoDB → MySQL)

## 개요
- TDD 참조: tdd.md 섹션 5.3 (Phase C)
- 선행 티켓: #4-1
- 크기: M

## 작업 내용

### 이관 대상

```mermaid
flowchart LR
    subgraph Source["MongoDB (Source)"]
        POINT_LOG["MessagePointLogsOnWorkspace<br/>• workspaceId<br/>• type (PAYMENT/CREDIT/USE/DELETE/EXPIRE/REFUND)<br/>• creditPoint / paymentPoint<br/>• smsCount / lmsCount"]
        CHARGE_LOG["MessagePointChargeLogsOnWorkspace<br/>• workspaceId<br/>• type (PAYMENT/CREDIT)<br/>• amount / rest<br/>• expiredAt"]
    end

    subgraph Transform["변환"]
        CONV1["PointLogToLedgerConverter<br/>(#4-3에서 구현한 것 재사용)"]
        CONV2["ChargeLogToLedgerConverter<br/>(#4-4에서 구현한 것 재사용)"]
    end

    subgraph Target["MySQL (Target)"]
        LEDGER["credit_ledger<br/>• credit_type = SMS<br/>• transaction_type = CHARGE/USE/REFUND/EXPIRE/GRANT<br/>• amount (양수=충전, 음수=사용)<br/>• balance_after<br/>• expired_at (충전 건)"]
    end

    POINT_LOG --> CONV1 --> LEDGER
    CHARGE_LOG --> CONV2 --> LEDGER

    style Source fill:#c8e6c9
    style Target fill:#bbdefb
```

### 배치 설계

2개 Step을 순차 실행한다: 충전 이력 먼저 → 사용 이력 나중에 (balance_after 계산 순서 보장).

```mermaid
flowchart LR
    START([시작]) --> STEP1["Step 1: 충전 이력 이관<br/>MessagePointChargeLogsOnWorkspace<br/>→ credit_ledger (CHARGE/GRANT)"]
    STEP1 --> STEP2["Step 2: 사용 이력 이관<br/>MessagePointLogsOnWorkspace<br/>→ credit_ledger (USE/REFUND/EXPIRE)"]
    STEP2 --> VERIFY["이관 건수 리포트"]
    VERIFY --> DONE([완료])

    style STEP1 fill:#c8e6c9
    style STEP2 fill:#bbdefb
```

### Step별 처리 흐름

```mermaid
flowchart LR
    READ["MongoDB 조회<br/>createdAt < cutoff<br/>chunk 500건"] --> CHECK{"중복<br/>확인"}
    CHECK -->|있음| SKIP[스킵] --> NEXT
    CHECK -->|없음| CONVERT[Converter 변환]
    CONVERT --> WRITE["credit_ledger INSERT"]
    WRITE --> NEXT{다음?}
    NEXT -->|있음| READ
    NEXT -->|끝| DONE([Step 완료])

    style SKIP fill:#e0e0e0
```

### 코드 예시

```kotlin
@Bean
fun creditLogMigrationJob(): Job = jobBuilderFactory.get("creditLogMigrationJob")
    .start(chargeLogMigrationStep())   // Step 1: 충전 먼저
    .next(pointLogMigrationStep())     // Step 2: 사용 나중에
    .build()
```

### 수정 파일 목록

| 레포 | 파일 경로 | 변경 유형 |
|------|----------|----------|
| greeting_payment-server | batch/CreditLogMigrationJobConfig.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-01 | 충전 이력 이관 | ChargeLog 50건 | Step 1 실행 | credit_ledger에 CHARGE/GRANT 50건 |
| TC-02 | 사용 이력 이관 | PointLog 200건 | Step 2 실행 | credit_ledger에 USE/REFUND/EXPIRE 200건 |
| TC-03 | 만료일 매핑 | ChargeLog.expiredAt 존재 | 이관 | credit_ledger.expired_at 정확히 매핑 |
| TC-04 | 순차 실행 보장 | 충전+사용 혼재 | Job 실행 | Step 1(충전) 완료 후 Step 2(사용) 실행 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-E01 | 변환 실패 건 스킵 | 비정상 PointLog 1건 | 배치 실행 | 스킵, 나머지 계속 |
| TC-E02 | 빈 컬렉션 | 두 컬렉션 모두 0건 | 배치 실행 | 정상 완료, 이관 0건 |

## 기대 결과 (AC)
- [ ] 충전 이력(ChargeLog)이 credit_ledger에 CHARGE/GRANT로 이관됨
- [ ] 사용 이력(PointLog)이 credit_ledger에 USE/REFUND/EXPIRE로 이관됨
- [ ] 충전 → 사용 순서로 실행되어 balance_after 계산 순서 보장
- [ ] expired_at이 정확히 매핑됨
- [ ] 중복 이관 방지
