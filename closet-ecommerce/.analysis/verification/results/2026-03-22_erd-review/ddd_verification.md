# DDD Tactical Design 검증 보고서

> 검증일: 2026-03-22
> 대상: `ddd_tactical_design.md` + `full_domain_analysis.md`
> 검증자: DDD Expert Reviewer
> 벤치마크: 무신사 (Musinsa)

---

## 총괄 요약

15개 Bounded Context에 대한 전술적 설계를 6개 카테고리(Aggregate 경계, BC 검증, 상태 머신, Domain Event, 비즈니스 규칙, 누락 사항)로 검증했다.

### 판정 결과

| 카테고리 | 위반 | 경고 | 제안 | 판정 |
|---------|------|------|------|------|
| Aggregate 경계 | 2 | 3 | 2 | 주의 |
| Bounded Context | 0 | 3 | 2 | 양호 |
| 상태 머신 | 1 | 2 | 1 | 주의 |
| Domain Event | 1 | 2 | 3 | 주의 |
| 비즈니스 규칙 | 1 | 2 | 2 | 주의 |
| 누락 사항 | - | - | 8 | 보완 필요 |
| **합계** | **5** | **12** | **18** | **조건부 승인** |

### 최종 판정: 조건부 승인 (Conditionally Approved)

전체적으로 도메인 분리, 상태 머신, 이벤트 설계가 충실하게 작성되어 있다. 그러나 몇 가지 Aggregate 크기 문제, 상태 전이 누락, 결제 금액 검증 로직 미흡 등이 있어 수정 후 승인 가능하다.

---

## 1. Aggregate 경계 검증

### 1.1 위반 (VIOLATION)

#### [V-AGG-01] Member Aggregate가 과도하게 크다

**심각도**: 높음

Member Aggregate에 `Point`, `PointHistory`, `SocialAccount`, `ShippingAddress`, `Wishlist`, `MemberGradeHistory`가 모두 포함되어 있다. 이는 단일 Aggregate가 너무 많은 책임을 지고 있으며, 트랜잭션 범위가 불필요하게 넓어진다.

**문제점**:
- Point는 독립적인 Aggregate로 분리해야 한다. 포인트 차감/적립은 회원 정보 수정과 별개의 트랜잭션이다.
- PointHistory가 Member 안에 있으면 회원 조회 시 불필요한 이력 로딩이 발생한다.
- Wishlist는 Member와 생명주기가 다르다 (독립적 CRUD).
- ShippingAddress 목록도 별도 Aggregate 또는 최소한 별도 Repository로 분리 권장.

**수정 제안**:

```kotlin
// Point를 별도 Aggregate로 분리
class PointAccount(
    val id: PointAccountId,
    val memberId: MemberId,
    private var balance: Int,
    private val histories: MutableList<PointHistory> = mutableListOf(),
    var version: Long = 0
) {
    fun earn(amount: Int, reason: String, referenceId: String, expiresAt: LocalDateTime) {
        require(amount > 0) { "적립 금액은 0보다 커야 합니다" }
        balance += amount
        histories.add(PointHistory.earn(amount, balance, reason, referenceId, expiresAt))
    }

    fun use(amount: Int, referenceId: String) {
        require(amount >= 1000) { "최소 사용 단위는 1,000P입니다" }
        require(balance >= amount) { "포인트 잔액이 부족합니다 (잔액: $balance, 요청: $amount)" }
        balance -= amount
        histories.add(PointHistory.use(amount, balance, referenceId))
    }

    fun restore(amount: Int, referenceId: String) {
        require(amount > 0)
        balance += amount
        histories.add(PointHistory.restore(amount, balance, referenceId))
    }

    fun getBalance(): Int = balance
}

// Wishlist를 별도 Aggregate로 분리
class Wishlist(
    val id: WishlistId,
    val memberId: MemberId,
    private val items: MutableList<WishlistItem> = mutableListOf()
) {
    companion object {
        const val MAX_ITEMS = 200
    }

    fun addItem(productId: ProductId, skuId: SkuId?) {
        require(items.size < MAX_ITEMS) { "위시리스트는 최대 ${MAX_ITEMS}개까지 등록 가능합니다" }
        require(items.none { it.productId == productId }) { "이미 위시리스트에 등록된 상품입니다" }
        items.add(WishlistItem(productId = productId, skuId = skuId))
    }
}

// Member는 핵심 회원 정보만 보유
class Member(
    val id: MemberId,
    val email: Email,
    private var password: Password,
    private var name: String,
    private var phone: PhoneNumber,
    private var grade: MemberGrade,
    private val socialAccounts: MutableList<SocialAccount>,
    private val addresses: MutableList<ShippingAddress>,
    var status: MemberStatus,
    // Point, Wishlist 제거 -> 별도 Aggregate
)
```

#### [V-AGG-02] StockHistory가 Inventory Aggregate 외부에 있다

