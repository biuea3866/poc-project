# [Ticket #17] Order API (주문 생성/조회/취소)

## 개요
- TDD 참조: tdd.md 섹션 4.2 (presentation/OrderController), 4.4 (플랜 구독 결제 흐름), 4.5 (크레딧 충전 흐름), 8.2 (ONE_TIME 시퀀스)
- 선행 티켓: #8, #9
- 크기: M

## 작업 내용

### 변경 사항

#### 1. OrderController 구현
- `POST /api/v1/orders` — 주문 생성
  - Request: `{ productCode, orderType, workspaceId, idempotencyKey?, memo? }`
  - 상품 유효성 검증 (Product.isActive, 현재 유효 가격 존재 여부)
  - OrderNumberGenerator로 주문번호 생성 (`ORD-{yyyyMMdd}-{UUID 8자리}`)
  - Order + OrderItem 생성 (가격 스냅샷 저장)
  - idempotencyKey 중복 시 기존 주문 반환 (201이 아닌 200)
  - Response: OrderDetailResponse (orderNumber, status, items, amounts)

- `GET /api/v1/orders/{orderNumber}` — 주문 상세 조회
  - orderNumber로 조회 (id 노출 안 함)
  - workspace 소속 검증 (다른 workspace 주문 접근 불가)
  - Response: OrderDetailResponse (주문 정보 + 항목 + 결제 정보)

- `GET /api/v1/orders?workspaceId=&status=&page=&size=` — 주문 목록 조회
  - 필수: workspaceId
  - 선택: status (다중 선택 가능), orderType
  - 페이지네이션: page (0-based), size (default 20, max 100)
  - 정렬: createdAt DESC
  - Response: Page<OrderSummaryResponse>

