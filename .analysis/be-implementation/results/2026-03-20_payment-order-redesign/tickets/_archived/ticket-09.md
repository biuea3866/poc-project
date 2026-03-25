# [Ticket #9] Payment 도메인 — PG 추상화

## 개요
- TDD 참조: tdd.md 섹션 3.4, 4.1.3, 4.2 (domain/payment/), 4.3, 8.4
- 선행 티켓: #2
- 크기: L

## 작업 내용

### 변경 사항

1. **PaymentStatus enum + 상태 전이 규칙**
   - `REQUESTED` → `APPROVED`, `FAILED`
   - `APPROVED` → `CANCEL_REQUESTED`
   - `CANCEL_REQUESTED` → `CANCELLED`, `CANCEL_FAILED`
   - `canTransitionTo(target: PaymentStatus): Boolean` 메서드
   - 잘못된 전이 시 `InvalidPaymentStatusTransitionException`

2. **PaymentMethod enum**
   - `BILLING_KEY`: 빌링키 자동 결제
   - `CARD`: 카드 직접 결제
   - `TRANSFER`: 계좌이체
   - `MANUAL`: 백오피스 수동 처리

3. **Payment entity 구현**
   - `orderId`: 연관 주문
   - `paymentKey`: PG 결제 키
   - `paymentMethod`: PaymentMethod
   - `gateway`: PG 이름 (TOSS, MANUAL)
   - `status`: PaymentStatus (기본 REQUESTED)
   - `amount`: 결제 금액
   - `receiptUrl`: 영수증 URL
   - `failureCode`, `failureMessage`: 실패 정보
   - `approvedAt`, `cancelledAt`: 승인/취소 시각
   - `idempotencyKey`: 멱등성 키 (UNIQUE)
   - BaseEntity 상속
   - 상태 전이 메서드: `approve()`, `fail()`, `requestCancel()`, `cancel()`, `cancelFailed()`

4. **PaymentResult VO**
   ```kotlin
   data class PaymentResult(
       val success: Boolean,
       val paymentKey: String?,
       val receiptUrl: String?,
       val approvedAt: LocalDateTime?,
       val failureCode: String?,
       val failureMessage: String?,
       val rawResponse: String?,  // PG 원본 응답 JSON
   )
   ```

5. **PaymentGateway 인터페이스**
   ```kotlin
   interface PaymentGateway {
       val gatewayName: String
       fun chargeByBillingKey(billingKey: String, orderId: String, amount: Int, orderName: String): PaymentResult
       fun confirmPayment(paymentKey: String, orderId: String, amount: Int): PaymentResult
       fun cancelPayment(paymentKey: String, cancelAmount: Int, cancelReason: String): PaymentResult
   }
   ```

6. **PaymentRepository 구현**
   - `findByOrderId(orderId: Long): List<Payment>`
   - `findByPaymentKey(paymentKey: String): Payment?`
   - `findByIdempotencyKey(key: String): Payment?`

7. **PaymentService 구현**
   - `processPayment(orderId: Long, paymentMethod: PaymentMethod, gateway: String?)`:
     1. Order 상태 → PENDING_PAYMENT
     2. BillingKey 조회 (BILLING_KEY 방식인 경우)
     3. Payment 생성 (status=REQUESTED)
     4. PaymentGateway 선택 (gateway 이름으로 매칭)
     5. PG 호출 (chargeByBillingKey 또는 confirmPayment)
     6. 성공: Payment → APPROVED, Order → PAID
     7. 실패: Payment → FAILED, Order → PAYMENT_FAILED
   - `cancelPayment(orderId: Long, cancelReason: String)`:
     1. Payment 조회 (APPROVED 상태)
     2. Payment → CANCEL_REQUESTED
     3. PG cancelPayment() 호출
     4. 성공: Payment → CANCELLED, Order → REFUNDED
     5. 실패: Payment → CANCEL_FAILED (수동 처리 필요 알림)
   - `handleWebhook(provider: String, eventType: String, paymentKey: String, payload: String)`:
     1. pg_webhook_log INSERT (RECEIVED)
     2. 중복 체크 (provider + paymentKey + eventType UNIQUE)
     3. Payment 조회 및 상태 반영
     4. pg_webhook_log → PROCESSED

8. **보상 트랜잭션 (Compensation)**
   - PAID 상태에서 fulfillment(상품 지급) 실패 시:
     1. 자동으로 `cancelPayment()` 호출
     2. Order → PAYMENT_FAILED 또는 CANCELLED
     3. 보상 실패 시 로그 + 알림 (수동 처리 대상)