**심각도**: 중간

`Inventory "1" -- "*" StockHistory` 관계가 연관(association)으로 표현되어 있으나, 재고 변경 시 반드시 이력이 기록되어야 한다는 불변 조건이 있다. 따라서 StockHistory는 Inventory Aggregate 내부의 Entity 또는 Aggregate 내에서 생성되어야 한다.

그러나 이력이 무한히 증가하는 구조이므로 Aggregate 내부에 List로 포함시키면 성능 문제가 발생한다.

**수정 제안**:

```kotlin
class Inventory(
    // ... 기존 필드
) {
    // StockHistory는 Aggregate 내부에서 생성하되, 별도 Repository로 저장
    fun reserve(quantity: Int): StockHistory {
        val availableStock = getAvailableStock()
        require(availableStock >= quantity) {
            "가용재고 부족 (가용: $availableStock, 요청: $quantity)"
        }
        val beforeReserved = reservedStock
        reservedStock += quantity
        return StockHistory.of(
            skuId = skuId,
            type = StockChangeType.RESERVE,
            quantity = quantity,
            beforeTotal = totalStock,
            afterTotal = totalStock,
            beforeReserved = beforeReserved,
            afterReserved = reservedStock
        )
    }
}

// Application Service에서 이력 저장
class ReserveStockUseCase(...) {
    @Transactional
    fun execute(command: ReserveStockCommand) {
        val inventory = inventoryRepository.findBySkuIdWithLock(command.skuId)
        val history = inventory.reserve(command.quantity)
        inventoryRepository.save(inventory)
        stockHistoryRepository.save(history)  // 별도 저장
    }
}
```

### 1.2 경고 (WARNING)

#### [W-AGG-01] Product Aggregate 내 SKU 목록이 과도하게 커질 수 있다

Product에 `List<Sku>`가 포함되어 있으며, 의류 특성상 색상 10개 x 사이즈 7개 = 70개 SKU가 가능하다. 이 정도 규모는 허용 가능하나, SKU 각각의 가격 변경 등이 빈번하면 Aggregate 전체 로딩 비용이 증가한다.

**권장**: 현재 구조 유지하되, SKU 수 상한(예: 100개)을 명시적으로 검증하는 불변 조건 추가.

```kotlin
class Product {
    companion object {
        const val MAX_SKUS = 100
    }

    fun addSku(combo: OptionCombination, code: SkuCode): Sku {
        require(skus.size < MAX_SKUS) { "SKU는 최대 ${MAX_SKUS}개까지 등록 가능합니다" }
        require(skus.none { it.optionCombination == combo }) { "동일 옵션 조합이 이미 존재합니다" }
        // ...
    }
}
```

#### [W-AGG-02] Cart와 Order가 동일 Bounded Context에 있다

Cart와 Order는 생명주기와 일관성 요구사항이 매우 다르다. Cart는 eventual consistency로 충분하고, Order는 strong consistency가 필요하다. 동일 BC에 있는 것은 허용 가능하나, 별도 모듈로 분리를 고려할 수 있다.

**권장**: 현재 구조 유지. 향후 트래픽 증가 시 Cart를 Redis 기반 별도 서비스로 분리 가능.

#### [W-AGG-03] CouponPolicy와 Coupon의 Aggregate 분리가 불명확하다

`CouponPolicy "1" -- "*" Coupon` 관계에서 Coupon이 CouponPolicy의 내부 Entity인지 별도 Aggregate인지 불명확하다. 선착순 발급 시 CouponPolicy.issue()에서 Coupon을 생성하는 구조인데, 수만 개의 Coupon이 CouponPolicy Aggregate에 포함되면 성능 문제가 발생한다.

**수정 제안**: Coupon을 별도 Aggregate Root로 명시적 분리.

```kotlin
// CouponPolicy: 쿠폰 정책 Aggregate (발급 수량 관리)
class CouponPolicy(
    val id: CouponPolicyId,
    // ... 정책 필드
    private var issuedQuantity: Int,
    private var remainingQuantity: Int,
) {
    fun validateIssuable(): Unit {
        require(active) { "비활성화된 쿠폰 정책입니다" }
        require(remainingQuantity > 0) { "발급 가능 수량이 없습니다" }
        require(LocalDateTime.now() in issueStartAt..issueEndAt) { "발급 기간이 아닙니다" }
    }

    fun decrementQuantity() {
        require(remainingQuantity > 0)
        remainingQuantity--
        issuedQuantity++
    }
}

// Coupon: 별도 Aggregate Root (회원별 쿠폰 인스턴스)
class Coupon(
    val id: CouponId,
    val policyId: CouponPolicyId,
    val memberId: MemberId,
    private var status: CouponStatus,
    val expiresAt: LocalDateTime,
    // ...
) {
    companion object {
        fun issue(policy: CouponPolicy, memberId: MemberId, validDays: Int): Coupon {
            policy.validateIssuable()
            policy.decrementQuantity()
            return Coupon(
                policyId = policy.id,
                memberId = memberId,
                status = CouponStatus.ISSUED,
                expiresAt = LocalDateTime.now().plusDays(validDays.toLong())
            )
        }
    }
}
```

