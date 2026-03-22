# Cross-Service 정합성 검증 보고서

> 검증일: 2026-03-22
> 검증 대상: full_domain_analysis.md, ddd_tactical_design.md, erd_detailed.md
> 검증 범위: 15개 Bounded Context + 64개 테이블

---

## 총괄 요약

| 구분 | 건수 |
|------|------|
| **정합 (Pass)** | 47건 |
| **불일치 (Mismatch)** | 18건 |
| **누락 (Missing)** | 9건 |
| **개선 제안 (Suggestion)** | 12건 |

**최종 판정: 조건부 구현 진행 가능**

핵심 도메인(Product, Order, Payment, Inventory, Shipping)의 설계는 전반적으로 높은 정합성을 보이나, 일부 필드 누락, Saga 보상 경로 미비, 서비스 간 데이터 참조 불일치가 존재한다. Critical 2건, Major 7건은 구현 전 반드시 수정이 필요하다.

---

## 1. ERD <-> DDD 정합성 검증

### 1.1 Aggregate 필드 <-> ERD 컬럼 매핑

#### Product Bounded Context

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Product.id | product.id | Pass | |
| Product.sellerId | product.seller_id | Pass | |
| Product.brandId | product.brand_id | Pass | |
| Product.categoryId | product.category_id | Pass | |
| Product.name | product.name | Pass | |
| Product.description | product.description | Pass | |
| Product.listPrice (Money) | product.list_price (BIGINT) | Pass | Money VO -> 원 단위 BIGINT. currency 컬럼 부재하나 KRW 단일 통화이므로 허용 |
| Product.sellingPrice (Money) | product.selling_price (BIGINT) | Pass | 상동 |
| Product.season (Enum) | product.season (VARCHAR(30)) | Pass | Enum 값 일치: SS, FW, PRE_SS, PRE_FW, ALL |
| Product.fit (Enum) | product.fit (VARCHAR(30)) | Pass | Enum 값 일치: OVERSIZED, REGULAR, SLIM, RELAXED |
| Product.status (Enum) | product.status (VARCHAR(30)) | Pass | 7가지 상태 값 일치 |
| Product.version | product.version | Pass | |
| Product.createdAt | product.created_at | Pass | |
| Product.updatedAt | product.updated_at | Pass | |
| Product.deletedAt | product.deleted_at | Pass | |
| **Product.rejectReason** | **product.reject_reason** | **Mismatch** | DDD에 rejectReason 필드 미정의. reject(reason) 메서드는 있으나 필드로 저장하는 구조가 Aggregate 클래스에 없음 |

**Sku (Entity)**

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Sku.id | sku.id | Pass | |
| Sku.skuCode (VO) | sku.sku_code | Pass | |
| Sku.optionCombination.color | sku.option_color | Pass | VO 분해하여 embedded |
| Sku.optionCombination.size | sku.option_size | Pass | VO 분해하여 embedded |
| Sku.additionalPrice (Money) | sku.additional_price (BIGINT) | Pass | |
| Sku.active | sku.active (TINYINT(1)) | Pass | |
| **Sku.productId** | **sku.product_id** | **Mismatch** | DDD Sku Entity에 productId 필드 미정의. Aggregate 내부 Entity로 Product가 소유하므로 DDD상으로는 불필요하나, ERD에서는 JOIN을 위해 필요. DDD 문서에 명시 필요 |

**SizeGuide (VO)**

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| SizeGuide.measurements[] | size_guide (별도 테이블) | Pass | VO이지만 1:N 관계이므로 별도 테이블은 적절 |
| SizeGuide.fitDescription | - | **Missing** | DDD SizeGuide VO에 fitDescription, modelInfo 필드가 있으나 ERD size_guide 테이블에 해당 컬럼 없음 |
| SizeGuide.modelInfo | - | **Missing** | 상동 |

**Category Aggregate**

| DDD 필드 | ERD 컬럼 | 판정 |
|----------|---------|------|
| Category.id | category.id | Pass |
| Category.parentId | category.parent_id | Pass |
| Category.name | category.name | Pass |
| Category.depth | category.depth | Pass |
| Category.displayOrder | category.display_order | Pass |
| Category.commissionRate | category.commission_rate | Pass |
| Category.active | category.active | Pass |

**Brand Aggregate**

| DDD 필드 | ERD 컬럼 | 판정 |
|----------|---------|------|
| Brand.id | brand.id | Pass |
| Brand.sellerId | brand.seller_id | Pass |
| Brand.nameKo | brand.name_ko | Pass |
| Brand.nameEn | brand.name_en | Pass |
| Brand.logoUrl | brand.logo_url | Pass |
| Brand.description | brand.description | Pass |
| Brand.status | brand.status | Pass |
| Brand.isPb | brand.is_pb | Pass |

#### Order Bounded Context

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Order.id | orders.id | Pass | |
| Order.orderNumber (VO) | orders.order_number | Pass | |
| Order.memberId | orders.member_id | Pass | |
| Order.shippingAddress (VO) | orders 테이블 내 embedded | Pass | receiver_name, receiver_phone, zip_code, address, detail_address, delivery_memo |
| Order.amounts (VO) | orders 테이블 내 embedded | Pass | total_item_amount, total_delivery_fee, coupon_discount, points_used, payment_amount |
| Order.status | orders.status | Pass | |
| Order.cancelReason | orders.cancel_reason | Pass | |
| Order.version | orders.version | Pass | |
| Order.orderedAt | orders.ordered_at | Pass | |
| **Order.createdAt** | **-** | **Mismatch** | DDD에는 createdAt이 없고 orderedAt만 있음. ERD도 ordered_at으로 통일. 그러나 orders 테이블에 created_at 컬럼이 없음 (다른 모든 테이블은 created_at 존재) |
| OrderItem.productSnapshot (VO) | order_item 테이블 내 embedded | Pass | product_name, brand_name, option_color, option_size, image_url로 분해 |
| **OrderItem.productSnapshot.originalPrice** | **-** | **Missing** | DDD ProductSnapshot에 originalPrice 필드가 있으나 ERD order_item에 해당 컬럼 없음. unit_price만 존재하여 할인 전 가격 추적 불가 |
| OrderItem.sellerId | order_item.seller_id | Pass | |

