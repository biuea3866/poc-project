# [Ticket #26] 통합 테스트 + MongoDB 제거 + plan-data-processor 폐기

## 개요
- TDD 참조: tdd.md 섹션 5.1 (전체 영향 범위), 5.3 (마이그레이션), 6 (리스크 & 대안)
- 선행 티켓: #17~#25 (All)
- 크기: L

## 작업 내용

### 변경 사항

#### 1. E2E 통합 테스트 시나리오

##### 시나리오 1: 플랜 구독 흐름
```
1. POST /api/v1/orders (productCode=PLAN_STANDARD, orderType=NEW)
2. 내부: Payment 생성 → Toss PG 결제 승인
3. 내부: Subscription 생성 (ACTIVE, period 설정)
4. 검증: Order(COMPLETED), Payment(APPROVED), Subscription(ACTIVE)
5. 검증: subscription.changed.v1 이벤트 발행 (changeType=NEW)
6. 검증: greeting-new-back PlanOnWorkspace 업데이트
```

##### 시나리오 2: 플랜 업그레이드 흐름
```
1. 사전: PLAN_BASIC 구독 활성화 (15일 남음)
2. GET /api/v1/subscriptions/upgrade/preview (targetProductCode=PLAN_STANDARD)
3. 검증: 프로레이션 금액 (환불액, 신규 결제액, 순결제액) 정확성
4. POST /api/v1/subscriptions/upgrade (targetProductCode=PLAN_STANDARD)
5. 검증: 기존 구독 종료 + 새 구독(STANDARD) 활성화
6. 검증: Refund 생성 (잔여 기간 환불)
7. 검증: Order(UPGRADE, COMPLETED), 새 Payment(APPROVED)
8. 검증: subscription.changed.v1 이벤트 (changeType=UPGRADE, previousPlanCode=PLAN_BASIC)
```

##### 시나리오 3: SMS 크레딧 충전 흐름
```
1. 사전: CreditBalance(SMS)=0
2. POST /api/v1/credits/charge (productCode=SMS_PACK_1000)
3. 검증: Order(PURCHASE, COMPLETED), Payment(APPROVED)
4. 검증: CreditLedger (CHARGE, amount=+1000, balanceAfter=1000)
5. 검증: CreditBalance(SMS)=1000
6. 검증: order.completed.v1 이벤트 발행
```

##### 시나리오 4: 크레딧 사용 흐름
```
1. 사전: CreditBalance(SMS)=100
2. 내부 API: CreditService.use(workspaceId, SMS, 10, "SMS 발송")
3. 검증: CreditLedger (USE, amount=-10, balanceAfter=90)
4. 검증: CreditBalance(SMS)=90
```

##### 시나리오 5: 구독 자동 갱신 흐름
```
1. 사전: Subscription(ACTIVE, currentPeriodEnd=tomorrow, autoRenew=true)
2. RenewalScheduler 실행
3. 검증: 새 Order(RENEWAL, COMPLETED), Payment(APPROVED)
4. 검증: Subscription period 갱신 (새 periodStart, periodEnd)
5. 검증: subscription.changed.v1 이벤트 (changeType=RENEWAL)
```

##### 시나리오 6: 갱신 실패 → 만료 흐름
```
1. 사전: Subscription(ACTIVE, autoRenew=true), PG 결제 거절 설정
2. 1차 갱신 시도 → 실패 → retry_count=1, status=PAST_DUE, periodEnd += 1일
3. 2차~4차 시도 → 실패 → retry_count 증가
4. 5차 시도 → 실패 → status=EXPIRED
5. 검증: subscription.changed.v1 이벤트 (changeType=EXPIRE)
6. 검증: Order 5건 (PAYMENT_FAILED)
```

##### 시나리오 7: 웹훅 멱등성 테스트
```
1. 사전: Payment(REQUESTED) 존재
2. Toss 웹훅 (PAYMENT_CONFIRMED) 1차 수신 → 정상 처리
3. 검증: pg_webhook_log(PROCESSED)
4. 동일 웹훅 2차 수신 → 200 OK, 재처리 안 함
5. 검증: pg_webhook_log 추가 행 없음 (UNIQUE 제약)
```

#### 2. MongoDB 의존성 제거 (payment-server)

##### build.gradle 변경
- `spring-boot-starter-data-mongodb` 제거
- `de.flapdoodle.embed.mongo` (테스트용) 제거
- MongoDB 관련 드라이버 의존성 제거

##### 설정 파일 변경
- `application.yml`에서 `spring.data.mongodb` 섹션 제거
- MongoDB 연결 설정 (host, port, database, authentication) 제거
- MongoConfig 클래스 삭제