### 1.3 제안 (SUGGESTION)

#### [S-AGG-01] ReturnRequest를 Shipping과 별도 Aggregate로 분리 고려

ReturnRequest는 Shipment과 다른 생명주기를 가진다. 현재 별도 Aggregate로 되어 있으나 동일 BC에 있으므로 적절하다. 다만 ExchangeRequest도 필요한데, 현재 설계에 명시적 Exchange Aggregate가 없다.

#### [S-AGG-02] Review에서 HelpfulVote 관계가 `"1" -- "*"`인데 Aggregate 경계 외부 참조로 보인다

HelpfulVote는 Review Aggregate 외부에서 관리하되, helpfulCount만 Review 내에 비정규화하여 보관하는 구조가 적절하다. 현재 설계가 이 의도인 것으로 보이나, 명시적으로 문서화 필요.

---

## 2. Bounded Context 검증

### 2.1 경고 (WARNING)

#### [W-BC-01] Order와 Shipping 간 구매확정 책임 중복

- `Order`에 `confirmPurchase()`, `autoConfirmPurchase()` 메서드가 있고
- `Shipping`에도 `Shipment.isAutoConfirmDue()`, `scheduleAutoConfirm()` 메서드가 있다
- `AutoConfirmPurchaseUseCase`가 Order BC에 있고, `ShippingDomainService.findAutoConfirmTargets()`도 Shipping BC에 있다
- `AutoConfirmedEvent`가 Shipping에서 발행되면서 동시에 `PurchaseConfirmedEvent`가 Order에서 발행된다

**문제**: 구매확정의 오케스트레이션 책임이 Order와 Shipping에 분산되어 있다. 어느 쪽이 주도하는지 불명확하다.

**수정 제안**: 구매확정의 주도권을 하나로 통일.

```
방안 A (권장): Shipping이 자동구매확정 판단 -> AutoConfirmedEvent 발행 -> Order가 소비하여 confirmPurchase() 호출
방안 B: Order에서 배송완료일 기반으로 직접 판단 (Shipping 의존 제거)
```

방안 A를 권장한다. Shipping이 배송 상태를 가장 잘 알고 있으므로, 자동구매확정 시점 판단은 Shipping이 하되, 주문 상태 변경은 Order가 이벤트를 수신하여 처리하는 것이 맞다. Order BC의 `AutoConfirmPurchaseUseCase`는 제거하고, Order의 이벤트 핸들러로 대체한다.

#### [W-BC-02] Payment와 Order 간 이벤트 양방향 의존

- Order -> Payment: `OrderCreatedEvent` (간접적으로 결제 요청)
- Payment -> Order: `PaymentCompletedEvent`, `PaymentFailedEvent`
- Order -> Payment: `OrderCancelledEvent` (결제 취소 요청)
- Payment -> Order: `PaymentCancelledEvent` (취소 결과)

양방향 이벤트 의존은 순환 가능성이 있다. 현재 설계에서는 각 이벤트의 역할이 다르므로 논리적 순환은 아니지만, 장애 복구 시 무한 루프 가능성에 대한 가드가 필요하다.

**수정 제안**: 이벤트 핸들러에 멱등성 보장 + 처리 상태 체크 로직 추가.

```kotlin
class OrderCancelledEventHandler {
    fun handle(event: OrderCancelledEvent) {
        val payment = paymentRepository.findByOrderId(event.orderId) ?: return
        if (payment.status == PaymentStatus.CANCELLED) return  // 이미 처리됨 (멱등성 가드)
        paymentService.cancel(payment.id, event.reason)
    }
}
```

#### [W-BC-03] Display BC가 너무 많은 Context에 Sync API 의존

Display BC가 Product, Search, Inventory, Review, Content에 동기 API 호출을 한다. 이는 Display 서비스의 가용성이 5개 서비스에 의존하게 되어 SPOF 위험이 있다.

**수정 제안**: Display는 CQRS Read Model 패턴을 적용하여, 필요한 데이터를 이벤트 기반으로 비정규화하여 자체 보관. 동기 호출을 최소화.

### 2.2 제안 (SUGGESTION)

#### [S-BC-01] ACL 정의 부족

Product(S3), Payment(Toss), Shipping(택배사), Search(ES) BC에만 ACL이 정의되어 있다. 다음 외부 시스템에 대한 ACL도 필요하다:

- **Notification**: 카카오 알림톡 API, SMS API, FCM/APNs 푸시 API
- **Member**: OAuth Provider (카카오, 네이버, 구글, 애플) API
- **Settlement**: 은행 이체 API