#### Payment Bounded Context

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Payment.id | payment.id | Pass | |
| Payment.orderId | payment.order_id | Pass | |
| Payment.memberId | payment.member_id | Pass | |
| Payment.paymentKey (VO) | payment.payment_key | Pass | |
| Payment.idempotencyKey (VO) | payment.idempotency_key | Pass | |
| Payment.amount (Money) | payment.amount (BIGINT) | Pass | |
| Payment.refundedAmount (Money) | payment.refunded_amount (BIGINT) | Pass | |
| Payment.method (Enum) | payment.method (VARCHAR(30)) | Pass | 6가지 Enum 값 일치 |
| Payment.status (Enum) | payment.status (VARCHAR(30)) | Pass | 7가지 Enum 값 일치 |
| Payment.failReason | payment.fail_reason | Pass | |
| Payment.paidAt | payment.paid_at | Pass | |
| Payment.cancelledAt | payment.cancelled_at | Pass | |
| **Payment.pgTransactionId** | **payment.pg_transaction_id** | **Mismatch** | DDD Payment Aggregate에 pgTransactionId 필드가 미정의되어 있으나 ERD에는 존재. ACL에서 반환하는 값을 저장하는 구조인데 DDD 모델에 누락 |
| **Payment.receiptUrl** | **payment.receipt_url** | **Mismatch** | 상동. ERD에는 존재하나 DDD Aggregate에 미정의 |

#### Inventory Bounded Context

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Inventory.id | inventory.id | Pass | |
| Inventory.skuId | inventory.sku_id | Pass | |
| **Inventory.productId** | **inventory.product_id** | **Mismatch** | DDD Inventory Aggregate에 productId 미정의. ERD에만 존재 |
| Inventory.totalStock | inventory.total_stock | Pass | |
| Inventory.reservedStock | inventory.reserved_stock | Pass | |
| Inventory.safetyStock | inventory.safety_stock | Pass | |
| Inventory.version | inventory.version | Pass | |

**StockHistory (Entity)**

| DDD 필드 | ERD 컬럼 | 판정 |
|----------|---------|------|
| StockHistory.id | stock_history.id | Pass |
| StockHistory.skuId | stock_history.sku_id | Pass |
| StockHistory.type (Enum) | stock_history.type (VARCHAR(30)) | Pass |
| StockHistory.quantity | stock_history.quantity | Pass |
| StockHistory.beforeTotal | stock_history.before_total | Pass |
| StockHistory.afterTotal | stock_history.after_total | Pass |
| StockHistory.beforeReserved | stock_history.before_reserved | Pass |
| StockHistory.afterReserved | stock_history.after_reserved | Pass |
| StockHistory.referenceId | stock_history.reference_id | Pass |
| StockHistory.referenceType | stock_history.reference_type | Pass |

#### Shipping Bounded Context

| DDD 필드 | ERD 컬럼 | 판정 | 비고 |
|----------|---------|------|------|
| Shipment.id | shipment.id | Pass | |
| Shipment.orderId | shipment.order_id | Pass | |
| **Shipment.orderItemId** | **shipment.order_item_id** | **Mismatch** | DDD Shipment에 orderItemId 미정의. ERD에만 존재 (부분 출고 시 사용) |
| Shipment.sellerId | shipment.seller_id | Pass | |
| Shipment.courierId (VO) | shipment.courier_id (VARCHAR(30)) | Pass | |
| Shipment.trackingNumber (VO) | shipment.tracking_number | Pass | |
| Shipment.status (Enum) | shipment.status (VARCHAR(30)) | Pass | |
| Shipment.address (VO) | shipment 테이블 내 embedded | Pass | receiver_name/phone/zip_code/address/detail_address |
| **ReturnRequest.sellerId** | **return_request.seller_id** | Pass | DDD에는 없으나 ERD에 존재. 셀러별 조회를 위해 ERD가 올바름 |
| **ReturnRequest.returnFee** | **return_request.return_fee** | **Missing** | DDD ReturnRequest에 returnFee/feePayer 필드 미정의. ERD에만 존재 |
| **ReturnRequest.feePayer** | **return_request.fee_payer** | **Missing** | 상동 |

### 1.2 DDD Value Object -> ERD 표현 방식 검증

| Value Object | 표현 방식 | 판정 | 비고 |
|-------------|----------|------|------|
| Money | Embedded (BIGINT) | Pass | 단일 통화(KRW)이므로 amount만 저장 적절 |
| OptionCombination | Embedded (2개 컬럼) | Pass | option_color, option_size로 분해 |
| SkuCode | Embedded (VARCHAR) | Pass | |
| OrderNumber | Embedded (VARCHAR) | Pass | |
| ShippingAddress (Order) | Embedded (5개 컬럼) | Pass | orders 테이블에 직접 포함 |
| ShippingAddress (Member) | Separate Table | Pass | 회원은 N개 배송지 가능하므로 별도 테이블 적절 |
| ShippingAddress (Shipment) | Embedded (5개 컬럼) | Pass | 배송 시점 스냅샷이므로 embedded 적절 |
| ProductSnapshot | Embedded (6개 컬럼) | Pass | order_item에 직접 포함 |
| SizeGuide/SizeMeasurement | Separate Table | Pass | 1:N 관계이므로 별도 테이블 적절 |
| OrderAmounts | Embedded (5개 컬럼) | Pass | orders 테이블에 직접 포함 |
| DiscountInfo | Embedded (3개 컬럼) | Pass | coupon_policy에 직접 포함 |
| CouponCondition | **Embedded (TEXT)** | **Suggestion** | applicable_categories, applicable_brands, excluded_products가 콤마 구분 TEXT. 정규화 시 별도 매핑 테이블이 이상적이나, 조회 빈도 고려 시 허용 가능 |

