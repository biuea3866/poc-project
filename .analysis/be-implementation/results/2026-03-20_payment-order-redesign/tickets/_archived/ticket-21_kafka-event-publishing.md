# [Ticket #21] Kafka 이벤트 발행 (order.completed, subscription.changed)

## 개요
- TDD 참조: tdd.md 섹션 4.2 (infrastructure/event), 5.2 (Kafka 이벤트 변경), 8.4 (Kafka 이벤트 설계 결정)
- 선행 티켓: #8, #13
- 크기: M

## 작업 내용

### 변경 사항

#### 1. 신규 Kafka 토픽 정의
- `order.completed.v1` — 주문 완료 이벤트
- `subscription.changed.v1` — 구독 변경 이벤트

#### 2. OrderCompletedEvent 스키마 및 발행
```
OrderCompletedEvent {
  eventId: String (UUID)
  eventType: "ORDER_COMPLETED"
  timestamp: Instant
  orderId: Long
  orderNumber: String
  workspaceId: Int
  productCode: String
  productType: String (SUBSCRIPTION | CONSUMABLE | ONE_TIME)
  orderType: String (NEW | RENEWAL | UPGRADE | DOWNGRADE | PURCHASE | REFUND)
  amount: Int
  status: String
}
```
- OrderService.completeOrder() 호출 시 발행
- 모든 상품 유형(SUBSCRIPTION, CONSUMABLE, ONE_TIME) 주문 완료 시 발행

#### 3. SubscriptionChangedEvent 스키마 및 발행
```
SubscriptionChangedEvent {
  eventId: String (UUID)
  eventType: "SUBSCRIPTION_CHANGED"
  timestamp: Instant
  workspaceId: Int
  planCode: String (PLAN_BASIC | PLAN_STANDARD | PLAN_FREE)
  planName: String
  status: String (ACTIVE | PAST_DUE | CANCELLED | EXPIRED)
  periodStart: Instant
  periodEnd: Instant
  billingIntervalMonths: Int
  changeType: String (NEW | RENEWAL | UPGRADE | DOWNGRADE | CANCEL | EXPIRE)
  previousPlanCode: String? (업/다운그레이드 시)
}
```
- SubscriptionService에서 구독 상태 변경 시 발행:
  - activateOrRenew() → changeType=NEW/RENEWAL
  - upgrade() → changeType=UPGRADE
  - downgrade 적용 시 → changeType=DOWNGRADE
  - cancel() → changeType=CANCEL
  - expire() → changeType=EXPIRE

#### 4. 레거시 어댑터 (하위호환)
- `subscription.changed.v1` 발행 시 레거시 토픽에도 동시 발행:
  - `event.ats.plan.changed.v1` — plan-data-processor 호환
  - `basic-plan.changed` — Basic 플랜 변경 시
  - `standard-plan.changed` — Standard 플랜 변경 시
- LegacyEventAdapter 클래스에서 SubscriptionChangedEvent → 기존 이벤트 스키마로 변환
- 기존 이벤트 스키마: plan-data-processor가 소비하는 현재 형식 유지

#### 5. Transactional Outbox 패턴 고려
- 현 단계: ApplicationEventPublisher + @TransactionalEventListener(AFTER_COMMIT) 방식
  - DB 커밋 성공 후 Kafka 발행
  - 발행 실패 시 로그 기록 + 재시도 (Spring Retry)
- 향후 고도화: outbox 테이블 기반 패턴으로 전환 가능하도록 EventPublisher 인터페이스 추상화
  - KafkaEventPublisher implements EventPublisher
  - OutboxEventPublisher implements EventPublisher (향후)