##### MongoDB Repository/Entity 제거
| 대상 | 파일 | 비고 |
|------|------|------|
| PaymentLogsOnGroup | Entity + Repository | → order + order_item + payment으로 대체 완료 |
| MessagePointLogsOnWorkspace | Entity + Repository | → credit_ledger로 대체 완료 |
| MessagePointChargeLogsOnWorkspace | Entity + Repository | → credit_ledger로 대체 완료 |

##### MongoDB 참조 코드 제거
- Service 레이어에서 MongoDB Repository 참조 제거
- 마이그레이션 검증 완료 후 제거 (데이터 정합성 확인 필수)

#### 3. plan-data-processor 폐기

##### 배포 파이프라인 제거
- CI/CD (GitHub Actions / Jenkins) 빌드 파이프라인에서 plan-data-processor 제거
- Kubernetes/Docker 배포 매니페스트에서 plan-data-processor 서비스 제거
- Helm chart 또는 deployment.yaml 삭제

##### Kafka Consumer Group 정리
- plan-data-processor의 Consumer Group 해제
- 소비 토픽 목록:
  - `event.ats.plan.changed.v1` → greeting-new-back이 대체 소비 (레거시 어댑터 #21 경유)
  - `basic-plan.changed` → 더 이상 소비자 없음 (토픽 유지, 발행 중지)
  - `standard-plan.changed` → 더 이상 소비자 없음 (토픽 유지, 발행 중지)

##### 레포지토리 아카이브
- `greeting_plan-data-processor` GitHub 레포 Archive 처리
- README에 마이그레이션 완료 및 대체 서비스(greeting-new-back) 기록

#### 4. Kafka 토픽 정리

| 토픽 | 액션 | 비고 |
|------|------|------|
| `order.completed.v1` | 유지 (신규) | payment-server 발행, 소비자 확인 |
| `subscription.changed.v1` | 유지 (신규) | payment-server 발행, greeting-new-back 소비 |
| `event.ats.plan.changed.v1` | 유지 | 레거시 어댑터가 발행, 기존 소비자 확인 후 점진 폐기 |
| `basic-plan.changed` | 발행 중지 | plan-data-processor 폐기로 소비자 없음 |
| `standard-plan.changed` | 발행 중지 | plan-data-processor 폐기로 소비자 없음 |
| `cdc.greeting.PlanOnGroup` | 폐기 예정 | #24 Dual-Read 완료 후 Debezium connector 제거 |

#### 5. 모니터링/알림 업데이트

##### 기존 제거
- plan-data-processor 관련 모니터링 대시보드 제거
- plan-data-processor 관련 알림 규칙 제거
- MongoDB 관련 모니터링 (connection pool, query latency 등) 제거

##### 신규 추가
- payment-server 신규 도메인 모니터링:
  - Order 상태별 건수 (CREATED, PAID, COMPLETED, CANCELLED, PAYMENT_FAILED)
  - Payment 성공/실패 비율
  - Subscription 갱신 성공/실패 비율
  - CreditBalance 변동 추이
- Kafka 이벤트 모니터링:
  - order.completed.v1, subscription.changed.v1 발행 건수/지연
  - Consumer lag (greeting-new-back)
- 알림 규칙:
  - Payment 실패율 > 10% → 경고
  - Subscription 갱신 실패 연속 3건 → 경고
  - Kafka Consumer lag > 1000 → 경고
  - 웹훅 처리 실패 건수 > 0 → 알림

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | test | test/integration/OrderFlowIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/SubscriptionUpgradeIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/CreditChargeIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/CreditUsageIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/SubscriptionRenewalIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/RenewalFailureIntegrationTest.kt | 신규 |
| greeting_payment-server | test | test/integration/WebhookIdempotencyIntegrationTest.kt | 신규 |
| greeting_payment-server | - | build.gradle | 수정 (MongoDB 의존성 제거) |
| greeting_payment-server | resources | application.yml | 수정 (MongoDB 설정 제거) |
| greeting_payment-server | infrastructure | infrastructure/config/MongoConfig.kt | 삭제 |
| greeting_payment-server | domain | domain/mongo/PaymentLogsOnGroup.kt | 삭제 |
| greeting_payment-server | domain | domain/mongo/MessagePointLogsOnWorkspace.kt | 삭제 |
| greeting_payment-server | domain | domain/mongo/MessagePointChargeLogsOnWorkspace.kt | 삭제 |
| greeting_payment-server | infrastructure | infrastructure/repository/mongo/ (전체) | 삭제 |
| greeting_payment-server | application | application/PlanService.kt | 수정 (MongoDB 참조 제거) |
| greeting_payment-server | application | application/MessagePointService.kt | 수정 (MongoDB 참조 제거) |
| greeting_plan-data-processor | - | (전체 레포) | 아카이브 |
| greeting-topic | - | topics/ (토픽 설정) | 수정 (basic-plan.changed, standard-plan.changed 발행 중지) |
| infra (deployment) | - | k8s/plan-data-processor/ | 삭제 |
| infra (monitoring) | - | grafana/dashboards/ | 수정 (기존 제거 + 신규 추가) |
| infra (alerting) | - | alertmanager/rules/ | 수정 (기존 제거 + 신규 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T26-01 | 플랜 구독 E2E | 신규 workspace, 빌링키 등록 완료 | 주문 생성 → 결제 → 구독 활성화 | Order(COMPLETED), Payment(APPROVED), Subscription(ACTIVE), 이벤트 발행 |
| T26-02 | 플랜 업그레이드 E2E | Basic 구독 활성 상태 | 업그레이드 프리뷰 → 실행 | 프로레이션 환불 + 새 구독, 이벤트(UPGRADE) 발행 |
| T26-03 | SMS 크레딧 충전 E2E | SMS balance=0 | 충전 주문 → 결제 | Order(COMPLETED), CreditBalance=1000, 이벤트 발행 |
| T26-04 | 크레딧 사용 E2E | SMS balance=100 | 10건 사용 | CreditBalance=90, CreditLedger(USE, -10) |
| T26-05 | 구독 자동 갱신 E2E | periodEnd=tomorrow | 스케줄러 실행 | 갱신 Order(COMPLETED), period 갱신, 이벤트(RENEWAL) |
| T26-06 | 갱신 실패 → 만료 E2E | PG 결제 거절 | 5회 시도 | retry_count=5, Subscription(EXPIRED), 이벤트(EXPIRE) |
| T26-07 | 웹훅 멱등성 E2E | Payment(REQUESTED) | 동일 웹훅 2회 수신 | 1회만 처리, pg_webhook_log 중복 방지 |
| T26-08 | MongoDB 제거 후 빌드 | MongoDB 의존성 제거 | gradle build | 빌드 성공, 모든 기존 테스트 통과 |
| T26-09 | MongoDB 제거 후 기동 | MongoDB 설정 제거 | application 기동 | 정상 기동, MongoDB 연결 시도 없음 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T26-E01 | 결제 실패 시 보상 트랜잭션 | PG 승인 후 구독 활성화 실패 | completeOrder() 예외 | 자동 cancelPayment(), Order(PAYMENT_FAILED) |
| T26-E02 | 동시 업그레이드 요청 | 동일 workspace 2건 동시 업그레이드 | POST /upgrade x2 | 1건 성공, 1건 OptimisticLockException → 재시도 또는 에러 |
| T26-E03 | 크레딧 동시 차감 | balance=10, 동시 2건 각 8 차감 | use(8) x2 동시 | 1건 성공, 1건 InsufficientBalanceException (Optimistic Lock) |
| T26-E04 | plan-data-processor 제거 후 다운그레이드 | plan-data-processor 중지 상태 | 플랜 다운그레이드 | greeting-new-back의 PlanDowngradeEventHandler가 처리 |
| T26-E05 | 레거시 API + 신규 API 병행 | 동일 workspace에서 기존/신규 API 혼용 | /api/plan/current + /api/v1/subscriptions/current | 동일 데이터 반환 |

## 기대 결과 (AC)
- [ ] 7개 E2E 시나리오가 모두 통과한다 (구독, 업그레이드, 크레딧 충전, 크레딧 사용, 자동 갱신, 갱신 실패, 웹훅 멱등성)
- [ ] payment-server에서 MongoDB 의존성(build.gradle, config, entity, repository)이 완전 제거된다
- [ ] MongoDB 제거 후 빌드 및 기동이 정상 동작한다
- [ ] plan-data-processor가 배포 파이프라인에서 제거되고 레포가 아카이브된다
- [ ] greeting-new-back의 PlanDowngradeEventHandler가 plan-data-processor의 기능을 완전 대체한다
- [ ] basic-plan.changed, standard-plan.changed 토픽의 발행이 중지된다
- [ ] 신규 모니터링 대시보드/알림이 구성되어 운영 가시성이 확보된다
- [ ] 레거시 API와 신규 API가 동일한 데이터를 반환한다
