# [Ticket #8] Order 도메인 — 주문 상태머신

## 개요
- TDD 참조: tdd.md 섹션 3.3, 4.1.2, 4.2 (domain/order/), 8.3
- 선행 티켓: #2, #7
- 크기: L

## 작업 내용

### 변경 사항

1. **OrderStatus enum + 상태 전이 규칙**
   - `CREATED` → `PENDING_PAYMENT`, `CANCELLED`
   - `PENDING_PAYMENT` → `PAID`, `PAYMENT_FAILED`
   - `PAYMENT_FAILED` → `PENDING_PAYMENT`, `CANCELLED`
   - `PAID` → `COMPLETED`, `REFUND_REQUESTED`
   - `COMPLETED` → `REFUND_REQUESTED`
   - `REFUND_REQUESTED` → `REFUNDED`
   - `canTransitionTo(target: OrderStatus): Boolean` 메서드로 허용 전이 검증
   - 잘못된 전이 시도 시 `InvalidOrderStatusTransitionException`

2. **OrderType enum**
   - `NEW`: 신규 구매
   - `RENEWAL`: 구독 갱신
   - `UPGRADE`: 상위 플랜 전환
   - `DOWNGRADE`: 하위 플랜 전환
   - `PURCHASE`: 일반 구매 (소진형/일회성)
   - `REFUND`: 환불 주문

3. **Order entity 구현**
   - `orderNumber`: 주문번호 (UNIQUE)
   - `workspaceId`: 주문자 워크스페이스
   - `orderType`: OrderType
   - `status`: OrderStatus (기본 CREATED)
   - `totalAmount`, `originalAmount`, `discountAmount`, `creditAmount`, `vatAmount`
   - `currency`: KRW 고정
   - `idempotencyKey`: 멱등성 키 (UNIQUE, nullable)
   - `memo`, `createdBy`
   - `version`: Optimistic Lock용
   - 상태 전이 메서드: `toPendingPayment()`, `toPaid()`, `toCompleted()`, `toPaymentFailed()`, `toCancelled()`, `toRefundRequested()`, `toRefunded()`
   - 각 전이 메서드 내부에서 `canTransitionTo()` 검증

4. **OrderItem entity 구현**
   - Order와 1:N 관계
   - `productId`, `productCode`, `productName`, `productType`: 주문 시점 스냅샷
   - `quantity`, `unitPrice`, `totalPrice`
   - 스냅샷 전략: 주문 생성 시 Product의 현재 정보를 복사하여 저장

5. **OrderNumberGenerator 구현**
   - 형식: `ORD-{yyyyMMdd}-{UUID 앞 8자리 대문자}`
   - 예: `ORD-20260320-A1B2C3D4`
   - `generate(): String` 메서드

6. **OrderRepository 구현**
   - `findByOrderNumber(orderNumber: String): Order?`
   - `findByIdempotencyKey(key: String): Order?`
   - `findByWorkspaceIdAndStatus(workspaceId: Int, status: OrderStatus): List<Order>`

7. **OrderService 구현**
   - `createOrder(workspaceId, productCode, orderType, idempotencyKey?, createdBy?)`:
     - 멱등성 키 중복 체크 → 기존 주문 반환
     - Product 조회 → 현재 가격 조회
     - Order 생성 (status=CREATED) + OrderItem 생성 (가격 스냅샷)
   - `completeOrder(order: Order)`: ProductType별 분기 처리
     - `SUBSCRIPTION` → FulfillmentStrategy(SubscriptionFulfillment) 위임
     - `CONSUMABLE` → FulfillmentStrategy(CreditFulfillment) 위임
     - `ONE_TIME` → FulfillmentStrategy(OneTimeFulfillment) 위임

8. **FulfillmentStrategy 인터페이스 + 구현체**
   ```kotlin
   interface FulfillmentStrategy {
       fun supports(productType: ProductType): Boolean
       fun fulfill(order: Order)
   }
   ```
   - `SubscriptionFulfillment`: subscriptionService.activateOrRenew(order) 호출
   - `CreditFulfillment`: creditService.grantCredit(order) 호출
   - `OneTimeFulfillment`: order.complete() 후 이벤트 발행 (소비자가 처리)
   - Strategy 선택은 `List<FulfillmentStrategy>`에서 `supports()` 매칭