9. **PaymentGateway 선택 전략**
   - `List<PaymentGateway>`를 주입받아 `gatewayName`으로 매칭
   - 매칭되는 gateway 없으면 `UnsupportedGatewayException`

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/payment/PaymentStatus.kt | 신규 |
| greeting_payment-server | domain | domain/payment/PaymentMethod.kt | 신규 |
| greeting_payment-server | domain | domain/payment/Payment.kt | 신규 |
| greeting_payment-server | domain | domain/payment/PaymentGateway.kt | 신규 |
| greeting_payment-server | domain | domain/payment/PaymentResult.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/InvalidPaymentStatusTransitionException.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/PaymentNotFoundException.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/UnsupportedGatewayException.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/PaymentFailedException.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/PaymentRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/PgWebhookLogRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/entity/PgWebhookLog.kt | 신규 |
| greeting_payment-server | application | application/PaymentService.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T9-01 | 빌링키 결제 성공 | Order(CREATED), BillingKey 존재 | processPayment(orderId, BILLING_KEY) | Payment(APPROVED), Order(PAID) |
| T9-02 | 카드 직접 결제 성공 | Order(CREATED), paymentKey 제공 | processPayment(orderId, CARD) | Payment(APPROVED), Order(PAID) |
| T9-03 | 결제 취소 성공 | Payment(APPROVED) | cancelPayment(orderId, "고객 요청") | Payment(CANCELLED) |
| T9-04 | 웹훅 수신 및 처리 | 유효한 웹훅 페이로드 | handleWebhook("TOSS", "PAYMENT_DONE", paymentKey, payload) | pg_webhook_log(PROCESSED) |
| T9-05 | Payment 상태 전이: REQUESTED → APPROVED | Payment(REQUESTED) | approve() | status=APPROVED, approvedAt 설정 |
| T9-06 | Payment 상태 전이: APPROVED → CANCEL_REQUESTED | Payment(APPROVED) | requestCancel() | status=CANCEL_REQUESTED |
| T9-07 | 보상 트랜잭션 — fulfillment 실패 시 자동 취소 | Payment(APPROVED), fulfillment 예외 | 보상 로직 실행 | cancelPayment() 호출됨 |
| T9-08 | Gateway 선택 — TOSS | gateway="TOSS" | processPayment() | TossPaymentGateway 사용 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T9-E01 | PG 결제 실패 | PG가 거절 응답 | processPayment() | Payment(FAILED), Order(PAYMENT_FAILED) |
| T9-E02 | PG 취소 실패 | PG가 취소 거절 | cancelPayment() | Payment(CANCEL_FAILED), 알림 발송 |
| T9-E03 | 잘못된 전이: FAILED → APPROVED | Payment(FAILED) | approve() | InvalidPaymentStatusTransitionException |
| T9-E04 | 중복 웹훅 처리 | 동일 (provider, paymentKey, eventType) 재수신 | handleWebhook() | IGNORED, 중복 무시 |
| T9-E05 | 지원하지 않는 gateway | gateway="UNKNOWN" | processPayment() | UnsupportedGatewayException |
| T9-E06 | BillingKey 없는 워크스페이스 | BillingKey 미등록 | processPayment(BILLING_KEY) | BillingKeyNotFoundException |
| T9-E07 | 보상 트랜잭션도 실패 | fulfillment 실패 + cancelPayment도 실패 | 보상 로직 | 에러 로그 + 수동 처리 알림 |
| T9-E08 | 멱등성 키 중복 Payment | 동일 idempotencyKey | processPayment() | 기존 Payment 반환 |

## 기대 결과 (AC)
- [ ] PaymentStatus enum이 TDD 3.4 상태머신 다이어그램의 모든 전이를 정확히 구현한다
- [ ] PaymentGateway 인터페이스가 chargeByBillingKey, confirmPayment, cancelPayment 3개 메서드를 정의한다
- [ ] PaymentResult VO가 PG 응답의 성공/실패 정보를 캡슐화한다
- [ ] PaymentService.processPayment()가 PG 호출 → Payment/Order 상태 업데이트를 트랜잭션으로 처리한다
- [ ] PG 결제 실패 시 Payment → FAILED, Order → PAYMENT_FAILED로 정확히 전이한다
- [ ] 웹훅 중복 수신 시 멱등하게 처리한다 (pg_webhook_log UNIQUE 인덱스)
- [ ] fulfillment 실패 시 보상 트랜잭션(자동 cancelPayment)이 실행된다
- [ ] 단위 테스트 커버리지 80% 이상