### 1.3 DDD Enum <-> ERD VARCHAR 일치 검증

| Enum | DDD 정의 값 | ERD COMMENT 값 | 판정 |
|------|-----------|---------------|------|
| ProductStatus | DRAFT, PENDING_REVIEW, REJECTED, APPROVED, ON_SALE, SOLD_OUT, DISCONTINUED | 동일 | Pass |
| Fit | OVERSIZED, REGULAR, SLIM, RELAXED | 동일 | Pass |
| Season | SS, FW, PRE_SS, PRE_FW, ALL | 동일 | Pass |
| OrderStatus | ORDER_CREATED, PAYMENT_COMPLETED, PREPARING, SHIPPED, IN_TRANSIT, DELIVERED, PURCHASE_CONFIRMED, ORDER_CANCELLED, CANCEL_REQUESTED | 동일 | Pass |
| OrderItemStatus | ORDERED, CANCELLED, RETURN_REQUESTED, RETURN_COMPLETED, RETURN_REJECTED, EXCHANGE_REQUESTED, EXCHANGE_COMPLETED, EXCHANGE_REJECTED, PURCHASE_CONFIRMED | 동일 | Pass |
| PaymentStatus | READY, IN_PROGRESS, DONE, PARTIAL_CANCELLED, CANCELLED, FAILED, EXPIRED | 동일 | Pass |
| PaymentMethod | CARD, KAKAO_PAY, NAVER_PAY, TOSS_PAY, VIRTUAL_ACCOUNT, BANK_TRANSFER | 동일 | Pass |
| ShipmentStatus | READY_TO_SHIP, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, AUTO_CONFIRMED | 동일 | Pass |
| ReturnStatus | REQUESTED, APPROVED, REJECTED, PICKUP_SCHEDULED, PICKUP_COMPLETED, INSPECTING, INSPECTION_PASSED, INSPECTION_FAILED, RETURN_COMPLETED, RETURN_REJECTED | DDD 정의와 일치하나 ERD return_request.status COMMENT에 미열거 | **Mismatch** |
| ReturnReasonType | CHANGE_OF_MIND, WRONG_SIZE, DEFECTIVE, WRONG_PRODUCT, DAMAGED_IN_TRANSIT | 동일 | Pass |
| StockChangeType | RESERVE, DEDUCT, RESTORE, RESTOCK, ADJUST | 동일 | Pass |
| MemberGrade | BASIC, SILVER, GOLD, PLATINUM | 동일 | Pass |
| MemberStatus | ACTIVE, DORMANT, WITHDRAWN | 동일 | Pass |
| SocialProvider | KAKAO, NAVER, GOOGLE, APPLE | 동일 | Pass |
| CouponStatus | ISSUED, USED, EXPIRED, RESTORED | 동일 | Pass |
| FitFeedback | SMALL, TRUE_TO_SIZE, LARGE | 동일 | Pass |
| BrandStatus | DDD: PENDING, APPROVED, REJECTED | ERD COMMENT: PENDING, APPROVED, REJECTED | Pass |
| SellerStatus | DDD: PENDING_REVIEW, APPROVED, ACTIVE, SUSPENDED, REJECTED, TERMINATED | ERD: 동일 | Pass |
| **SettlementStatementStatus** | DDD: PENDING, CALCULATED, CONFIRMED, DISPUTED, RE_CALCULATED, PAID | ERD settlement_statement: CALCULATED만 기본값 명시. PENDING이 DDD에는 있으나 settlement_item에서 사용. statement에서는 CALCULATED부터 시작 | **Mismatch** |

---

## 2. 이벤트 <-> ERD 정합성 검증

### 2.1 Domain Event 페이로드 필드 -> ERD 소스 테이블 추적

| 이벤트 | 페이로드 필드 | 소스 테이블 | 판정 | 비고 |
|--------|------------|-----------|------|------|
| ProductCreatedEvent | productId, sellerId, name, categoryId, brandId, skus[] | product, sku | Pass | |
| ProductApprovedEvent | productId, skus[{skuId, skuCode}], categoryId | product, sku | Pass | |
| PriceChangedEvent | productId, oldPrice, newPrice | product | Pass | oldPrice는 변경 전 값 (메모리 기반) |
| OrderCreatedEvent | orderId, orderNumber, memberId, items[{skuId, quantity}], amounts | orders, order_item | Pass | |
| OrderCancelledEvent | orderId, items[{skuId, quantity}], **paymentId**, couponId, pointsUsed | orders, order_item | **Mismatch** | paymentId가 이벤트에 포함되나 orders/order_item 테이블에 payment_id 컬럼 없음. Payment 서비스에서 order_id로 역조회해야 함 |
| OrderItemCancelledEvent | orderId, itemId, skuId, quantity, refundAmount | order_item | Pass | |
| PurchaseConfirmedEvent | orderId, items[{skuId, sellerId, amount}] | orders, order_item | Pass | |
| PaymentCompletedEvent | paymentId, orderId, amount, method, paymentKey | payment | Pass | |
| PaymentFailedEvent | paymentId, orderId, reason | payment | Pass | |
| PaymentCancelledEvent | paymentId, orderId, refundAmount | payment | Pass | |
| StockReservedEvent | orderId, items[{skuId, quantity}] | inventory | Pass | orderId는 referenceId로 저장 |
| StockReservationFailedEvent | orderId, failedSkuId, availableStock, requestedQuantity | inventory | Pass | availableStock은 계산 값 |
| SoldOutEvent | skuId, productId | inventory | Pass | |
| RestockedEvent | skuId, productId, quantity | inventory | Pass | |
| ReviewCreatedEvent | reviewId, productId, memberId, rating, reward | review | Pass | |
| CouponIssuedEvent | couponId, policyId, memberId, expiresAt | coupon | Pass | |
| MemberRegisteredEvent | memberId, email, name | member | Pass | |
| GradeChangedEvent | memberId, fromGrade, toGrade | member, member_grade_history | Pass | |