9. **Idempotency 처리**
   - `idempotency_key` UNIQUE 제약
   - 동일 키로 재요청 시 기존 Order 반환 (새로 생성하지 않음)
   - 키가 없으면 멱등성 체크 스킵

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/order/OrderStatus.kt | 신규 |
| greeting_payment-server | domain | domain/order/OrderType.kt | 신규 |
| greeting_payment-server | domain | domain/order/Order.kt | 신규 |
| greeting_payment-server | domain | domain/order/OrderItem.kt | 신규 |
| greeting_payment-server | domain | domain/order/OrderNumberGenerator.kt | 신규 |
| greeting_payment-server | domain | domain/order/FulfillmentStrategy.kt | 신규 |
| greeting_payment-server | domain | domain/order/exception/InvalidOrderStatusTransitionException.kt | 신규 |
| greeting_payment-server | domain | domain/order/exception/OrderNotFoundException.kt | 신규 |
| greeting_payment-server | domain | domain/order/exception/DuplicateIdempotencyKeyException.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/OrderRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/OrderItemRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/fulfillment/SubscriptionFulfillment.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/fulfillment/CreditFulfillment.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/fulfillment/OneTimeFulfillment.kt | 신규 |
| greeting_payment-server | application | application/OrderService.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T8-01 | 주문 생성 성공 | Product(PLAN_BASIC) 존재, 가격 30000원 | createOrder(ws1, PLAN_BASIC, NEW) | Order(CREATED), OrderItem(unitPrice=30000) |
| T8-02 | 상태 전이: CREATED → PENDING_PAYMENT | Order(CREATED) | toPendingPayment() | status=PENDING_PAYMENT |
| T8-03 | 상태 전이: PENDING_PAYMENT → PAID | Order(PENDING_PAYMENT) | toPaid() | status=PAID |
| T8-04 | 상태 전이: PAID → COMPLETED | Order(PAID) | toCompleted() | status=COMPLETED |
| T8-05 | 주문번호 형식 검증 | - | OrderNumberGenerator.generate() | ORD-yyyyMMdd-[A-Z0-9]{8} 형식 |
| T8-06 | 멱등성 키 중복 → 기존 주문 반환 | Order(idempotencyKey="key1") 존재 | createOrder(..., idempotencyKey="key1") | 기존 Order 반환, 새로 생성 안 함 |
| T8-07 | completeOrder — SUBSCRIPTION | Order(PAID), product.type=SUBSCRIPTION | completeOrder(order) | SubscriptionFulfillment.fulfill() 호출됨 |
| T8-08 | completeOrder — CONSUMABLE | Order(PAID), product.type=CONSUMABLE | completeOrder(order) | CreditFulfillment.fulfill() 호출됨 |
| T8-09 | completeOrder — ONE_TIME | Order(PAID), product.type=ONE_TIME | completeOrder(order) | order.status=COMPLETED |
| T8-10 | OrderItem 가격 스냅샷 | Product 가격 30000원 | createOrder() 후 가격 50000원으로 변경 | OrderItem.unitPrice=30000 유지 |
| T8-11 | 환불 요청 전이 | Order(COMPLETED) | toRefundRequested() | status=REFUND_REQUESTED |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T8-E01 | 잘못된 전이: CREATED → PAID | Order(CREATED) | toPaid() | InvalidOrderStatusTransitionException |
| T8-E02 | 잘못된 전이: COMPLETED → PAID | Order(COMPLETED) | toPaid() | InvalidOrderStatusTransitionException |
| T8-E03 | 잘못된 전이: CANCELLED → 모든 상태 | Order(CANCELLED) | 아무 전이 | InvalidOrderStatusTransitionException |
| T8-E04 | 존재하지 않는 상품으로 주문 | productCode="INVALID" | createOrder() | ProductNotFoundException |
| T8-E05 | 비활성 상품으로 주문 | Product(isActive=false) | createOrder() | ProductNotFoundException 또는 별도 예외 |
| T8-E06 | PAYMENT_FAILED → CANCELLED (최종 실패) | Order(PAYMENT_FAILED) | toCancelled() | status=CANCELLED |
| T8-E07 | Fulfillment 실패 시 보상 트랜잭션 | PAID 상태에서 fulfill() 예외 | completeOrder(order) | 보상 로직 트리거 (PaymentService.cancelPayment) |

## 기대 결과 (AC)
- [ ] OrderStatus enum이 TDD 3.3 상태머신 다이어그램의 모든 전이를 정확히 구현한다
- [ ] canTransitionTo()가 허용되지 않는 전이에 대해 false를 반환한다
- [ ] OrderNumberGenerator가 `ORD-{yyyyMMdd}-{UUID 8자리}` 형식의 유니크 번호를 생성한다
- [ ] OrderItem이 주문 시점의 상품 정보(코드, 이름, 타입, 가격)를 스냅샷으로 저장한다
- [ ] idempotency_key 중복 시 기존 Order를 반환하고 새로 생성하지 않는다
- [ ] completeOrder()가 ProductType에 따라 올바른 FulfillmentStrategy를 선택하여 실행한다
- [ ] PAID 상태에서 상품 지급(fulfillment) 실패 시 보상 트랜잭션이 트리거된다
- [ ] 단위 테스트 커버리지 80% 이상