#### 6. 이벤트 발행 구현
- OrderEventPublisher: OrderCompletedEvent → order.completed.v1 토픽 발행
- SubscriptionEventPublisher: SubscriptionChangedEvent → subscription.changed.v1 + 레거시 토픽 발행
- Kafka key: workspaceId (파티션 정렬 보장)
- 직렬화: JSON (Jackson)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | infrastructure | infrastructure/event/OrderEventPublisher.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/event/SubscriptionEventPublisher.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/event/LegacyEventAdapter.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/event/EventPublisher.kt | 신규 (인터페이스) |
| greeting_payment-server | domain | domain/event/OrderCompletedEvent.kt | 신규 |
| greeting_payment-server | domain | domain/event/SubscriptionChangedEvent.kt | 신규 |
| greeting_payment-server | domain | domain/event/ChangeType.kt | 신규 (enum) |
| greeting_payment-server | application | application/OrderService.kt | 수정 (이벤트 발행 호출 추가) |
| greeting_payment-server | application | application/SubscriptionService.kt | 수정 (이벤트 발행 호출 추가) |
| greeting_payment-server | infrastructure | infrastructure/config/KafkaProducerConfig.kt | 수정 (신규 토픽 설정) |
| greeting-topic | - | topics/ (토픽 정의 파일) | 수정 (order.completed.v1, subscription.changed.v1 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T21-01 | 주문 완료 이벤트 발행 | Order COMPLETED 상태 전이 | completeOrder() 호출 | order.completed.v1 토픽에 OrderCompletedEvent 발행 |
| T21-02 | 신규 구독 이벤트 발행 | 새 구독 활성화 | activateOrRenew() (NEW) | subscription.changed.v1 + event.ats.plan.changed.v1 토픽 발행, changeType=NEW |
| T21-03 | 구독 갱신 이벤트 발행 | 자동 갱신 처리 | activateOrRenew() (RENEWAL) | subscription.changed.v1 발행, changeType=RENEWAL |
| T21-04 | 업그레이드 이벤트 발행 | Basic → Standard 업그레이드 | upgrade() | subscription.changed.v1 발행, changeType=UPGRADE, previousPlanCode=PLAN_BASIC |
| T21-05 | 레거시 토픽 동시 발행 | Standard 플랜 변경 | subscription.changed.v1 발행 | standard-plan.changed, event.ats.plan.changed.v1에도 동시 발행 |
| T21-06 | 해지 이벤트 발행 | 구독 해지 | cancel() | subscription.changed.v1 발행, changeType=CANCEL |
| T21-07 | 만료 이벤트 발행 | 5회 재시도 실패 후 만료 | expire() | subscription.changed.v1 발행, changeType=EXPIRE |
| T21-08 | Kafka key 파티셔닝 | workspaceId=100 | 이벤트 발행 | Kafka message key = "100" |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T21-E01 | Kafka 브로커 장애 | Kafka 연결 불가 | 이벤트 발행 시도 | Spring Retry로 재시도, 최종 실패 시 에러 로그 기록, DB 트랜잭션은 커밋 유지 |
| T21-E02 | 레거시 어댑터 변환 실패 | SubscriptionChangedEvent → 레거시 스키마 변환 중 NPE | 레거시 발행 시도 | 신규 토픽 발행은 성공, 레거시 발행 실패 로그 기록 |
| T21-E03 | 트랜잭션 롤백 시 이벤트 미발행 | Order 저장 후 후속 처리에서 예외 | 트랜잭션 롤백 | @TransactionalEventListener(AFTER_COMMIT)이므로 이벤트 발행 안 됨 |
| T21-E04 | 이벤트 직렬화 실패 | 잘못된 데이터로 JSON 직렬화 불가 | 이벤트 발행 시도 | SerializationException 로그, DB 트랜잭션 유지 |

## 기대 결과 (AC)
- [ ] 주문 완료 시 order.completed.v1 토픽에 OrderCompletedEvent가 발행된다
- [ ] 구독 상태 변경 시 subscription.changed.v1 토픽에 SubscriptionChangedEvent가 발행된다
- [ ] 레거시 어댑터가 subscription.changed.v1 이벤트를 기존 토픽(event.ats.plan.changed.v1, basic-plan.changed, standard-plan.changed)에도 동시 발행한다
- [ ] Kafka key가 workspaceId로 설정되어 동일 workspace 이벤트의 순서가 보장된다
- [ ] @TransactionalEventListener(AFTER_COMMIT)으로 DB 커밋 성공 후에만 이벤트가 발행된다
- [ ] Kafka 발행 실패 시에도 DB 트랜잭션은 유지되며, 에러 로그가 기록된다
- [ ] EventPublisher 인터페이스가 추상화되어 향후 Outbox 패턴 전환이 가능하다