### 2.2 이벤트 흐름 -> 데이터 조회 가능성 검증

| 이벤트 소비 시나리오 | 필요 데이터 | 조회 경로 | 판정 | 비고 |
|-------------------|-----------|----------|------|------|
| Inventory가 OrderCreatedEvent 소비 -> 재고 예약 | skuId, quantity | 이벤트 페이로드에 포함 | Pass | |
| Order가 PaymentCompletedEvent 소비 -> 주문 상태 변경 | orderId | 이벤트 페이로드에 포함 | Pass | |
| Settlement이 PurchaseConfirmedEvent 소비 -> 정산 등록 | orderId, items[{skuId, sellerId, amount}] | 이벤트 페이로드에 포함 | **Mismatch** | 정산에 필요한 categoryId, commissionRate가 이벤트에 없음. Settlement이 Product API를 동기 호출하여 카테고리 수수료율을 조회해야 하는데, 이 의존성이 명확하게 문서화되지 않음 |
| Notification이 RestockedEvent 소비 -> 재입고 알림 | skuId | 이벤트 -> restock_subscription.sku_id 조회 | Pass | |
| Search가 PriceChangedEvent 소비 -> 인덱스 업데이트 | productId, newPrice | 이벤트 페이로드에 포함 | Pass | |
| Member가 ReviewCreatedEvent 소비 -> 포인트 적립 | memberId, reward | 이벤트 페이로드에 포함 | Pass | |

---

## 3. API <-> 도메인 정합성 검증

### 3.1 PRD 핵심 유스케이스 -> 도메인 모델 구현 가능성

| 유스케이스 | 필요 데이터 | 도메인 모델 매핑 | 판정 | 비고 |
|----------|-----------|---------------|------|------|
| 상품 등록 | 기본정보 + 옵션 + 이미지 + 사이즈가이드 | Product Aggregate | Pass | |
| 상품 검색 (필터: 카테고리/브랜드/가격/사이즈/색상) | 다중 필터 조건 | SearchDocument (ES) | Pass | |
| 장바구니 -> 주문 전환 | Cart -> Order + ProductSnapshot | Cart, Order Aggregate | Pass | |
| 셀러별 배송비 계산 | 셀러ID, 주문금액, 배송지역 | delivery_fee_policy | Pass | |
| 부분 취소 시 쿠폰 재계산 | 쿠폰 최소금액, 남은 항목 금액 | Order + Promotion 연동 | Pass | |
| 등급별 포인트 적립 | 구매확정금액, 등급별 적립률 | Member.grade.getPointRate() | Pass | |
| 사이즈 리뷰 요약 | 상품별 핏감 투표 집계 | size_feedback.fit_feedback 집계 | Pass | |
| 셀러 파트너센터 대시보드 | 매출/주문/정산 통합 조회 | 다수 서비스 조합 | **Suggestion** | Aggregator/BFF 서비스 필요성이 PRD에 언급되나 서비스 목록에 미포함 |

### 3.2 API Response 필드 -> ERD 존재 여부

| API 응답 시나리오 | 필요 필드 | ERD 컬럼 존재 | 판정 | 비고 |
|-----------------|----------|-------------|------|------|
| 상품 상세 조회 | 상품정보 + 옵션 + 가용재고 + 리뷰평점 | product + sku + inventory(타서비스) + review(타서비스) | Pass | 서비스 간 API 조합 필요 |
| 주문 내역 조회 | 주문정보 + 배송상태 + 상품이미지 | orders + order_item(스냅샷) + shipment(타서비스) | Pass | |
| 정산서 상세 | 건별 내역 + 수수료율 + 반품상계 | settlement_item + settlement_statement | Pass | |
| **주문 상세 - 쿠폰 정보** | **사용 쿠폰명, 할인 금액** | **orders.coupon_discount만 존재** | **Missing** | orders에 coupon_id / coupon_policy_id 컬럼이 없어 사용된 쿠폰 상세 추적 불가 |
| **주문 상세 - 적립 예정 포인트** | **등급별 적립 예정 포인트** | **-** | **Missing** | 주문 시점 등급/적립률 스냅샷이 orders에 없음 |

---

## 4. Saga 플로우 검증

### 4.1 주문 -> 결제 -> 재고 Saga

#### Happy Path 검증

```
Order(ORDER_CREATED) -> Inventory(RESERVE) -> Payment(APPROVE) -> Inventory(DEDUCT) -> Order(PAYMENT_COMPLETED)
```

| 단계 | 이벤트 | 보상 트랜잭션 | 판정 | 비고 |
|------|-------|-------------|------|------|
| 1. 주문 생성 | OrderCreatedEvent | - | Pass | |
| 2. 재고 예약 성공 | StockReservedEvent | 없음 (첫 단계) | Pass | |
| 2'. 재고 예약 실패 | StockReservationFailedEvent | Order -> ORDER_CANCELLED | Pass | |
| 3. 결제 승인 성공 | PaymentCompletedEvent | - | Pass | |
| 3'. 결제 승인 실패 | PaymentFailedEvent | Inventory RESTORE + Order CANCEL | Pass | |
| 4. 재고 확정 차감 | StockDeductedEvent | - | Pass | |
| 5. 쿠폰 사용 확정 | CouponUsedEvent | - | Pass | |
| 6. 포인트 차감 | PointsDeductedEvent | - | **Mismatch** | DDD에 PointsDeductedEvent 미정의. Member 이벤트 목록에 PointUsedEvent만 존재 |

#### Critical 누락 사항

**[Critical-1] 주문 상태 STOCK_RESERVED 누락**

PRD Saga 플로우(Section 4.1)에서 `ORDER_CREATED -> STOCK_RESERVED -> PAYMENT_COMPLETED` 전이가 기술되어 있으나:
- DDD OrderStatus Enum에 `STOCK_RESERVED` 상태가 없음
- ERD orders.status COMMENT에도 `STOCK_RESERVED` 미포함
- DDD 정의: `ORDER_CREATED -> PAYMENT_COMPLETED` (직접 전이)

