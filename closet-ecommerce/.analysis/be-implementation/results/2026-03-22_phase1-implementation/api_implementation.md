# Phase 1 API 구현 현황

> 작성일: 2026-03-22
> 소스 코드 기반 정리 (Controller + Gateway 라우팅)

---

## 1. Gateway 라우팅 설정

**포트**: 8080 (Spring Cloud Gateway, WebFlux 기반)

### 1.1 라우트 테이블

| Route ID | URI (로컬) | URI (Docker) | Path Predicates |
|----------|-----------|-------------|-----------------|
| member-service | http://localhost:8081 | http://closet-member:8081 | `/api/v1/members/**` |
| product-service | http://localhost:8082 | http://closet-product:8082 | `/api/v1/products/**`, `/api/v1/categories/**`, `/api/v1/brands/**` |
| order-service | http://localhost:8083 | http://closet-order:8083 | `/api/v1/orders/**`, `/api/v1/carts/**` |
| payment-service | http://localhost:8084 | http://closet-payment:8084 | `/api/v1/payments/**` |
| bff-service | http://localhost:8085 | http://closet-bff:8085 | `/api/v1/bff/**` |

### 1.2 인증 (JwtAuthenticationFilter)

Gateway 레벨에서 JWT 검증 후 `X-Member-Id` 헤더를 하위 서비스로 전파한다.

**인증 불필요 경로 (Public Paths):**

| Path | 조건 | 설명 |
|------|------|------|
| `/api/v1/members/register` | POST | 회원가입 |
| `/api/v1/members/login` | POST | 로그인 |
| `/api/v1/members/auth/refresh` | POST | 토큰 갱신 |
| `/api/v1/products/**` | GET만 | 상품 목록/상세 조회 |
| `/api/v1/categories/**` | GET만 | 카테고리 조회 |
| `/api/v1/brands/**` | GET만 | 브랜드 조회 |
| `/api/v1/bff/products/**` | GET만 | BFF 상품 조회 |
| `/api/v1/bff/auth/**` | POST | BFF 인증 (회원가입/로그인) |

**인증 필요 경로**: 위 외 모든 경로. `Authorization: Bearer {token}` 헤더 필수.

### 1.3 필터 체인

| 순서 | 필터 | 설명 |
|------|------|------|
| -2 | RequestLoggingFilter | 요청/응답 로깅 (method, path, route, 소요시간 ms) |
| -1 | JwtAuthenticationFilter | JWT 검증 + `X-Member-Id` 헤더 전파 |

### 1.4 CORS 설정

- 허용 Origin: `http://localhost:3000` (Web), `http://localhost:19006` (Mobile/Expo)
- 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Credentials: true
- Max Age: 3600초

### 1.5 Rate Limiting

- IP 기반 KeyResolver (`remoteAddress.hostAddress`)

### 1.6 Actuator

노출 엔드포인트: `health`, `info`, `gateway`, `prometheus`, `metrics`

---

## 2. Member Service API (포트 8081)

### 2.1 MemberController (`/api/v1/members`)

| Method | Path | 설명 | 인증 | Request Body | Response |
|--------|------|------|------|-------------|----------|
| POST | `/api/v1/members/register` | 회원가입 | 불필요 | `RegisterRequest(email, password, name, phone?)` | `201` `ApiResponse<MemberResponse>` |
| POST | `/api/v1/members/login` | 로그인 | 불필요 | `LoginRequest(email, password)` | `200` `ApiResponse<LoginResponse>` |
| GET | `/api/v1/members/me` | 내 정보 조회 | **필요** (JWT) | - | `200` `ApiResponse<MemberResponse>` |
| DELETE | `/api/v1/members/me` | 회원 탈퇴 | **필요** (JWT) | - | `204` No Content |
| POST | `/api/v1/members/auth/refresh` | 토큰 갱신 | 불필요 | `RefreshTokenRequest(refreshToken)` | `200` `ApiResponse<LoginResponse>` |

**Validation 규칙:**
- `email`: NotBlank, Email 형식
- `password`: NotBlank, 8~50자
- `name`: NotBlank, 2~50자

### 2.2 ShippingAddressController (`/api/v1/members/me/addresses`)