#### [S-BC-02] Seller와 Product 간 관계 명확화 필요

Seller가 Product를 등록하는 플로우에서, Product BC의 `CreateProductUseCase`가 Seller의 승인 상태를 어떻게 검증하는지 불명확하다. `ProductDomainService.validateProductCreation()`에서 Brand의 승인 상태만 확인하고, Seller의 ACTIVE 상태는 확인하지 않는다.

---

## 3. 상태 머신 검증

### 3.1 위반 (VIOLATION)

#### [V-SM-01] OrderStatus에서 PREPARING -> CANCEL_REQUESTED 전이 후 흐름이 불완전하다

OrderStatus 테이블:
- `CANCEL_REQUESTED -> ORDER_CANCELLED` 만 정의됨

그러나 PREPARING 단계에서 취소 요청을 셀러가 거부할 수 있다 (이미 출고 준비 완료 등). 거부 시 `CANCEL_REQUESTED -> PREPARING`으로 복원하는 전이가 없다.

또한 `cancelOrder()` 메서드의 전이 설명에 `-> ORDER_CANCELLED`로만 되어 있는데, OrderStatus 테이블에서는 `PAYMENT_COMPLETED`와 `PREPARING`에서 `CANCEL_REQUESTED`로의 전이만 있고, `ORDER_CREATED`에서 직접 `ORDER_CANCELLED`로의 전이가 있다. 일관성이 부족하다.

**수정 제안**:

```kotlin
enum class OrderStatus {
    ORDER_CREATED, PAYMENT_COMPLETED, PREPARING, SHIPPED, IN_TRANSIT,
    DELIVERED, PURCHASE_CONFIRMED, ORDER_CANCELLED, CANCEL_REQUESTED, CANCEL_REJECTED;

    fun canTransitionTo(target: OrderStatus): Boolean = when (this) {
        ORDER_CREATED -> target in listOf(PAYMENT_COMPLETED, ORDER_CANCELLED)
        PAYMENT_COMPLETED -> target in listOf(PREPARING, CANCEL_REQUESTED)
        PREPARING -> target in listOf(SHIPPED, CANCEL_REQUESTED)
        CANCEL_REQUESTED -> target in listOf(ORDER_CANCELLED, CANCEL_REJECTED)
        CANCEL_REJECTED -> target == PREPARING  // 셀러 거부 시 원래 상태로 복귀
        SHIPPED -> target == IN_TRANSIT
        IN_TRANSIT -> target == DELIVERED
        DELIVERED -> target in listOf(PURCHASE_CONFIRMED, RETURN_REQUESTED, EXCHANGE_REQUESTED)
        PURCHASE_CONFIRMED -> false
        ORDER_CANCELLED -> false
    }
}
```

### 3.2 경고 (WARNING)

#### [W-SM-01] ProductStatus에서 DISCONTINUED가 터미널 상태인데 재판매 불가

현재 `DISCONTINUED -> false` (어떤 전이도 불가)로 설계되어 있다. 그러나 실제 이커머스에서 판매 중지 상품을 다시 판매 개시하는 것은 흔한 시나리오이다 (시즌 상품의 재판매 등).

**수정 제안**: `DISCONTINUED -> ON_SALE` 전이 허용 또는 `DISCONTINUED -> APPROVED` 전이 허용하여 재심사 후 판매 가능하게 변경.

```kotlin
DISCONTINUED -> target == APPROVED  // 재판매 시 재승인 필요
```

#### [W-SM-02] SellerStatus에서 APPROVED -> ACTIVE 전이가 자동인지 수동인지 불명확

`PENDING_REVIEW -> APPROVED -> ACTIVE`에서 APPROVED와 ACTIVE가 별도 상태로 존재하는 이유가 불명확하다. 승인 즉시 ACTIVE로 전환된다면 APPROVED 상태가 불필요하고, 별도 활성화 단계가 있다면 그 트리거가 문서에 없다.

**수정 제안**: 입점 승인 후 셀러가 초기 설정(정산 계좌, 브랜드 등록 등)을 완료해야 ACTIVE로 전환되는 것이라면 명시 필요. 아니라면 APPROVED를 제거하고 심사 승인 시 바로 ACTIVE로.

### 3.3 제안 (SUGGESTION)

#### [S-SM-01] PaymentStatus에 IN_PROGRESS -> READY 복원 전이 추가 고려

결제 진행 중 사용자가 결제 화면을 닫고 재시도하는 경우, IN_PROGRESS 상태의 결제를 어떻게 처리할지 명시되지 않았다. 타임아웃 후 EXPIRED 또는 FAILED로 전이하는 규칙이 필요하다. 현재 IN_PROGRESS에서 EXPIRED로의 전이는 정의되어 있지 않다 (READY -> EXPIRED만 있음).