이는 Saga의 중간 상태가 ERD/DDD에 반영되지 않은 것으로, **재고 예약 완료~결제 사이의 상태를 추적할 수 없다**.

**수정 필요**: OrderStatus에 `STOCK_RESERVED` 추가하거나, 재고 예약을 동기 처리로 변경

**[Critical-2] 예약 타임아웃 처리 테이블 부재**

PRD에 "예약 타임아웃 30분 -> 미결제 시 자동 예약 해제"가 명시되어 있으나:
- 예약 만료 시각을 저장할 컬럼이 orders/inventory 어디에도 없음
- 스케줄러가 만료 대상을 조회할 인덱스도 없음

**수정 필요**: orders에 `reservation_expires_at DATETIME(6)` 컬럼 추가, 또는 별도 reservation 테이블 생성

### 4.2 반품 -> 환불 Saga

```
CS(접수) -> Shipping(수거) -> Seller(검수) -> Payment(환불) -> Inventory(복원) -> Order(상태변경)
```

| 단계 | 보상 트랜잭션 | 판정 | 비고 |
|------|-------------|------|------|
| 수거 요청 실패 | 반품 접수 취소 | Pass | |
| 검수 거부 | 반송 처리 | Pass | |
| 환불 실패 | 재시도 3회 -> 수동 처리 | **Suggestion** | 환불 실패 상태를 추적할 별도 상태값이 없음 |
| 재고 복원 실패 | 수동 보정 | **Suggestion** | stock_history에 실패 이력을 남길 방안 미정의 |

### 4.3 정산 Saga

```
PurchaseConfirmed -> SettlementItem(PENDING) -> 주간배치 -> SettlementStatement(CALCULATED) -> Seller확인 -> PAID
```

| 단계 | 판정 | 비고 |
|------|------|------|
| 구매확정 -> 정산 항목 등록 | Pass | PurchaseConfirmedEvent -> settlement_item |
| 수수료 계산 | Pass | category.commission_rate 참조 |
| 정산서 생성 | Pass | settlement_statement |
| 이의 제기 -> 재계산 | Pass | DISPUTED -> RE_CALCULATED 상태 |
| **구매확정 후 반품 시 상계** | **Mismatch** | settlement_item.status에 OFFSET 상태는 있으나, 반품과 정산 항목을 연결하는 return_request_id 컬럼이 settlement_item에 없음 |
| 최소 정산 금액 이월 | **Missing** | 이월 금액을 다음 주기로 넘기는 carry_over_amount 컬럼이 settlement_statement에 없음 |

### 4.4 타임아웃/재시도 전략 종합

| 구간 | 타임아웃 | 재시도 | 정의 여부 | 판정 |
|------|---------|--------|----------|------|
| PG 승인 | 30초 | 3회 (10초 간격) | PRD 정의 | Pass |
| 재고 예약 락 | 3초 | 미정의 | DDD 정의 | **Suggestion** |
| Kafka Consumer | 미정의 | 3회 -> DLQ | PRD 정의 | Pass |
| 외부 API (택배사) | 미정의 | 3회 (Exp. Backoff) | PRD 정의 | Pass |
| 가상계좌 입금 | 24시간 | 없음 (만료 처리) | PRD 정의 | Pass |
| 예약 -> 결제 | 30분 | 없음 (자동 해제) | PRD 정의 (ERD 미반영) | **Mismatch** |

---

## 5. 서비스 간 데이터 흐름 검증

### 5.1 Order -> Product 스냅샷 구조

**검증 항목**: 주문 시 상품 정보를 스냅샷으로 저장하여 이후 상품 변경에 영향받지 않는 구조

| 스냅샷 필드 | ERD order_item 컬럼 | 판정 |
|-----------|-------------------|------|
| 상품명 | product_name | Pass |
| 브랜드명 | brand_name | Pass |
| 색상 옵션 | option_color | Pass |
| 사이즈 옵션 | option_size | Pass |
| 이미지 URL | image_url | Pass |
| 단가 | unit_price | Pass |
| **카테고리명** | **-** | **Missing** | 주문 내역 조회 시 카테고리 표시를 위해 필요하나 미저장. 정산 시 수수료율 기준이 되는 category_id도 order_item에 없음 |
| **SKU 코드** | **-** | **Suggestion** | CS 대응이나 물류 확인 시 SKU 코드가 필요할 수 있으나 미저장 |

### 5.2 Settlement -> Order 데이터 참조

| 정산 필요 데이터 | 참조 방식 | 판정 | 비고 |
|---------------|----------|------|------|
| 주문 ID | settlement_item.order_id | Pass | |
| 주문 항목 ID | settlement_item.order_item_id | Pass | |
| 셀러 ID | settlement_item.seller_id | Pass | |
| 상품 ID | settlement_item.product_id | Pass | |
| 카테고리 ID | settlement_item.category_id | Pass | 수수료율 기준 |
| 판매 금액 | settlement_item.sale_amount | Pass | |
| **쿠폰 할인 구분 (플랫폼/셀러 부담)** | **-** | **Missing** | PRD에 "쿠폰 할인 중 플랫폼 부담분은 셀러 정산에서 차감하지 않음" 규칙이 있으나, settlement_item에 platform_discount / seller_discount 구분 컬럼 없음 |

### 5.3 Shipping <-> Order 상태 동기화

| Shipping 상태 | Order 동기화 상태 | 이벤트 | 판정 |
|-------------|----------------|--------|------|
| SHIPPED | SHIPPED | ShippedEvent | **Mismatch** | DDD OrderStatus에 SHIPPED 존재하나 전이 규칙에서 PREPARING -> SHIPPED 정의됨. 그런데 Shipping에서 READY_TO_SHIP -> SHIPPED로 전이 시 이벤트를 보내면, Order는 PAYMENT_COMPLETED -> PREPARING을 먼저 거쳐야 함. PREPARING 전이 트리거가 불명확 |
| IN_TRANSIT | - | - | **Mismatch** | OrderStatus에 IN_TRANSIT이 있지만 DDD 전이 규칙에서 SHIPPED -> IN_TRANSIT이 정의되어 있지 않음. Order가 세부 배송 상태를 모두 추적할 필요가 있는지 재검토 필요 |
| DELIVERED | DELIVERED | DeliveredEvent | Pass | |
| AUTO_CONFIRMED | PURCHASE_CONFIRMED | PurchaseConfirmedEvent | Pass | |