- `PATCH /api/v1/orders/{orderNumber}/cancel` — 주문 취소
  - CREATED 상태에서만 취소 가능 (상태머신 규칙 준수)
  - Request: `{ cancelReason? }`
  - Order 상태 → CANCELLED, OrderStatusHistory 자동 기록 (#22)
  - Response: OrderDetailResponse

#### 2. Request/Response DTO 정의
- `CreateOrderRequest`: productCode, orderType(NEW/PURCHASE), workspaceId, idempotencyKey?, memo?
- `CancelOrderRequest`: cancelReason?
- `OrderDetailResponse`: orderNumber, workspaceId, orderType, status, items[], totalAmount, originalAmount, discountAmount, vatAmount, currency, payment?, createdAt, updatedAt
- `OrderItemResponse`: productCode, productName, productType, quantity, unitPrice, totalPrice
- `OrderSummaryResponse`: orderNumber, orderType, status, totalAmount, productName, createdAt
- `OrderPaymentResponse`: paymentMethod, status, amount, approvedAt, receiptUrl

#### 3. 권한 처리
- 커스텀 어노테이션 또는 Spring Security PreAuthorize로 OWNER/MANAGER 역할만 허용
- workspaceId에 대한 소속 검증 (요청자가 해당 workspace의 멤버인지)

#### 4. 예외 처리
- ProductNotFoundException: 상품 코드 없음
- ProductInactiveException: 비활성 상품 주문 시도
- OrderNotFoundException: 주문번호 없음
- OrderStatusTransitionException: 잘못된 상태 전이 (예: PAID 상태에서 cancel 호출)
- WorkspaceAccessDeniedException: 다른 workspace 주문 접근
- DuplicateIdempotencyKeyException 대신 기존 주문 반환

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | presentation | presentation/OrderController.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/CreateOrderRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/CancelOrderRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/OrderDetailResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/OrderSummaryResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/OrderItemResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/order/OrderPaymentResponse.kt | 신규 |
| greeting_payment-server | application | application/OrderService.kt | 수정 (API용 메서드 추가) |
| greeting_payment-server | application | application/dto/OrderSearchCriteria.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/OrderRepository.kt | 수정 (검색 쿼리 추가) |
| greeting_payment-server | presentation | presentation/exception/PaymentExceptionHandler.kt | 수정 (주문 관련 예외 핸들러 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T17-01 | 주문 생성 성공 | 유효한 상품(PLAN_STANDARD), OWNER 권한 | POST /api/v1/orders | 201 Created, Order(CREATED), OrderItem 가격 스냅샷 저장 |
| T17-02 | 주문 상세 조회 | 존재하는 주문, 동일 workspace 사용자 | GET /api/v1/orders/{orderNumber} | 200 OK, 주문+항목+결제 정보 반환 |
| T17-03 | 주문 목록 조회 (기본) | workspace에 주문 5건 존재 | GET /api/v1/orders?workspaceId=1&page=0&size=20 | 200 OK, 5건 반환, createdAt DESC |
| T17-04 | 주문 목록 필터링 | COMPLETED 2건, CANCELLED 1건 | GET /api/v1/orders?workspaceId=1&status=COMPLETED | 200 OK, 2건만 반환 |
| T17-05 | 주문 취소 성공 | CREATED 상태 주문 | PATCH /api/v1/orders/{orderNumber}/cancel | 200 OK, status=CANCELLED |
| T17-06 | 멱등성 키 중복 시 기존 주문 반환 | 동일 idempotencyKey 주문 이미 존재 | POST /api/v1/orders (동일 idempotencyKey) | 200 OK, 기존 주문 반환 (새 주문 생성 안 함) |
| T17-07 | CONSUMABLE 상품 주문 생성 | SMS_PACK_1000 상품 | POST /api/v1/orders (type=PURCHASE) | 201 Created, OrderItem에 SMS 팩 정보 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T17-E01 | 존재하지 않는 상품 코드 | productCode=INVALID_CODE | POST /api/v1/orders | 404, ProductNotFoundException |
| T17-E02 | 비활성 상품 주문 시도 | product.isActive=false | POST /api/v1/orders | 400, ProductInactiveException |
| T17-E03 | PAID 상태에서 취소 시도 | PAID 상태 주문 | PATCH /cancel | 409, OrderStatusTransitionException |
| T17-E04 | 다른 workspace 주문 조회 | workspaceId=1 주문을 workspaceId=2 사용자가 | GET /api/v1/orders/{orderNumber} | 403, WorkspaceAccessDeniedException |
| T17-E05 | MEMBER 권한으로 주문 생성 | MEMBER 역할 사용자 | POST /api/v1/orders | 403 Forbidden |
| T17-E06 | 존재하지 않는 주문번호 | orderNumber=ORD-000-INVALID | GET /api/v1/orders/{orderNumber} | 404, OrderNotFoundException |
| T17-E07 | 페이지 크기 초과 | size=200 | GET /api/v1/orders?size=200 | 400, size는 max 100 |
| T17-E08 | 가격 정책 없는 상품 | 유효 기간 내 가격 정책 없음 | POST /api/v1/orders | 500, ProductPriceNotFoundException |

## 기대 결과 (AC)
- [ ] POST /api/v1/orders로 주문 생성 시 Order(CREATED) + OrderItem이 정상 생성되고, 상품 가격이 스냅샷으로 저장된다
- [ ] GET /api/v1/orders/{orderNumber}로 주문 상세 조회 시 주문, 항목, 결제 정보가 함께 반환된다
- [ ] GET /api/v1/orders로 목록 조회 시 workspaceId 필수, status 필터, 페이지네이션이 정상 동작한다
- [ ] PATCH /api/v1/orders/{orderNumber}/cancel로 CREATED 상태 주문만 취소 가능하다
- [ ] OWNER/MANAGER 권한만 모든 주문 API에 접근 가능하다
- [ ] 다른 workspace의 주문에는 접근할 수 없다
- [ ] idempotencyKey 중복 시 새 주문을 만들지 않고 기존 주문을 반환한다
- [ ] 모든 에러 응답은 통일된 에러 응답 형식(errorCode, message)을 따른다
