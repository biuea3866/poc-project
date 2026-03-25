# [Ticket #22] 이력 관리 (OrderStatusHistory, PaymentStatusHistory)

## 개요
- TDD 참조: tdd.md 섹션 4.1.6 (order_status_history, payment_status_history 스키마), 3.3 (Order 상태 머신), 3.4 (Payment 상태 머신)
- 선행 티켓: #8, #9
- 크기: S

## 작업 내용

### 변경 사항

#### 1. OrderStatusHistory 자동 기록
- Order 엔티티의 상태(status) 전이 시 자동으로 order_status_history에 INSERT
- 기록 항목: orderId, fromStatus, toStatus, changedBy, reason, createdAt
- **수동 INSERT 금지** — 상태 머신 전이 메서드 내에서만 생성

#### 2. PaymentStatusHistory 자동 기록
- Payment 엔티티의 상태(status) 전이 시 자동으로 payment_status_history에 INSERT
- 기록 항목: paymentId, fromStatus, toStatus, pgResponse (PG 원본 응답 JSON), createdAt
- PG 응답 원본(PaymentResult.rawResponse)을 pgResponse 필드에 저장

#### 3. 구현 방식: 도메인 이벤트 리스너
- Order/Payment 엔티티에서 상태 변경 시 도메인 이벤트 발행:
  - `OrderStatusChangedEvent(orderId, fromStatus, toStatus, changedBy, reason)`
  - `PaymentStatusChangedEvent(paymentId, fromStatus, toStatus, pgResponse)`
- `@EventListener` 또는 `@TransactionalEventListener(BEFORE_COMMIT)`로 이력 INSERT
  - BEFORE_COMMIT: 동일 트랜잭션 내에서 이력 저장 보장
  - 이력 저장 실패 시 원래 상태 변경도 롤백

- 대안(AOP):
  - `@Around` 어드바이스로 Order/Payment의 상태 변경 메서드 감지
  - 도메인 이벤트 방식이 더 명확하므로 이벤트 리스너 방식 우선 권장

#### 4. 이력 엔티티 구현
- `OrderStatusHistory` JPA 엔티티 (#2에서 기본 생성, 이 티켓에서 리스너 연결)
- `PaymentStatusHistory` JPA 엔티티

#### 5. 상태 전이 메서드에 이벤트 발행 추가
- Order 엔티티의 상태 전이 메서드들:
  - `toPendingPayment()`, `toPaid()`, `toCompleted()`, `toCancelled()`, `toRefundRequested()`, `toRefunded()`, `toPaymentFailed()`
  - 각 메서드에서 `registerEvent(OrderStatusChangedEvent(...))` 호출
- Payment 엔티티의 상태 전이 메서드들:
  - `approve(pgResponse)`, `fail(pgResponse)`, `requestCancel()`, `cancel(pgResponse)`, `cancelFail(pgResponse)`
  - 각 메서드에서 `registerEvent(PaymentStatusChangedEvent(...))` 호출

#### 6. AbstractAggregateRoot 활용
- Spring Data의 `AbstractAggregateRoot`를 Order/Payment 엔티티에 적용
- `registerEvent()`로 도메인 이벤트 등록, JPA save() 시 자동 발행

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/order/Order.kt | 수정 (AbstractAggregateRoot 상속, 상태 전이 시 이벤트 등록) |
| greeting_payment-server | domain | domain/order/event/OrderStatusChangedEvent.kt | 신규 |
| greeting_payment-server | domain | domain/payment/Payment.kt | 수정 (AbstractAggregateRoot 상속, 상태 전이 시 이벤트 등록) |
| greeting_payment-server | domain | domain/payment/event/PaymentStatusChangedEvent.kt | 신규 |
| greeting_payment-server | domain | domain/history/OrderStatusHistory.kt | 수정 (엔티티 보완) |
| greeting_payment-server | domain | domain/history/PaymentStatusHistory.kt | 수정 (엔티티 보완) |
| greeting_payment-server | application | application/listener/OrderStatusHistoryListener.kt | 신규 |
| greeting_payment-server | application | application/listener/PaymentStatusHistoryListener.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/OrderStatusHistoryRepository.kt | 수정 (조회 메서드 추가) |
| greeting_payment-server | infrastructure | infrastructure/repository/PaymentStatusHistoryRepository.kt | 수정 (조회 메서드 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T22-01 | 주문 생성 시 이력 기록 | 신규 주문 | Order 생성 (status=CREATED) | order_status_history: fromStatus=null, toStatus=CREATED |
| T22-02 | 주문 상태 전이 시 이력 기록 | CREATED 상태 Order | toPendingPayment() | order_status_history: fromStatus=CREATED, toStatus=PENDING_PAYMENT |
| T22-03 | 결제 승인 시 이력 기록 | REQUESTED 상태 Payment | approve(pgResponse) | payment_status_history: fromStatus=REQUESTED, toStatus=APPROVED, pgResponse 저장 |
| T22-04 | 결제 실패 시 PG 응답 저장 | REQUESTED 상태 Payment | fail(pgResponse) | payment_status_history: pgResponse에 PG 에러 코드/메시지 포함 |
| T22-05 | 전체 주문 흐름 이력 | 주문 생성 → 결제 → 완료 | 전체 플로우 실행 | order_status_history 4건: CREATED → PENDING_PAYMENT → PAID → COMPLETED |
| T22-06 | 환불 흐름 이력 | COMPLETED 주문 → 환불 | 환불 요청 → 환불 완료 | order_status_history: COMPLETED → REFUND_REQUESTED → REFUNDED |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T22-E01 | 이력 저장 실패 시 롤백 | DB 제약 조건 위반 | 상태 전이 시도 | 이력 저장 실패 → 상태 변경도 함께 롤백 |
| T22-E02 | 잘못된 상태 전이 시 이력 미생성 | COMPLETED 상태 Order | toPendingPayment() 호출 | OrderStatusTransitionException, 이력 레코드 생성 안 됨 |
| T22-E03 | PG 응답 null | 수동 결제 (ManualPaymentGateway) | approve(null) | payment_status_history: pgResponse=null 허용 |
| T22-E04 | PG 응답 대용량 | rawResponse가 매우 긴 JSON | approve(largePgResponse) | TEXT 컬럼이므로 정상 저장 |

## 기대 결과 (AC)
- [ ] Order 상태가 변경될 때마다 order_status_history에 이력이 자동 기록된다
- [ ] Payment 상태가 변경될 때마다 payment_status_history에 이력이 자동 기록된다 (PG 원본 응답 포함)
- [ ] 이력 기록은 수동 INSERT 없이 상태 머신 전이 메서드 호출 시 자동으로 발생한다
- [ ] 이력 저장은 상태 변경과 동일 트랜잭션에서 처리되어 원자성이 보장된다
- [ ] 잘못된 상태 전이 시에는 이력이 생성되지 않는다
- [ ] AbstractAggregateRoot를 활용하여 도메인 이벤트 기반으로 깔끔하게 구현된다