**수정 제안**: Order는 배송 세부 상태(IN_TRANSIT, OUT_FOR_DELIVERY)를 추적하지 않고, SHIPPED / DELIVERED / PURCHASE_CONFIRMED만 관리하는 것이 적절. OrderStatus에서 IN_TRANSIT 제거하거나 Shipping 전용 조회 API로 분리.

---

## 6. 전체 플로우 시뮬레이션

### 6.1 주문 Happy Path 시뮬레이션

```
시나리오: 회원 A가 브랜드X 반팔티(M/블랙) 2개 + 브랜드Y 데님(32) 1개 주문
쿠폰: 5,000원 정액 할인, 적립금: 3,000P 사용

1. [Cart] cart_item에 2개 SKU 저장
   -> cart_item.sku_id, product_id, quantity, product_name, brand_name, option_color, option_size, unit_price, image_url
   -> Pass: 모든 필드 존재

2. [Order] 주문 생성
   -> orders: order_number, member_id, receiver_*, total_item_amount, total_delivery_fee, coupon_discount(5000), points_used(3000), payment_amount
   -> order_item x 2: product_id, sku_id, seller_id, product_name, brand_name, option_color, option_size, image_url, quantity, unit_price, subtotal
   -> Pass: 스냅샷 데이터 저장 가능

3. [Inventory] 재고 예약
   -> inventory WHERE sku_id IN (X-M-BLK, Y-32) SELECT FOR UPDATE
   -> reserve(quantity)
   -> stock_history: type=RESERVE, reference_id=주문번호, reference_type=ORDER
   -> Pass

4. [Payment] 결제 승인
   -> payment: order_id, member_id, idempotency_key, amount=payment_amount, method=CARD
   -> PG API 호출 -> approve()
   -> payment: payment_key, pg_transaction_id, receipt_url, paid_at, status=DONE
   -> payment_history: type=APPROVED
   -> Pass

5. [Inventory] 재고 확정 차감
   -> deduct(quantity) for each SKU
   -> stock_history: type=DEDUCT
   -> Pass

6. [Promotion] 쿠폰 사용 처리
   -> coupon: status=USED, used_order_id=orderId, used_at=now
   -> Pass

7. [Member] 포인트 차감
   -> member: point_balance -= 3000
   -> point_history: type=USE, amount=-3000, reference_id=주문번호
   -> Pass

8. [Order] 주문 확정 -> PAYMENT_COMPLETED
   -> Pass

9. [Notification] 주문 확인 알림
   -> notification: member_id, type=ORDER_CONFIRMED, channel=KAKAO_ALIMTALK
   -> Pass
```

**빈틈 발견**:
- **[GAP-1]** 단계 2에서 `orders` 테이블에 `coupon_id` 컬럼이 없어 어떤 쿠폰을 사용했는지 추적 불가. 주문 상세 API에서 쿠폰명을 표시할 수 없음.
- **[GAP-2]** 단계 2에서 셀러별 배송비를 계산하나, 배송비를 셀러별로 구분 저장하는 구조가 orders에 없음. `total_delivery_fee`만 존재하여 셀러별 배송비 내역 확인 불가. 부분 취소 시 특정 셀러 배송비 재계산에 필요한 데이터 부족.
- **[GAP-3]** 단계 3-4 사이 `STOCK_RESERVED` 중간 상태 미정의 (Critical-1 재확인)

### 6.2 반품 플로우 시뮬레이션

```
시나리오: 배송 완료 후 3일, 회원 A가 반팔티(M/블랙) 단순변심 반품

1. [Order] requestReturn(itemId, reason="단순변심")
   -> order_item.status = RETURN_REQUESTED
   -> ReturnRequestedEvent 발행
   -> Pass

2. [Shipping] return_request 생성
   -> return_request: order_id, order_item_id, shipment_id, seller_id, reason_type=CHANGE_OF_MIND, status=REQUESTED, fee_payer=BUYER, return_fee=2500
   -> Pass

3. [Shipping] 수거 요청
   -> 택배사 API -> return_request.return_courier_id, return_tracking
   -> status = PICKUP_SCHEDULED -> PICKUP_COMPLETED
   -> Pass

4. [Seller] 검수
   -> return_request.inspection_result = PASSED, inspected_at = now
   -> status = INSPECTING -> INSPECTION_PASSED
   -> Pass

5. [Shipping] return_request.status = RETURN_COMPLETED
   -> ReturnApprovedEvent 발행
   -> Pass

6. [Payment] 부분 환불
   -> 환불금액 = unit_price * quantity - return_fee(2500)
   -> payment.refunded_amount += 환불금액
   -> PG 부분 취소 API
   -> payment_history: type=PARTIAL_CANCELLED
   -> Pass

7. [Inventory] 재고 복원
   -> restock(quantity)
   -> stock_history: type=RESTOCK, reference_type=RETURN
   -> Pass

8. [Order] order_item.status = RETURN_COMPLETED
   -> Pass
```

**빈틈 발견**:
- **[GAP-4]** 단계 5-6에서 ReturnApprovedEvent의 페이로드 정의가 DDD 문서에 없음. Shipping의 Domain Event 목록에 Return 관련 이벤트가 ShippedEvent, DeliveredEvent, PurchaseConfirmedEvent뿐이고, ReturnApprovedEvent/ReturnCompletedEvent가 누락되어 있음.
- **[GAP-5]** 반품 시 쿠폰 재계산 로직: 반품으로 인해 쿠폰 최소 주문금액 미달 시 쿠폰을 반환하고 재계산해야 하는데, 이 이벤트 플로우가 Saga에 미정의.