| Method | Path | 설명 | 인증 | Request Body | Response |
|--------|------|------|------|-------------|----------|
| POST | `/api/v1/members/me/addresses` | 배송지 등록 | **필요** | `ShippingAddressRequest` | `201` `ApiResponse<ShippingAddressResponse>` |
| GET | `/api/v1/members/me/addresses` | 배송지 목록 조회 | **필요** | - | `200` `ApiResponse<List<ShippingAddressResponse>>` |
| PUT | `/api/v1/members/me/addresses/{id}` | 배송지 수정 | **필요** | `ShippingAddressRequest` | `200` `ApiResponse<ShippingAddressResponse>` |
| DELETE | `/api/v1/members/me/addresses/{id}` | 배송지 삭제 | **필요** | - | `204` No Content |
| PATCH | `/api/v1/members/me/addresses/{id}/default` | 기본 배송지 설정 | **필요** | - | `200` `ApiResponse<ShippingAddressResponse>` |

**ShippingAddressRequest Validation:**
- `name`: NotBlank, max 50자
- `phone`: NotBlank, max 20자
- `zipCode`: NotBlank, max 10자
- `address`: NotBlank, max 200자
- `detailAddress`: max 200자 (선택)

---

## 3. Product Service API (포트 8082)

### 3.1 ProductController (`/api/v1/products`)

| Method | Path | 설명 | 인증 | Request Body / Params | Response |
|--------|------|------|------|-----------------------|----------|
| POST | `/api/v1/products` | 상품 생성 | **필요** | `ProductCreateRequest` | `201` `ApiResponse<ProductResponse>` |
| PUT | `/api/v1/products/{id}` | 상품 수정 | **필요** | `ProductUpdateRequest` | `200` `ApiResponse<ProductResponse>` |
| GET | `/api/v1/products/{id}` | 상품 단건 조회 | 불필요 | - | `200` `ApiResponse<ProductResponse>` |
| GET | `/api/v1/products` | 상품 목록 조회 | 불필요 | `?categoryId&brandId&minPrice&maxPrice&status&page&size` | `200` `ApiResponse<Page<ProductListResponse>>` |
| PATCH | `/api/v1/products/{id}/status` | 상품 상태 변경 | **필요** | `ProductStatusChangeRequest(status)` | `200` `ApiResponse<ProductResponse>` |
| POST | `/api/v1/products/{id}/options` | 옵션 추가 | **필요** | `ProductOptionCreateRequest` | `201` `ApiResponse<ProductOptionResponse>` |
| DELETE | `/api/v1/products/{id}/options/{optionId}` | 옵션 삭제 | **필요** | - | `204` No Content |

**ProductCreateRequest Validation:**
- `name`: NotBlank
- `description`: NotBlank
- `brandId`: NotNull
- `categoryId`: NotNull
- `basePrice`: Min(0)
- `salePrice`: Min(0)

**ProductOptionCreateRequest Validation:**
- `size`: NotNull (Size enum)
- `colorName`: NotBlank
- `colorHex`: NotBlank
- `skuCode`: NotBlank

**필터 파라미터 (GET /api/v1/products):**
- `categoryId` (Long, 선택)
- `brandId` (Long, 선택)
- `minPrice` (BigDecimal, 선택)
- `maxPrice` (BigDecimal, 선택)
- `status` (ProductStatus, 선택)
- `page` (default 0), `size` (default 20) -- Pageable

### 3.2 BrandController (`/api/v1/brands`)

| Method | Path | 설명 | 인증 | Request Body | Response |
|--------|------|------|------|-------------|----------|
| GET | `/api/v1/brands` | 전체 브랜드 조회 | 불필요 | - | `200` `ApiResponse<List<BrandResponse>>` |
| POST | `/api/v1/brands` | 브랜드 생성 | **필요** | `BrandCreateRequest(name, logoUrl?, description?, sellerId)` | `201` `ApiResponse<BrandResponse>` |

### 3.3 CategoryController (`/api/v1/categories`)

| Method | Path | 설명 | 인증 | Request Body | Response |
|--------|------|------|------|-------------|----------|
| GET | `/api/v1/categories` | 트리 구조 카테고리 조회 | 불필요 | - | `200` `ApiResponse<List<CategoryResponse>>` |
| POST | `/api/v1/categories` | 카테고리 생성 | **필요** | `CategoryCreateRequest(name, parentId?, depth, sortOrder)` | `201` `ApiResponse<CategoryResponse>` |

---