---

## 4. Domain Event 검증

### 4.1 위반 (VIOLATION)

#### [V-EVT-01] 재고 차감(DEDUCT) 시점의 이벤트 흐름이 불일치

`full_domain_analysis.md`에서:
> "결제 완료 시: 예약재고 차감, 실물재고 차감 (DEDUCT)"

그러나 `PaymentCompletedEvent`의 Consumer에 Inventory가 있고, Inventory에서 `StockDeductedEvent`를 발행하지만, 이 이벤트의 Consumer가 `-` (없음)으로 표기되어 있다.

문제는 `PaymentCompletedEvent`의 Consumer에 `Order`와 `Inventory`가 동시에 있는데:
- Order는 이 이벤트를 받아 `confirmPayment()` 호출 (ORDER_CREATED -> PAYMENT_COMPLETED)
- Inventory는 이 이벤트를 받아 `deduct()` 호출

그런데 `OrderCreatedEvent`에서 이미 재고 예약(reserve)을 했으므로, `PaymentCompletedEvent`에서 deduct를 하려면 orderId로 예약된 재고를 찾아 차감해야 한다. 이 매핑 정보(어떤 주문의 어떤 SKU가 예약되었는지)가 StockDeductedEvent 페이로드에 반드시 포함되어야 한다.

**수정 제안**: PaymentCompletedEvent 페이로드에 `items[{skuId, quantity}]` 필드 추가 또는 Inventory에서 orderId 기반으로 예약 내역을 조회하는 로직 명시.

```
PaymentCompletedEvent {
    paymentId, orderId, amount, method, paymentKey,
    items: [{skuId, quantity}],  // 추가 필요
    timestamp
}
```

### 4.2 경고 (WARNING)

#### [W-EVT-01] ShippedEvent와 DeliveredEvent의 페이로드 정의 누락

Shipping BC의 Domain Event 테이블에 `ShippedEvent`, `DeliveredEvent` 등이 부록 이벤트 카탈로그에만 나열되어 있고, Shipping 섹션(5.x)의 Domain Event 테이블이 아예 없다. 이벤트 페이로드와 상세 Consumer가 정의되지 않았다.

**수정 제안**: Shipping 섹션에 Domain Event 테이블 추가.

```
| 이벤트 | 페이로드 | Consumer |
|--------|---------|----------|
| ShipmentCreatedEvent | shipmentId, orderId, sellerId, timestamp | Order |
| ShippedEvent | shipmentId, orderId, courierId, trackingNumber, timestamp | Order, Notification |
| DeliveredEvent | shipmentId, orderId, deliveredAt, timestamp | Order, Notification |
| AutoConfirmedEvent | shipmentId, orderId, timestamp | Order, Settlement |
| ReturnApprovedEvent | returnRequestId, orderId, orderItemId, returnAmount, timestamp | Payment, Inventory |
| ReturnRejectedEvent | returnRequestId, orderId, reason, timestamp | Order, Notification |
```

#### [W-EVT-02] 이벤트 Consumer 매핑에서 Notification이 누락된 곳이 있다

- `OrderCancelledEvent`: Consumer에 Notification이 없다. 주문 취소 시 구매자에게 알림이 필요하다.
- `PaymentFailedEvent`: Consumer에 Notification이 없다. 결제 실패 시 구매자 알림이 필요하다.
- `ReturnApprovedEvent`: Consumer에 Notification이 없다. 반품 승인 시 구매자 알림이 필요하다.

### 4.3 제안 (SUGGESTION)

#### [S-EVT-01] 이벤트 버전 관리 전략 추가 필요

이벤트 스키마가 변경될 때의 하위 호환성 전략이 없다. Avro/JSON Schema 기반 이벤트 버전 관리 필요.

```kotlin
// 이벤트 공통 헤더 추가 제안
abstract class DomainEvent {
    abstract val eventId: String       // UUID
    abstract val eventType: String     // 이벤트 타입
    abstract val version: Int          // 스키마 버전
    abstract val aggregateId: String   // Aggregate Root ID
    abstract val occurredAt: LocalDateTime
    abstract val correlationId: String // 트레이싱용
}
```

#### [S-EVT-02] Dead Letter Queue(DLQ) 처리 전략 명시 필요

이벤트 소비 실패 시의 재시도 정책과 DLQ 처리 전략이 없다. Kafka 기반이므로:

```
재시도 정책: 3회 (1초, 5초, 30초 간격)
DLQ 토픽: {원본토픽}.DLT
DLQ 알림: 관리자 Slack 알림
수동 재처리: 관리자 도구 제공
```

#### [S-EVT-03] 순환 의존 분석 결과

이벤트 흐름에서 직접적인 순환 의존은 발견되지 않았다. 다만 다음 간접 경로에 주의:

```
Order -> Inventory (재고 예약)
Inventory -> Product (품절 알림)
Product -> Search (인덱스 업데이트)
```