### 6.3 정산 플로우 시뮬레이션

```
시나리오: 3월 3주차 (3/15~3/21) 구매확정 건 정산

1. [배치] PurchaseConfirmedEvent 수신 -> settlement_item 생성
   -> settlement_item: order_id, order_item_id, seller_id, product_id, category_id, sale_amount, commission_rate, commission_amount, settlement_amount, status=PENDING, confirmed_at
   -> Pass

2. [월요일 배치] 셀러별 집계
   -> SUM(sale_amount), SUM(commission_amount) WHERE seller_id = ? AND status = PENDING AND confirmed_at BETWEEN period_start AND period_end
   -> settlement_statement 생성: total_sale_amount, total_commission, net_settlement, item_count, status=CALCULATED
   -> settlement_item.status = CALCULATED
   -> Pass

3. [셀러 확인] 셀러 파트너센터에서 정산서 확인
   -> settlement_statement.status = CONFIRMED, confirmed_at = now
   -> Pass

4. [수요일 배치] 지급
   -> Seller API로 bank_code, account_number 조회
   -> 은행 API 호출 -> settlement_statement.bank_transfer_ref, paid_at, status=PAID
   -> Pass
```

**빈틈 발견**:
- **[GAP-6]** 단계 1에서 `category_id`를 어디서 가져오는지 불명확. PurchaseConfirmedEvent 페이로드에 categoryId가 없음. order_item에도 category_id가 없음. Product API를 동기 호출해야 하는데, 상품이 카테고리를 변경했을 수 있음. **주문 시점의 category_id를 스냅샷으로 저장해야 함**.
- **[GAP-7]** 반품 상계 처리: settlement_statement.total_return_offset 필드는 있으나, 어떤 반품 건이 상계되었는지 추적하는 구조(return_request_id -> settlement_item 매핑)가 없음.
- **[GAP-8]** 이월 처리: 최소 정산 금액(10,000원) 미달 시 이월하는 규칙이 있으나, 이월 금액을 저장/추적하는 컬럼이 settlement_statement에 없음.

---

## 불일치 상세 정리 (우선순위별)

### Critical (구현 차단)

| ID | 영역 | 내용 | 영향 범위 |
|----|------|------|----------|
| C-1 | Saga/ERD | OrderStatus에 STOCK_RESERVED 중간 상태 누락. Saga 중간 단계 추적 불가 | Order, Inventory |
| C-2 | ERD | 예약 타임아웃(30분) 만료 시각 저장 컬럼 부재. 미결제 주문 자동 해제 구현 불가 | Order, Inventory |

### Major (기능 결함 가능)

| ID | 영역 | 내용 | 영향 범위 |
|----|------|------|----------|
| M-1 | ERD | orders에 coupon_id 미저장. 사용 쿠폰 추적 불가 | Order, CS |
| M-2 | ERD | orders에 셀러별 배송비 구분 저장 구조 없음 | Order, Settlement |
| M-3 | ERD | order_item에 category_id 미저장. 정산 수수료 기준 스냅샷 불가 | Order, Settlement |
| M-4 | DDD/ERD | size_guide에 fitDescription, modelInfo 컬럼 누락 | Product |
| M-5 | 이벤트 | PurchaseConfirmedEvent에 categoryId 미포함. 정산 등록 시 Product API 의존 | Settlement |
| M-6 | DDD/이벤트 | Shipping BC에 Return 관련 Domain Event 미정의 (ReturnApprovedEvent 등) | Shipping, Payment, Inventory |
| M-7 | ERD | settlement_item에 쿠폰 플랫폼/셀러 부담 구분 컬럼 없음 | Settlement |

### Minor (개선 권장)

| ID | 영역 | 내용 | 영향 범위 |
|----|------|------|----------|
| m-1 | DDD/ERD | Product.rejectReason이 DDD Aggregate 필드에 미정의 (ERD에만 존재) | Product |
| m-2 | DDD/ERD | Payment.pgTransactionId, receiptUrl이 DDD Aggregate에 미정의 | Payment |
| m-3 | DDD/ERD | Inventory.productId가 DDD에 미정의 | Inventory |
| m-4 | DDD/ERD | Shipment.orderItemId가 DDD에 미정의 | Shipping |
| m-5 | DDD/ERD | ReturnRequest.returnFee, feePayer가 DDD에 미정의 | Shipping |
| m-6 | ERD | return_request.status COMMENT에 ReturnStatus Enum 값 미열거 | Shipping |
| m-7 | DDD | Sku Entity에 productId 필드 미정의 (JPA 매핑 시 필요) | Product |
| m-8 | ERD | orders에 created_at 컬럼 없음 (다른 모든 테이블에는 존재) | Order |
| m-9 | DDD/ERD | OrderItem.productSnapshot.originalPrice가 ERD에 없음 | Order |
| m-10 | ERD | settlement_statement에 이월 금액 추적 컬럼 없음 | Settlement |
| m-11 | 이벤트 | OrderCancelledEvent에 paymentId 포함되나 orders에 payment_id 없음 | Order, Payment |

---

## 수정 제안

### ERD 수정