## 4. Order Service API (포트 8083)

### 4.1 OrderController (`/api/v1/orders`)

| Method | Path | 설명 | 인증 | Request Body / Params | Response |
|--------|------|------|------|-----------------------|----------|
| POST | `/api/v1/orders` | 주문 생성 | **필요** | `CreateOrderRequest` | `201` `ApiResponse<OrderResponse>` |
| GET | `/api/v1/orders/{id}` | 주문 단건 조회 | **필요** | - | `200` `ApiResponse<OrderResponse>` |
| GET | `/api/v1/orders` | 회원 주문 목록 | **필요** | `?memberId&page&size` | `200` `ApiResponse<Page<OrderResponse>>` |
| POST | `/api/v1/orders/{id}/cancel` | 주문 취소 | **필요** | `CancelOrderRequest(reason)` | `200` `ApiResponse<OrderResponse>` |

**CreateOrderRequest Validation:**
- `memberId`: NotNull
- `sellerId`: NotNull
- `items`: NotEmpty (1개 이상)
- `receiverName`: NotBlank
- `receiverPhone`: NotBlank
- `zipCode`: NotBlank
- `address`: NotBlank
- `detailAddress`: NotBlank
- `shippingFee`: default 0
- `discountAmount`: default 0

**CreateOrderItemRequest Validation:**
- `productId`: NotNull
- `productOptionId`: NotNull
- `productName`: NotBlank
- `optionName`: NotBlank
- `categoryId`: NotNull
- `quantity`: Min(1)
- `unitPrice`: NotNull

### 4.2 CartController (`/api/v1/carts`)

| Method | Path | 설명 | 인증 | Request Body / Params | Response |
|--------|------|------|------|-----------------------|----------|
| POST | `/api/v1/carts/items` | 장바구니 항목 추가 | **필요** | `AddCartItemRequest(memberId, productId, productOptionId, quantity, unitPrice)` | `201` `ApiResponse<CartResponse>` |
| GET | `/api/v1/carts` | 장바구니 조회 | **필요** | `?memberId` | `200` `ApiResponse<CartResponse>` |
| PUT | `/api/v1/carts/items/{itemId}` | 수량 변경 | **필요** | `UpdateCartItemRequest(quantity)` | `200` `ApiResponse<CartResponse>` |
| DELETE | `/api/v1/carts/items/{itemId}` | 항목 삭제 | **필요** | - | `204` No Content |

---

## 5. Payment Service API (포트 8084)

**스켈레톤 상태** -- Application 클래스만 존재. API 미구현.

BFF `PaymentServiceClient`에서 예상되는 API:
- `GET /api/v1/payments/orders/{orderId}` -- 주문별 결제 조회
- `POST /api/v1/payments/confirm` -- 결제 승인
- `POST /api/v1/payments/{id}/cancel` -- 결제 취소

---

## 6. BFF Service API (포트 8085)

### 6.1 BffAuthController (`/api/v1/bff/auth`)

| Method | Path | 설명 | 인증 | Request Body | Response |
|--------|------|------|------|-------------|----------|
| POST | `/api/v1/bff/auth/register` | 회원가입 | 불필요 | `RegisterRequest(email, password, name, phone?)` | `201` 프록시 응답 |
| POST | `/api/v1/bff/auth/login` | 로그인 | 불필요 | `LoginRequest(email, password)` | `200` 프록시 응답 |

### 6.2 BffProductController (`/api/v1/bff`)

| Method | Path | 설명 | 인증 | Response |
|--------|------|------|------|----------|
| GET | `/api/v1/bff/products/{id}` | 상품 상세 (집계) | 불필요 | `ApiResponse<ProductDetailBffResponse>` |
| GET | `/api/v1/bff/home` | 홈 페이지 데이터 | 불필요 | `ApiResponse<HomeBffResponse>` |

### 6.3 BffOrderController (`/api/v1/bff`)

| Method | Path | 설명 | 인증 | Request | Response |
|--------|------|------|------|---------|----------|
| GET | `/api/v1/bff/orders/{id}` | 주문 상세 (집계) | **필요** | - | `ApiResponse<OrderDetailBffResponse>` |
| GET | `/api/v1/bff/checkout` | 체크아웃 페이지 | **필요** | Header: `X-Member-Id` | `ApiResponse<CheckoutBffResponse>` |
| POST | `/api/v1/bff/orders` | 주문 생성 | **필요** | Header: `X-Member-Id` + `CreateOrderBffRequest` | `201` 응답 |
| POST | `/api/v1/bff/orders/{orderId}/pay` | 결제 확인 | **필요** | `ConfirmPaymentBffRequest` | `200` 응답 |
| POST | `/api/v1/bff/orders/{orderId}/cancel` | 주문 취소 | **필요** | `CancelRequest(reason)` | `200` 응답 |

