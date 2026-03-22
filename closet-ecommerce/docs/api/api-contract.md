# API Contract Document

## 공통 규칙

### Base URL
```
/api/v1/{service}
```

### 응답 포맷
모든 API는 `ApiResponse<T>` 래퍼로 응답한다.

```json
// 성공
{
  "success": true,
  "data": { ... },
  "error": null
}

// 실패
{
  "success": false,
  "data": null,
  "error": {
    "code": "M001",
    "message": "이미 등록된 이메일입니다"
  }
}
```

### 인증
```
Authorization: Bearer {accessToken}
```
- 인증 불필요 API: 회원가입, 로그인, 토큰 재발급, 상품 목록/상세 조회
- 그 외 모든 API는 Access Token 필수

### 페이지네이션
```
?page=0&size=20&sort=createdAt,desc
```
- `page`: 0-based 페이지 번호
- `size`: 페이지 크기 (기본값 20, 최대 100)
- `sort`: 정렬 필드,방향

페이지네이션 응답:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### 에러 코드 체계
| Prefix | 서비스 |
|--------|--------|
| C | 공통 |
| M | 회원 (Member) |
| P | 상품 (Product) |
| O | 주문 (Order) |
| PAY | 결제 (Payment) |

---

## 1. Member (회원) 서비스

### 1.1 인증 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/member/auth/signup` | 회원가입 | X |
| POST | `/api/v1/member/auth/login` | 로그인 | X |
| POST | `/api/v1/member/auth/logout` | 로그아웃 | O |
| POST | `/api/v1/member/auth/reissue` | 토큰 재발급 | X (Refresh Token) |

#### POST /api/v1/member/auth/signup
회원가입

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  },
  "error": null
}
```

#### POST /api/v1/member/auth/login
로그인

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "error": null
}
```

#### POST /api/v1/member/auth/logout
로그아웃 (Access Token 블랙리스트 등록)

**Request Header:** `Authorization: Bearer {accessToken}`

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

#### POST /api/v1/member/auth/reissue
토큰 재발급

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "error": null
}
```

### 1.2 회원 프로필 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/member/me` | 내 정보 조회 | O |
| PUT | `/api/v1/member/me` | 내 정보 수정 | O |
| PUT | `/api/v1/member/me/password` | 비밀번호 변경 | O |
| DELETE | `/api/v1/member/me` | 회원 탈퇴 (Soft Delete) | O |

#### GET /api/v1/member/me
내 정보 조회

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "grade": "NORMAL",
    "createdAt": "2026-01-15T10:30:00"
  },
  "error": null
}
```

#### PUT /api/v1/member/me
내 정보 수정

**Request Body:**
```json
{
  "name": "김철수",
  "phone": "010-9876-5432"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "memberId": 1,
    "email": "user@example.com",
    "name": "김철수",
    "phone": "010-9876-5432",
    "grade": "NORMAL"
  },
  "error": null
}
```

#### PUT /api/v1/member/me/password
비밀번호 변경

**Request Body:**
```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

#### DELETE /api/v1/member/me
회원 탈퇴 (Soft Delete)

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

### 1.3 배송지 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/member/addresses` | 배송지 목록 조회 | O |
| POST | `/api/v1/member/addresses` | 배송지 등록 | O |
| PUT | `/api/v1/member/addresses/{addressId}` | 배송지 수정 | O |
| DELETE | `/api/v1/member/addresses/{addressId}` | 배송지 삭제 | O |
| PUT | `/api/v1/member/addresses/{addressId}/default` | 기본 배송지 설정 | O |

#### POST /api/v1/member/addresses
배송지 등록

**Request Body:**
```json
{
  "label": "집",
  "recipientName": "홍길동",
  "phone": "010-1234-5678",
  "zipCode": "06234",
  "address": "서울특별시 강남구 테헤란로 123",
  "addressDetail": "4층 401호",
  "isDefault": true
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "addressId": 1,
    "label": "집",
    "recipientName": "홍길동",
    "phone": "010-1234-5678",
    "zipCode": "06234",
    "address": "서울특별시 강남구 테헤란로 123",
    "addressDetail": "4층 401호",
    "isDefault": true
  },
  "error": null
}
```