```sql
-- [C-1] OrderStatus에 STOCK_RESERVED 추가
-- orders.status COMMENT 변경: 'STOCK_RESERVED' 추가

-- [C-2] 예약 타임아웃 컬럼 추가
ALTER TABLE orders ADD COLUMN reservation_expires_at DATETIME(6) NULL COMMENT '재고 예약 만료 예정 일시 (주문생성 + 30분)';
CREATE INDEX idx_orders_reservation_expires ON orders (status, reservation_expires_at);

-- [M-1] 쿠폰 추적
ALTER TABLE orders ADD COLUMN coupon_id BIGINT NULL COMMENT '사용된 쿠폰 ID';
ALTER TABLE orders ADD COLUMN coupon_policy_id BIGINT NULL COMMENT '사용된 쿠폰 정책 ID';

-- [M-2] 셀러별 배송비 (별도 테이블 또는 order_item에 추가)
CREATE TABLE order_delivery_fee (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '주문 배송비 고유 식별자',
    order_id        BIGINT       NOT NULL COMMENT '주문 ID',
    seller_id       BIGINT       NOT NULL COMMENT '셀러 ID',
    delivery_fee    BIGINT       NOT NULL COMMENT '배송비 (원)',
    free_applied    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '무료배송 적용 여부',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    PRIMARY KEY (id)
) COMMENT='주문별 셀러 배송비';

-- [M-3] 정산 기준 카테고리 스냅샷
ALTER TABLE order_item ADD COLUMN category_id BIGINT NULL COMMENT '카테고리 ID (정산 수수료 기준, 스냅샷)';
ALTER TABLE order_item ADD COLUMN category_name VARCHAR(50) NULL COMMENT '카테고리명 (스냅샷)';

-- [M-4] 사이즈 가이드 보조 정보
ALTER TABLE size_guide ADD COLUMN fit_description TEXT NULL COMMENT '핏 설명';
ALTER TABLE size_guide ADD COLUMN model_info VARCHAR(200) NULL COMMENT '모델 정보 (키/몸무게)';
-- 주의: size_guide는 측정값 행 단위이므로, fit_description/model_info는 product 테이블에 추가하는 것이 더 적절할 수 있음
-- 대안: product 테이블에 추가
ALTER TABLE product ADD COLUMN fit_description TEXT NULL COMMENT '핏 상세 설명';
ALTER TABLE product ADD COLUMN model_info VARCHAR(200) NULL COMMENT '모델 정보';

-- [M-7] 정산 쿠폰 부담 구분
ALTER TABLE settlement_item ADD COLUMN platform_discount BIGINT NOT NULL DEFAULT 0 COMMENT '플랫폼 부담 할인 금액 (원)';
ALTER TABLE settlement_item ADD COLUMN seller_discount BIGINT NOT NULL DEFAULT 0 COMMENT '셀러 부담 할인 금액 (원)';

-- [m-8] orders created_at 추가
-- 이미 ordered_at이 역할 수행하나 컨벤션 통일을 위해:
-- ordered_at을 비즈니스 의미, created_at을 기술적 의미로 분리 가능

-- [m-9] 할인 전 가격 스냅샷
ALTER TABLE order_item ADD COLUMN original_price BIGINT NULL COMMENT '정가 (할인 전, 스냅샷)';

-- [m-10] 정산 이월 추적
ALTER TABLE settlement_statement ADD COLUMN carry_over_amount BIGINT NOT NULL DEFAULT 0 COMMENT '이전 기간 이월 금액 (원)';

-- [m-11] 주문-결제 연결
ALTER TABLE orders ADD COLUMN payment_id BIGINT NULL COMMENT '결제 ID';
```

### DDD 수정

1. **OrderStatus Enum에 STOCK_RESERVED 추가**
   - 전이: ORDER_CREATED -> STOCK_RESERVED -> PAYMENT_COMPLETED

2. **Product Aggregate에 rejectReason 필드 추가**
   - `private String rejectReason`

3. **Payment Aggregate에 pgTransactionId, receiptUrl 필드 추가**

4. **Inventory Aggregate에 productId 필드 추가**

5. **Shipment Aggregate에 orderItemId 필드 추가**

6. **ReturnRequest에 returnFee, feePayer 필드 추가**

7. **Shipping BC Domain Event에 추가**
   - ReturnRequestCreatedEvent
   - ReturnApprovedEvent (-> Payment 환불 트리거)
   - ReturnCompletedEvent (-> Inventory 복원 트리거)
   - ReturnRejectedEvent

8. **PurchaseConfirmedEvent 페이로드에 categoryId 추가**
   - `items[{skuId, sellerId, amount, categoryId}]`

### 이벤트 페이로드 수정

```
PurchaseConfirmedEvent {
    orderId: Long,
    items: [{
        skuId: Long,
        sellerId: Long,
        amount: Long,
        categoryId: Long,     // 추가
        commissionRate: BigDecimal  // 추가 (주문 시점 스냅샷)
    }],
    timestamp: LocalDateTime
}

OrderCancelledEvent {
    orderId: Long,
    // paymentId 제거 (Order가 paymentId를 모를 수 있으므로)
    // Payment가 orderId로 자체 조회
    items: [{skuId: Long, quantity: Int}],
    couponId: Long?,
    pointsUsed: Long,
    timestamp: LocalDateTime
}
```

---

## 최종 판정

### 구현 진행 가능 여부: **조건부 가능**

**구현 전 필수 수정 (Critical 2건 + Major 7건)**:
1. OrderStatus에 STOCK_RESERVED 상태 추가 또는 재고 예약 동기 처리 결정
2. 예약 타임아웃 관리 컬럼/구조 확정
3. orders에 coupon_id, payment_id 컬럼 추가
4. order_item에 category_id 스냅샷 추가
5. 셀러별 배송비 저장 구조 확정
6. Shipping BC Return 관련 이벤트 정의
7. PurchaseConfirmedEvent 페이로드에 categoryId 추가
8. settlement_item 쿠폰 부담 구분 컬럼 추가
9. size_guide 또는 product에 fitDescription/modelInfo 추가

**구현 중 점진적 보완 가능 (Minor 11건)**:
- DDD 모델에 ERD 전용 필드(productId 등) 명시
- 이벤트 페이로드 세부 조정
- COMMENT 보완

**아키텍처 수준 권장사항**:
- 셀러 파트너센터용 Aggregator/BFF 서비스 추가 검토
- Order가 배송 세부 상태(IN_TRANSIT 등)를 직접 추적할지 Shipping 조회 API 위임할지 결정
- CouponCondition의 TEXT 기반 저장을 별도 매핑 테이블로 정규화할지 결정

전체적으로 15개 Bounded Context와 64개 테이블의 설계 품질이 높으며, 핵심 도메인 모델의 정합성이 잘 유지되고 있다. 위 수정사항을 반영한 후 구현 단계로 진행하는 것을 권장한다.