### 6.4 BffCartController (`/api/v1/bff/cart`)

| Method | Path | 설명 | 인증 | Request | Response |
|--------|------|------|------|---------|----------|
| POST | `/api/v1/bff/cart/items` | 장바구니 항목 추가 | **필요** | Header: `X-Member-Id` + `AddCartItemRequest` | `201` 응답 |
| PUT | `/api/v1/bff/cart/items/{itemId}` | 수량 변경 | **필요** | `UpdateQuantityRequest(quantity)` | `200` 응답 |
| DELETE | `/api/v1/bff/cart/items/{itemId}` | 항목 삭제 | **필요** | - | `204` No Content |

### 6.5 BffAddressController (`/api/v1/bff/addresses`)

| Method | Path | 설명 | 인증 | Request | Response |
|--------|------|------|------|---------|----------|
| POST | `/api/v1/bff/addresses` | 배송지 등록 | **필요** | Header: `X-Member-Id` + `AddAddressRequest` | `201` 응답 |
| PUT | `/api/v1/bff/addresses/{id}` | 배송지 수정 | **필요** | Header: `X-Member-Id` + `UpdateAddressRequest` | `200` 응답 |
| DELETE | `/api/v1/bff/addresses/{id}` | 배송지 삭제 | **필요** | Header: `X-Member-Id` | `204` No Content |
| PATCH | `/api/v1/bff/addresses/{id}/default` | 기본 배송지 설정 | **필요** | Header: `X-Member-Id` | `200` 응답 |

### 6.6 BffMyPageController (`/api/v1/bff`)

| Method | Path | 설명 | 인증 | Response |
|--------|------|------|------|----------|
| GET | `/api/v1/bff/mypage` | 마이페이지 종합 | **필요** | `ApiResponse<MyPageBffResponse>` |

---

## 7. API 전체 요약

### 7.1 서비스별 엔드포인트 수

| 서비스 | 엔드포인트 수 | 인증 필요 | 인증 불필요 |
|--------|-------------|----------|-----------|
| Member | 10 | 6 | 4 |
| Product | 11 | 5 | 6 |
| Order | 8 | 8 | 0 |
| Payment | 0 (미구현) | - | - |
| BFF | 14 | 10 | 4 |
| **합계** | **43** | **29** | **14** |

### 7.2 공통 응답 포맷

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "C001",
    "message": "잘못된 입력값입니다",
    "details": ["email: 이메일 형식이 올바르지 않습니다"]
  }
}
```

### 7.3 HTTP 상태 코드 규칙

| 상태 코드 | 사용 시점 |
|----------|----------|
| 200 OK | 조회, 수정, 상태 변경 성공 |
| 201 Created | 생성 성공 (POST) |
| 204 No Content | 삭제 성공 (DELETE), 탈퇴 |
| 400 Bad Request | 유효성 검증 실패, 잘못된 상태 전이 |
| 401 Unauthorized | JWT 없음/만료/무효 |
| 403 Forbidden | 접근 권한 없음 |
| 404 Not Found | 엔티티 미존재 |
| 409 Conflict | 중복 엔티티 (이메일 등) |
| 500 Internal Server Error | 서버 오류 |

### 7.4 인증 흐름

```
Client -> Gateway(:8080) -> [JwtAuthenticationFilter]
                              |
                              |- Public Path -> 바로 라우팅
                              |- Protected Path -> JWT 검증
                                  |
                                  |- 유효 -> X-Member-Id 헤더 추가 후 라우팅
                                  |- 무효 -> 401 Unauthorized
```

Member 서비스 자체 인증 필터 (JwtAuthenticationFilter):
- `OncePerRequestFilter` 기반
- `Authorization: Bearer {token}` -> `request.setAttribute("memberId", memberId)`
- 제외 경로: `/api/v1/members/register`, `/api/v1/members/login`, `/api/v1/auth/refresh`