이 경로에서 Product가 품절 상태로 변경되면서 다시 주문에 영향을 주는 간접 순환이 있으나, 이는 비즈니스 의도에 부합하므로 문제 없다. 다만 이벤트 폭풍(event storm)을 방지하기 위해 변경 감지 기반 이벤트 발행을 권장한다 (상태가 실제로 변경된 경우에만 이벤트 발행).

---

## 5. 비즈니스 규칙 검증

### 5.1 위반 (VIOLATION)

#### [V-BIZ-01] 결제 금액 검증 공식이 Aggregate 내에 캡슐화되지 않았다

`full_domain_analysis.md`에서 정의된 결제 금액 공식:
> `Order.totalAmount = SUM(OrderItem.subtotal) + SUM(deliveryFee) - couponDiscount - pointsUsed`

이 공식은 `OrderAmounts` VO에 `validate()`와 `recalculate()` 메서드로 존재하지만, **검증 로직의 상세 구현이 없다**. 특히:

1. 쿠폰 할인 + 적립금이 상품 총액을 초과할 수 없다는 규칙
2. 적립금은 주문 총액의 최대 30%까지만 사용 가능하다는 규칙
3. 배송비는 할인 대상이 아니라는 규칙

이 규칙들이 OrderAmounts VO 내에 캡슐화되어야 한다.

**수정 제안**:

```kotlin
data class OrderAmounts(
    val totalItemAmount: Money,       // 상품 합계
    val totalDeliveryFee: Money,      // 배송비 합계
    val couponDiscount: Money,        // 쿠폰 할인
    val pointsUsed: Money,            // 적립금 사용
    val paymentAmount: Money           // 최종 결제 금액
) {
    init {
        validate()
    }

    fun validate() {
        // 규칙 1: 결제 금액 = 상품가 + 배송비 - 쿠폰 할인 - 적립금
        val calculated = totalItemAmount.add(totalDeliveryFee)
            .subtract(couponDiscount)
            .subtract(pointsUsed)
        require(paymentAmount == calculated) {
            "결제 금액 불일치: 예상 $calculated, 실제 $paymentAmount"
        }

        // 규칙 2: 쿠폰 + 적립금은 상품 총액을 초과할 수 없다 (배송비는 할인 대상 아님)
        require(couponDiscount.add(pointsUsed).isLessThanOrEqual(totalItemAmount)) {
            "할인 금액이 상품 총액을 초과합니다"
        }

        // 규칙 3: 적립금은 주문 총액(상품+배송비)의 30%까지만 사용 가능
        val maxPointUsage = totalItemAmount.add(totalDeliveryFee).multiply(BigDecimal("0.30"))
        require(pointsUsed.isLessThanOrEqual(maxPointUsage)) {
            "적립금 사용 한도 초과 (최대: $maxPointUsage)"
        }

        // 규칙 4: 최종 결제 금액은 0 이상
        require(paymentAmount.isPositive() || paymentAmount.isZero()) {
            "결제 금액은 0 이상이어야 합니다"
        }

        // 규칙 5: 적립금 최소 사용 단위 1,000P
        if (pointsUsed.amount > BigDecimal.ZERO) {
            require(pointsUsed.amount >= BigDecimal("1000")) {
                "적립금 최소 사용 단위는 1,000P입니다"
            }
        }
    }

    companion object {
        fun calculate(
            items: List<OrderItem>,
            deliveryFees: Map<SellerId, Money>,
            couponDiscount: Money,
            pointsUsed: Money
        ): OrderAmounts {
            val totalItemAmount = items.fold(Money.ZERO) { acc, item -> acc.add(item.subtotal) }
            val totalDeliveryFee = deliveryFees.values.fold(Money.ZERO) { acc, fee -> acc.add(fee) }
            val paymentAmount = totalItemAmount.add(totalDeliveryFee)
                .subtract(couponDiscount)
                .subtract(pointsUsed)
            return OrderAmounts(totalItemAmount, totalDeliveryFee, couponDiscount, pointsUsed, paymentAmount)
        }
    }
}
```

### 5.2 경고 (WARNING)

#### [W-BIZ-01] 재고 예약 타임아웃(30분) 처리 로직 누락

`full_domain_analysis.md`에서:
> "예약 후 결제 미완료: 예약 타임아웃 30분 -> 미결제 시 자동 예약 해제"

이 로직이 어느 Bounded Context의 어느 UseCase에서 처리되는지 정의되어 있지 않다. 스케줄러 또는 TTL 기반 자동 해제 메커니즘이 필요하다.

**수정 제안**: Inventory BC에 예약 해제 스케줄러 추가.

