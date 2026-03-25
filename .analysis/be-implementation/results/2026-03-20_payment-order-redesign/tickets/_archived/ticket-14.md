# [Ticket #14] SubscriptionRenewalScheduler (자동 갱신)

## 개요
- TDD 참조: tdd.md 섹션 4.6 (구독 자동 갱신 시퀀스), 4.2 (infrastructure/), 리스크 6번
- 선행 티켓: #13
- 크기: M

## 작업 내용

### 변경 사항

1. **SubscriptionRenewalScheduler 구현**
   - `@Scheduled` CRON: `0 0 0 * * *` (매일 00:00 KST)
   - Spring `@Component` + `@Scheduled` 사용
   - 실행 시간대: Asia/Seoul (KST)

2. **갱신 대상 조회**
   - 조건: `current_period_end <= tomorrow(내일 자정) AND auto_renew = true AND status IN (ACTIVE, PAST_DUE)`
   - PAST_DUE 포함 이유: 재시도 대상
   - `tomorrow` 기준으로 조회하여 만료 전날 미리 처리

3. **갱신 처리 로직 (각 구독건)**
   ```
   for each subscription:
     1. RENEWAL 타입 Order 생성 (OrderService.createOrder)
     2. 결제 시도 (PaymentService.processPayment)
     3-a. 결제 성공:
       - retryCount = 0
       - currentPeriodStart = 기존 currentPeriodEnd
       - currentPeriodEnd = currentPeriodStart + billingIntervalMonths
       - status = ACTIVE
       - lastOrderId = newOrder.id
       - Order → COMPLETED
     3-b. 결제 실패:
       - retryCount++
       - if retryCount >= 5:
         - status → EXPIRED
         - autoRenew = false
         - Kafka: subscription.expired
       - else:
         - status → PAST_DUE (또는 유지)
         - currentPeriodEnd += 1 day (버퍼)
       - Order → PAYMENT_FAILED 또는 CANCELLED
   ```

4. **Catch-up 로직 (서버 다운타임 보상)**
   - 스케줄러 시작 시 `current_period_end < today` (이미 만료된) 건도 조회
   - 놓친 갱신 건을 소급 처리
   - 소급 처리 시에도 동일한 갱신 로직 적용
   - 로그에 "catch-up renewal" 표시

5. **배치 사이즈 제한**
   - 한 번에 처리할 최대 건수: 설정값 (기본 100건)
   - `@Value("${subscription.renewal.batch-size:100}")`
   - PG 과부하 방지를 위한 건별 처리 간 딜레이: 설정값 (기본 100ms)
   - 총 건수가 배치 사이즈 초과 시 다음 스케줄 실행에서 나머지 처리

6. **에러 핸들링**
   - 개별 구독 갱신 실패가 전체 배치를 중단하지 않음 (try-catch per subscription)
   - 예외 발생 시 해당 건 에러 로그 + 다음 건 계속 처리
   - 스케줄러 전체 실패 시 알림 (모니터링 연동)

7. **로깅 및 모니터링**
   - 스케줄러 시작/종료 INFO 로그
   - 처리 건수 요약: 성공 N건, 실패 N건, 만료 N건
   - 개별 실패 건 WARN 로그 (workspaceId, subscriptionId, 실패 사유)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | infrastructure | infrastructure/scheduler/SubscriptionRenewalScheduler.kt | 신규 |
| greeting_payment-server | config | config/SchedulerConfig.kt | 신규 또는 수정 |
| greeting_payment-server | config | application.yml | 수정 (batch-size, delay 설정 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T14-01 | 정상 갱신 — 1건 | Subscription(ACTIVE, periodEnd=내일, autoRenew=true) | 스케줄러 실행 | RENEWAL Order 생성, 결제 성공, period 갱신 |
| T14-02 | 갱신 대상 0건 | 모든 구독 periodEnd > 내일 | 스케줄러 실행 | 처리 없음, 정상 종료 |
| T14-03 | 복수 건 갱신 | 3건 갱신 대상 | 스케줄러 실행 | 3건 모두 처리, 성공 3건 로그 |
| T14-04 | PAST_DUE 재시도 성공 | Subscription(PAST_DUE, retryCount=2) | 스케줄러 실행, 결제 성공 | status=ACTIVE, retryCount=0 |
| T14-05 | catch-up 처리 | periodEnd=어제 (놓친 건) | 스케줄러 실행 | 소급 갱신 처리, "catch-up" 로그 |
| T14-06 | 배치 사이즈 제한 | 150건 대상, batch-size=100 | 스케줄러 실행 | 100건만 처리 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T14-E01 | 결제 실패 — retryCount < 5 | 결제 실패, retryCount=2 | 스케줄러 실행 | retryCount=3, PAST_DUE, periodEnd += 1일 |
| T14-E02 | 결제 실패 — retryCount 도달 5 | 결제 실패, retryCount=4 | 스케줄러 실행 | retryCount=5, status=EXPIRED, autoRenew=false |
| T14-E03 | 개별 건 예외 — 전체 중단 안 됨 | 2번째 건에서 예외 | 스케줄러 실행 | 1번째/3번째 정상 처리, 2번째 에러 로그 |
| T14-E04 | autoRenew=false 건 제외 | autoRenew=false | 스케줄러 실행 | 갱신 대상에서 제외 |
| T14-E05 | CANCELLED 구독 제외 | status=CANCELLED | 스케줄러 실행 | 갱신 대상에서 제외 |
| T14-E06 | 동시 스케줄러 실행 방지 | 이전 실행이 아직 진행 중 | 스케줄러 트리거 | 중복 실행 방지 (distributed lock 또는 @SchedulerLock) |

## 기대 결과 (AC)
- [ ] 스케줄러가 매일 00:00 KST에 실행되어 갱신 대상을 조회한다
- [ ] ACTIVE 및 PAST_DUE 상태의 auto_renew=true 구독만 갱신 대상이다
- [ ] 갱신 성공 시 retryCount가 0으로 리셋되고 구독 기간이 연장된다
- [ ] 갱신 실패 시 retryCount가 증가하고 periodEnd에 +1일 버퍼가 추가된다
- [ ] retryCount >= 5일 때 구독이 EXPIRED 처리된다
- [ ] 서버 다운타임으로 놓친 갱신 건이 catch-up 로직으로 소급 처리된다
- [ ] 배치 사이즈 제한으로 PG 과부하를 방지한다
- [ ] 개별 건 실패가 전체 배치를 중단하지 않는다
- [ ] 처리 결과 요약 로그가 출력된다 (성공/실패/만료 건수)
