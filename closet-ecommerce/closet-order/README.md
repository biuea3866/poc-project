# closet-order

> 장바구니, 주문 생성, 주문 상태 관리, 주문 취소/반품 서비스

## 역할

closet-order는 주문 도메인을 담당하는 서비스이다.
장바구니 관리(추가/수량변경/삭제), 주문 생성, 주문 상태 관리(PENDING -> PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED), 주문 취소/반품 요청을 처리한다.
Kafka를 통해 OrderCreatedEvent, OrderPaidEvent, OrderCancelledEvent 등 도메인 이벤트를 발행한다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Data JPA | 엔티티 매핑, Repository |
| MySQL 8.0 (Flyway) | 데이터 저장 |
| Spring Data Redis | 캐싱 |
| Spring Kafka | 주문 이벤트 발행 |
| Virtual Threads | 가상 스레드 활성화 |

## 도메인 모델

### Order (Aggregate Root)
주문 엔티티. `orderNumber`(unique, 날짜+랜덤6자리), `memberId`, `sellerId`, `totalAmount`/`discountAmount`/`shippingFee`/`paymentAmount`(Money VO), `status`, 배송지 정보(`receiverName`, `receiverPhone`, `zipCode`, `address`, `detailAddress`), `reservationExpiresAt`, `orderedAt` 필드를 가진다.
`place()`, `pay()`, `prepare()`, `ship()`, `deliver()`, `confirm()`, `cancel()`, `requestReturn()` 등 상태 전이 메서드와 `calculatePaymentAmount()` 비즈니스 로직을 캡슐화한다.

### OrderItem
주문 항목 엔티티. `orderId`, `productId`, `productOptionId`, `productName`, `optionName`, `categoryId`, `quantity`, `unitPrice`/`totalPrice`(Money VO), `status` 필드.

### OrderStatus
주문 상태 enum: `PENDING -> STOCK_RESERVED -> PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED`. 터미널 상태: `CONFIRMED`, `CANCELLED`, `FAILED`. `canTransitionTo()` / `validateTransitionTo()`로 전이 규칙을 관리한다.

### OrderItemStatus
주문 항목 상태 enum: `ORDERED`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`.

### OrderStatusHistory
주문 상태 변경 이력 엔티티. `orderId`, `fromStatus`, `toStatus`, `reason`, `changedBy` 필드.

### Cart
장바구니 엔티티. `memberId` 필드. 회원 1인당 1개의 장바구니를 가진다.

### CartItem
장바구니 항목 엔티티. `cartId`, `productId`, `productOptionId`, `quantity`, `unitPrice`(Money VO) 필드.

### 도메인 이벤트
- `OrderCreatedEvent` -- 주문 생성 시 발행 (재고 차감 요청)
- `OrderPaidEvent` -- 결제 완료 시 발행
- `OrderCancelledEvent` -- 주문 취소 시 발행 (재고 복원 요청)

## API

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/orders | 주문 생성 |
| GET | /api/v1/orders/{id} | 주문 상세 조회 |
| GET | /api/v1/orders?memberId={memberId} | 회원 주문 목록 조회 (페이징) |
| POST | /api/v1/orders/{id}/cancel | 주문 취소 |
| POST | /api/v1/carts/items | 장바구니 항목 추가 |
| GET | /api/v1/carts | 장바구니 조회 (X-Member-Id 헤더 또는 memberId 파라미터) |
| PUT | /api/v1/carts/items/{itemId} | 장바구니 항목 수량 변경 |
| DELETE | /api/v1/carts/items/{itemId} | 장바구니 항목 삭제 |

## 패키지 구조

```
src/main/kotlin/com/closet/order/
├── application/           # OrderService, CartService
├── domain/
│   ├── cart/              # Cart, CartItem
│   ├── event/             # OrderCreatedEvent, OrderPaidEvent, OrderCancelledEvent
│   └── order/             # Order, OrderItem, OrderStatus, OrderItemStatus, OrderStatusHistory
├── presentation/
│   ├── cart/              # CartController
│   ├── order/             # OrderController
│   └── dto/               # CartDto, OrderDto
└── repository/            # CartRepository, CartItemRepository, OrderRepository, OrderItemRepository, OrderStatusHistoryRepository
```

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| cart | 장바구니 (member_id) |
| cart_item | 장바구니 항목 (cart_id, product_id, product_option_id, quantity, unit_price) |
| orders | 주문 (order_number, member_id, seller_id, total_amount, payment_amount, status, 배송지 정보) |
| order_item | 주문 항목 (order_id, product_id, product_option_id, product_name, quantity, unit_price, status) |
| order_status_history | 주문 상태 변경 이력 (order_id, from_status, to_status, reason, changed_by) |

## 포트

- 서버 포트: 8083

## 의존 서비스

- closet-common (공통 라이브러리)
- Redis (캐싱)
- Kafka (주문 이벤트 발행)