```kotlin
class ExpireStockReservationUseCase(
    private val inventoryRepository: InventoryRepository,
    private val eventPublisher: DomainEventPublisher
) {
    /**
     * @Scheduled(fixedRate = 60000)  // 1분 간격
     * 1. 30분 이상 예약 상태인 재고 조회
     * 2. 해당 주문의 결제 상태 확인
     * 3. 미결제 시 재고 복원 (restore)
     * 4. StockReservationExpiredEvent 발행 -> Order 취소
     */
    fun execute(): Unit
}
```

#### [W-BIZ-02] 정산 금액 계산에서 쿠폰 부담분 처리가 Aggregate에 캡슐화되지 않았다

`full_domain_analysis.md`에서:
> "정산 금액 = 결제 금액 - 쿠폰 할인(플랫폼 부담분) - 수수료"
> "쿠폰 할인 중 플랫폼 부담분은 셀러 정산에서 차감하지 않음"

이 규칙이 `SettlementItem.calculate()`에 어떻게 반영되는지 불명확하다. 쿠폰 부담 비율(플랫폼 vs 셀러) 정보가 SettlementItem에 포함되어야 한다.

**수정 제안**:

```kotlin
class SettlementItem(
    // ... 기존 필드
    val platformCouponShare: Money,   // 플랫폼 부담 쿠폰 할인
    val sellerCouponShare: Money,     // 셀러 부담 쿠폰 할인
) {
    fun calculate() {
        // 셀러 정산 대상 = 판매 금액 - 셀러 부담 쿠폰 할인
        val settleableSaleAmount = saleAmount.subtract(sellerCouponShare)
        // 수수료 = 정산 대상 x 수수료율
        commissionAmount = settleableSaleAmount.multiply(commissionRate)
        // 정산 금액 = 정산 대상 - 수수료
        settlementAmount = settleableSaleAmount.subtract(commissionAmount)
    }
}
```

### 5.3 제안 (SUGGESTION)

#### [S-BIZ-01] 동시성 제어 전략 일관성 확보

| Aggregate | 동시성 제어 | 비고 |
|-----------|-----------|------|
| Product | 낙관적 락 (version) | 적절 |
| Inventory | 비관적 락 (SELECT FOR UPDATE) | 적절 |
| Order | 낙관적 락 (version) | 적절 |
| Payment | 멱등성 키 | 적절 |
| CouponPolicy | 미정의 | **추가 필요**: Redis DECR 또는 비관적 락 |
| TimeSale | 미정의 | **추가 필요**: Redis DECR (soldQuantity) |

선착순 쿠폰과 타임세일의 동시성 제어 전략이 `full_domain_analysis.md`에만 언급되고 `ddd_tactical_design.md`에는 없다. Tactical Design에 반영 필요.

#### [S-BIZ-02] 부분 취소 시 배송비 재계산 규칙 명확화

`full_domain_analysis.md`에서:
> "부분 취소 후 무료배송 기준 미달 시 배송비 추가 청구하지 않음 (셀러 부담)"

이 규칙이 `OrderDomainService.recalculateAmountsForPartialCancel()`에 반영되어야 한다. 구체적으로 부분 취소 시:
1. 배송비는 재계산하지 않는다 (셀러 부담)
2. 쿠폰 최소 사용 금액 미달 시 쿠폰 전액 반환
3. 적립금은 비례 배분하여 반환

---

## 6. 누락 사항

### 6.1 이커머스 필수 기능 누락

#### [M-01] 교환(Exchange) 도메인 모델 부재

**심각도**: 높음

반품(ReturnRequest)은 Shipping BC에 상세 모델링되어 있으나, 교환(Exchange)은 OrderItem에 `requestExchange()` 메서드만 있고 별도 Aggregate나 상태 머신이 없다. 의류 이커머스에서 사이즈 교환은 가장 빈번한 CS 케이스이다.

**필요한 설계**:

```kotlin
class ExchangeRequest(
    val id: ExchangeRequestId,
    val orderId: OrderId,
    val orderItemId: OrderItemId,
    val originalSkuId: SkuId,
    val targetSkuId: SkuId,           // 교환 대상 SKU (다른 사이즈/색상)
    val reason: ExchangeReason,
    val reasonType: ReturnReasonType,  // 배송비 부담 결정
    val status: ExchangeStatus,
    // ...
)

enum class ExchangeStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PICKUP_SCHEDULED,
    PICKUP_COMPLETED,
    INSPECTING,
    INSPECTION_PASSED,
    INSPECTION_FAILED,
    RE_SHIPPING,          // 새 상품 발송중
    EXCHANGE_COMPLETED;
}
```

#### [M-02] 주문서(Checkout) 임시 저장 모델 부재

**심각도**: 중간

주문 생성 플로우에서 장바구니 -> 주문서(Checkout) -> 주문(Order) 단계가 있어야 한다. 현재 Cart에서 바로 Order로 생성되는 구조인데, 주문서 단계에서 배송지 선택, 쿠폰 적용, 적립금 입력 등을 임시 저장하는 Checkout Aggregate가 필요하다.