---

## 2. Product (상품) 서비스

### 2.1 상품 조회 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/product/products` | 상품 목록 조회 | X |
| GET | `/api/v1/product/products/{productId}` | 상품 상세 조회 | X |
| GET | `/api/v1/product/products/search` | 상품 검색 | X |

#### GET /api/v1/product/products
상품 목록 조회 (페이지네이션)

**Query Parameters:**
- `page` (int, default: 0)
- `size` (int, default: 20)
- `sort` (string, default: "createdAt,desc")
- `categoryId` (long, optional)
- `minPrice` (int, optional)
- `maxPrice` (int, optional)
- `status` (string, optional: ACTIVE)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "클래식 화이트 셔츠",
        "price": 49000,
        "thumbnailUrl": "https://cdn.closet.com/products/1/thumb.jpg",
        "categoryName": "상의",
        "stockQuantity": 150,
        "status": "ACTIVE",
        "createdAt": "2026-03-01T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  },
  "error": null
}
```

#### GET /api/v1/product/products/{productId}
상품 상세 조회

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "productId": 1,
    "name": "클래식 화이트 셔츠",
    "description": "100% 면 소재의 클래식 화이트 셔츠입니다.",
    "price": 49000,
    "stockQuantity": 150,
    "sku": "SH-WHT-001",
    "status": "ACTIVE",
    "categoryId": 1,
    "categoryName": "상의",
    "images": [
      {
        "imageId": 1,
        "url": "https://cdn.closet.com/products/1/main.jpg",
        "sortOrder": 1
      }
    ],
    "options": [
      {
        "optionId": 1,
        "name": "사이즈",
        "values": ["S", "M", "L", "XL"]
      }
    ],
    "createdAt": "2026-03-01T09:00:00",
    "updatedAt": "2026-03-15T14:00:00"
  },
  "error": null
}
```

#### GET /api/v1/product/products/search
상품 검색

**Query Parameters:**
- `keyword` (string, required)
- `page`, `size`, `sort`

### 2.2 상품 관리 API (관리자)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/product/products` | 상품 등록 | O (ADMIN) |
| PUT | `/api/v1/product/products/{productId}` | 상품 수정 | O (ADMIN) |
| DELETE | `/api/v1/product/products/{productId}` | 상품 삭제 (Soft) | O (ADMIN) |
| PUT | `/api/v1/product/products/{productId}/status` | 상품 상태 변경 | O (ADMIN) |
| PUT | `/api/v1/product/products/{productId}/stock` | 재고 수정 | O (ADMIN) |

#### POST /api/v1/product/products
상품 등록

**Request Body:**
```json
{
  "name": "클래식 화이트 셔츠",
  "description": "100% 면 소재의 클래식 화이트 셔츠입니다.",
  "price": 49000,
  "stockQuantity": 150,
  "sku": "SH-WHT-001",
  "categoryId": 1,
  "images": [
    { "url": "https://cdn.closet.com/products/1/main.jpg", "sortOrder": 1 }
  ],
  "options": [
    { "name": "사이즈", "values": ["S", "M", "L", "XL"] }
  ]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "productId": 1,
    "name": "클래식 화이트 셔츠",
    "status": "DRAFT"
  },
  "error": null
}
```

#### PUT /api/v1/product/products/{productId}/status
상품 상태 변경

**Request Body:**
```json
{
  "status": "ACTIVE"
}
```

#### PUT /api/v1/product/products/{productId}/stock
재고 수정

**Request Body:**
```json
{
  "quantity": 200,
  "reason": "입고"
}
```

### 2.3 카테고리 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/product/categories` | 카테고리 목록 조회 | X |
| POST | `/api/v1/product/categories` | 카테고리 등록 | O (ADMIN) |
| PUT | `/api/v1/product/categories/{categoryId}` | 카테고리 수정 | O (ADMIN) |
| DELETE | `/api/v1/product/categories/{categoryId}` | 카테고리 삭제 | O (ADMIN) |

#### GET /api/v1/product/categories
카테고리 목록 조회 (트리 구조)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "categoryId": 1,
      "name": "상의",
      "parentId": null,
      "sortOrder": 1,
      "children": [
        {
          "categoryId": 3,
          "name": "셔츠",
          "parentId": 1,
          "sortOrder": 1,
          "children": []
        }
      ]
    }
  ],
  "error": null
}
```

---

## 3. Order (주문) 서비스

### 3.1 주문 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/order/orders` | 주문 생성 | O |
| GET | `/api/v1/order/orders` | 내 주문 목록 조회 | O |
| GET | `/api/v1/order/orders/{orderId}` | 주문 상세 조회 | O |
| POST | `/api/v1/order/orders/{orderId}/cancel` | 주문 취소 요청 | O |
| POST | `/api/v1/order/orders/{orderId}/confirm` | 구매 확정 | O |
| POST | `/api/v1/order/orders/{orderId}/return` | 반품 요청 | O |

#### POST /api/v1/order/orders
주문 생성

**Request Body:**
```json
{
  "orderItems": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 49000,
      "options": {
        "사이즈": "M"
      }
    },
    {
      "productId": 3,
      "quantity": 1,
      "price": 89000,
      "options": {
        "사이즈": "L",
        "색상": "네이비"
      }
    }
  ],
  "shippingAddressId": 1,
  "paymentMethod": "CARD"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20260322-0001",
    "status": "PENDING",
    "totalAmount": 187000,
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "클래식 화이트 셔츠",
        "quantity": 2,
        "price": 49000,
        "subtotal": 98000
      },
      {
        "orderItemId": 2,
        "productId": 3,
        "productName": "슬림핏 치노 팬츠",
        "quantity": 1,
        "price": 89000,
        "subtotal": 89000
      }
    ],
    "createdAt": "2026-03-22T10:00:00"
  },
  "error": null
}
```

#### GET /api/v1/order/orders
내 주문 목록 조회 (페이지네이션)

**Query Parameters:**
- `page`, `size`, `sort`
- `status` (string, optional)
- `startDate` (date, optional)
- `endDate` (date, optional)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 1,
        "orderNumber": "ORD-20260322-0001",
        "status": "PAID",
        "totalAmount": 187000,
        "itemCount": 2,
        "firstItemName": "클래식 화이트 셔츠 외 1건",
        "createdAt": "2026-03-22T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  },
  "error": null
}
```

#### GET /api/v1/order/orders/{orderId}
주문 상세 조회

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20260322-0001",
    "status": "PAID",
    "totalAmount": 187000,
    "orderItems": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "클래식 화이트 셔츠",
        "quantity": 2,
        "price": 49000,
        "subtotal": 98000,
        "options": { "사이즈": "M" }
      }
    ],
    "shippingAddress": {
      "recipientName": "홍길동",
      "phone": "010-1234-5678",
      "zipCode": "06234",
      "address": "서울특별시 강남구 테헤란로 123",
      "addressDetail": "4층 401호"
    },
    "payment": {
      "paymentId": 1,
      "method": "CARD",
      "amount": 187000,
      "status": "APPROVED",
      "approvedAt": "2026-03-22T10:01:00"
    },
    "createdAt": "2026-03-22T10:00:00",
    "updatedAt": "2026-03-22T10:01:00"
  },
  "error": null
}
```

#### POST /api/v1/order/orders/{orderId}/cancel
주문 취소 요청

**Request Body:**
```json
{
  "reason": "단순 변심"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "status": "CANCEL_REQUESTED",
    "cancelReason": "단순 변심"
  },
  "error": null
}
```