#### [M-03] 상품 문의(Product Q&A) 기능 부재

**심각도**: 중간

무신사에서 구매 전 상품 문의(Q&A)는 CS 문의와 별도로 상품 상세 페이지에서 직접 질문/답변하는 기능이다. 현재 CS BC의 Inquiry로 통합되어 있으나, 상품 문의는 공개적이고 다른 구매자도 볼 수 있다는 점에서 성격이 다르다.

#### [M-04] 쿠폰 중복 사용 규칙 미정의

**심각도**: 중간

`full_domain_analysis.md`에서 "주문당 쿠폰 최대 2개 적용 가능 (상품쿠폰 1 + 장바구니쿠폰 1)"이라고 했으나, Tactical Design의 Order Aggregate에 이 제한 로직이 없다. OrderAmounts에 `appliedCoupons: List<CouponId>`와 쿠폰 유형별 개수 제한 검증이 필요하다.

#### [M-05] 셀러별 배송비 정책(DeliveryFeePolicy) Aggregate 부재

**심각도**: 중간

`OrderDomainService`에서 `DeliveryFeePolicyRepository`를 사용하지만, DeliveryFeePolicy Aggregate가 어느 BC에도 정의되어 있지 않다. Seller BC 또는 Shipping BC에 포함되어야 한다.

```kotlin
class DeliveryFeePolicy(
    val id: DeliveryFeePolicyId,
    val sellerId: SellerId,
    val baseFee: Money,                        // 기본 배송비 (예: 3,000원)
    val freeShippingThreshold: Money?,          // 무료배송 기준 (예: 50,000원)
    val jejuAdditionalFee: Money,               // 제주 추가 배송비
    val islandAdditionalFee: Money,             // 도서산간 추가 배송비
    val bundleShippingPolicy: BundlePolicy,     // 묶음배송 정책
)
```

#### [M-06] 브랜드 팔로우(Brand Follow) 기능 부재

**심각도**: 낮음

`full_domain_analysis.md`에서 "브랜드 팔로우 기능"이 언급되었으나, Member BC에도 Product BC에도 해당 모델이 없다.

#### [M-07] 비회원 주문 처리 부재

**심각도**: 낮음

현재 설계는 모두 MemberId 기반이다. 비회원 주문(Guest Checkout)을 지원하려면 Order에 `guestEmail`, `guestPhone` 등의 필드가 필요하다. 무신사도 비회원 주문을 지원한다.

#### [M-08] 상품 옵션의 재질/소재 정보 부재

**심각도**: 낮음

의류 이커머스 특화 정보인 소재(면, 폴리에스터, 나일론 등) 정보가 Product Aggregate에 없다. `CLAUDE.md`에서 "색상/소재 관리"가 주요 도메인으로 언급되어 있다.

```kotlin
// Product에 추가
class MaterialInfo(
    val compositions: List<MaterialComposition>,  // 예: [{면, 60%}, {폴리에스터, 40%}]
    val washingInstruction: WashingInstruction,    // 세탁 방법
    val madeIn: String                             // 제조국
)

data class MaterialComposition(
    val material: String,   // 소재명
    val percentage: Int      // 비율 (합계 100%)
)
```

---

## 최종 판정

### 조건부 승인 (Conditionally Approved)

**필수 수정 사항 (구현 전 반드시 해결)**:

1. **[V-AGG-01]** Member Aggregate 분리 (Point, Wishlist 별도 Aggregate)
2. **[V-SM-01]** OrderStatus 취소 요청 거부 전이 추가
3. **[V-EVT-01]** PaymentCompletedEvent 페이로드에 items 필드 추가
4. **[V-BIZ-01]** OrderAmounts.validate()에 결제 금액 검증 로직 캡슐화
5. **[M-01]** ExchangeRequest Aggregate 추가

**권장 수정 사항 (구현 중 반영)**:

1. **[W-BC-01]** 구매확정 오케스트레이션 주도권 통일
2. **[W-AGG-03]** Coupon을 별도 Aggregate로 명시적 분리
3. **[W-EVT-01]** Shipping Domain Event 테이블 보완
4. **[M-05]** DeliveryFeePolicy Aggregate 정의
5. **[M-04]** 쿠폰 중복 사용 제한 로직 추가

**향후 개선 사항**:

1. 이벤트 버전 관리 전략 수립
2. DLQ 처리 정책 정의
3. CQRS Read Model 패턴 적용 (Display BC)
4. 비회원 주문 지원
5. 소재/재질 정보 모델 추가

---

> 본 검증은 설계 문서 기준으로 수행되었으며, 실제 구현 코드 리뷰는 포함하지 않는다.
> 위반 5건, 경고 12건, 제안 18건 중 필수 수정 5건을 해결하면 구현 착수가 가능하다.