#### POST /api/v1/order/orders/{orderId}/confirm
구매 확정

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "status": "CONFIRMED",
    "confirmedAt": "2026-03-25T14:00:00"
  },
  "error": null
}
```

#### POST /api/v1/order/orders/{orderId}/return
반품 요청

**Request Body:**
```json
{
  "reason": "상품 불량",
  "returnItems": [
    { "orderItemId": 1, "quantity": 1 }
  ]
}
```

### 3.2 주문 관리 API (관리자)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/order/admin/orders` | 전체 주문 목록 조회 | O (ADMIN) |
| PUT | `/api/v1/order/admin/orders/{orderId}/status` | 주문 상태 변경 | O (ADMIN) |
| POST | `/api/v1/order/admin/orders/{orderId}/approve-cancel` | 취소 승인 | O (ADMIN) |
| POST | `/api/v1/order/admin/orders/{orderId}/approve-return` | 반품 승인 | O (ADMIN) |

---

## 4. Payment (결제) 서비스

### 4.1 결제 API

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/payment/payments/prepare` | 결제 준비 (PG 키 발급) | O |
| POST | `/api/v1/payment/payments/approve` | 결제 승인 | O |
| GET | `/api/v1/payment/payments/{paymentId}` | 결제 상세 조회 | O |
| GET | `/api/v1/payment/payments` | 내 결제 내역 조회 | O |
| POST | `/api/v1/payment/payments/{paymentId}/cancel` | 결제 전체 취소 | O |
| POST | `/api/v1/payment/payments/{paymentId}/partial-cancel` | 부분 취소 | O |

#### POST /api/v1/payment/payments/prepare
결제 준비 (PG 결제 키 발급)

**Request Body:**
```json
{
  "orderId": 1,
  "amount": 187000,
  "method": "CARD",
  "orderName": "클래식 화이트 셔츠 외 1건"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "paymentKey": "toss_payment_key_xxx",
    "orderId": 1,
    "amount": 187000,
    "status": "READY"
  },
  "error": null
}
```

#### POST /api/v1/payment/payments/approve
결제 승인 (PG 승인 요청)

**Request Body:**
```json
{
  "paymentKey": "toss_payment_key_xxx",
  "orderId": 1,
  "amount": 187000
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "paymentKey": "toss_payment_key_xxx",
    "orderId": 1,
    "amount": 187000,
    "method": "CARD",
    "status": "APPROVED",
    "approvedAt": "2026-03-22T10:01:00",
    "receipt": {
      "url": "https://dashboard.tosspayments.com/receipt/..."
    }
  },
  "error": null
}
```

#### GET /api/v1/payment/payments/{paymentId}
결제 상세 조회

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "paymentKey": "toss_payment_key_xxx",
    "orderId": 1,
    "orderNumber": "ORD-20260322-0001",
    "amount": 187000,
    "method": "CARD",
    "status": "APPROVED",
    "approvedAt": "2026-03-22T10:01:00",
    "cardInfo": {
      "company": "신한카드",
      "number": "****-****-****-1234",
      "installmentPlanMonths": 0
    },
    "cancellations": []
  },
  "error": null
}
```

#### POST /api/v1/payment/payments/{paymentId}/cancel
결제 전체 취소

**Request Body:**
```json
{
  "cancelReason": "고객 요청에 의한 취소"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "status": "CANCELLED",
    "cancelAmount": 187000,
    "cancelReason": "고객 요청에 의한 취소",
    "cancelledAt": "2026-03-22T11:00:00"
  },
  "error": null
}
```

#### POST /api/v1/payment/payments/{paymentId}/partial-cancel
부분 취소

**Request Body:**
```json
{
  "cancelAmount": 49000,
  "cancelReason": "부분 상품 반품"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "status": "PARTIAL_CANCELLED",
    "totalAmount": 187000,
    "cancelledAmount": 49000,
    "remainingAmount": 138000,
    "cancelReason": "부분 상품 반품",
    "cancelledAt": "2026-03-22T11:00:00"
  },
  "error": null
}
```

### 4.2 결제 관리 API (관리자)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/payment/admin/payments` | 전체 결제 내역 조회 | O (ADMIN) |
| POST | `/api/v1/payment/admin/payments/{paymentId}/force-cancel` | 강제 취소 | O (ADMIN) |
