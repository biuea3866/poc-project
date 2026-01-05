# 🔗 외부 채널 연동 스펙 (EXTERNAL_INTEGRATION_SPEC.md)

> **담당**: LLM Agent (Mock Server 구현)
> **목적**: 실제 오픈마켓 채널 API를 모방한 Mock Server 구현

---

## 개요

### 연동 대상 채널
| 채널 | 코드 | Mock Port | 실제 API 기반 |
|------|------|-----------|--------------|
| 11번가 | ST11 | 8082 | SK플래닛 OpenAPI |
| 네이버 스마트스토어 | NAVER_STORE | 8082 | 커머스 API |
| 카카오 스토어 | KAKAO_STORE | 8082 | 카카오 커머스 API |
| 토스 스토어 | TOSS_STORE | 8082 | 토스 셀러 API |
| 쿠팡 | COUPANG | 8082 | 쿠팡 Wing API |

### 공통 아키텍처
```
┌──────────────────────────────────────────────────────────────┐
│                    Channel Mock Server                        │
│                      (Express.js)                             │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    Route Layer                          │ │
│  │  /api/st11/*  /api/naver/*  /api/kakao/*  /api/toss/*  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                            │                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   Service Layer                         │ │
│  │  각 채널별 비즈니스 로직 처리                              │ │
│  └─────────────────────────────────────────────────────────┘ │
│                            │                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   Storage Layer                         │ │
│  │  SQLite (Mock 데이터 저장) + Scenarios (테스트 시나리오) │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

---

## 1. 11번가 (ST11) Mock API

### 인증
```
POST /api/st11/auth/token
Content-Type: application/json

Request:
{
  "openapiKey": "test-api-key",
  "secretKey": "test-secret-key"
}

Response:
{
  "code": "200",
  "message": "성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 3600
  }
}
```

### 상품 등록
```
POST /api/st11/products
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "productName": "테스트 상품",
  "categoryCode": "001001001",
  "sellingPrice": 10000,
  "stockQuantity": 100,
  "productDetail": "<p>상품 상세 설명</p>",
  "images": [
    {
      "imageUrl": "https://example.com/image1.jpg",
      "imageType": "MAIN"
    }
  ],
  "options": [
    {
      "optionName": "색상",
      "optionValue": "빨강",
      "additionalPrice": 0,
      "stockQuantity": 50
    }
  ]
}

Response (성공):
{
  "code": "200",
  "message": "성공",
  "data": {
    "productNo": "ST11-PRD-00001",
    "status": "WAIT_APPROVAL",
    "createdAt": "2025-01-04T12:00:00"
  }
}

Response (실패):
{
  "code": "400",
  "message": "필수 값 누락",
  "errors": [
    {
      "field": "categoryCode",
      "message": "카테고리 코드가 유효하지 않습니다"
    }
  ]
}
```

### 상품 조회
```
GET /api/st11/products/{productNo}
Authorization: Bearer {accessToken}

Response:
{
  "code": "200",
  "data": {
    "productNo": "ST11-PRD-00001",
    "productName": "테스트 상품",
    "categoryCode": "001001001",
    "sellingPrice": 10000,
    "status": "ON_SALE",
    "stockQuantity": 100,
    "options": [...],
    "images": [...]
  }
}
```

### 주문 목록 조회
```
GET /api/st11/orders?startDate={}&endDate={}&status={}&page={}&size={}
Authorization: Bearer {accessToken}

Response:
{
  "code": "200",
  "data": {
    "orders": [
      {
        "orderNo": "ST11-ORD-00001",
        "orderDate": "2025-01-04T10:00:00",
        "buyerName": "홍길동",
        "buyerPhone": "010-****-5678",
        "totalAmount": 10000,
        "status": "PAY_COMPLETE",
        "items": [
          {
            "productNo": "ST11-PRD-00001",
            "productName": "테스트 상품",
            "optionName": "빨강",
            "quantity": 1,
            "price": 10000
          }
        ],
        "shippingAddress": {
          "recipientName": "홍길동",
          "phone": "010-1234-5678",
          "address": "서울시 강남구...",
          "zipCode": "06000"
        }
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalCount": 100,
      "totalPages": 5
    }
  }
}
```

### 발송 처리
```
POST /api/st11/orders/{orderNo}/ship
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "deliveryCompanyCode": "CJ",
  "trackingNumber": "1234567890"
}

Response:
{
  "code": "200",
  "message": "발송 처리 완료",
  "data": {
    "orderNo": "ST11-ORD-00001",
    "status": "SHIPPING"
  }
}
```

---

## 2. 네이버 스마트스토어 Mock API

### 인증 (OAuth 2.0 방식)
```
POST /api/naver/oauth/token
Content-Type: application/x-www-form-urlencoded

Request:
client_id=test-client-id
&client_secret=test-client-secret
&grant_type=client_credentials

Response:
{
  "access_token": "AAAAN...",
  "token_type": "Bearer",
  "expires_in": 43200
}
```

### 상품 등록
```
POST /api/naver/products
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "originProduct": {
    "statusType": "SALE",
    "saleType": "NEW",
    "leafCategoryId": "50000001",
    "name": "테스트 상품",
    "detailContent": "<p>상품 상세</p>",
    "images": {
      "representativeImage": {
        "url": "https://example.com/main.jpg"
      },
      "optionalImages": []
    },
    "salePrice": 10000,
    "stockQuantity": 100,
    "deliveryInfo": {
      "deliveryType": "DELIVERY",
      "deliveryAttributeType": "NORMAL",
      "deliveryFee": {
        "deliveryFeeType": "FREE"
      }
    },
    "productOption": {
      "optionCombinations": [
        {
          "optionName1": "색상",
          "optionName2": "사이즈",
          "stockQuantity": 50,
          "price": 0,
          "usable": true
        }
      ]
    }
  }
}

Response:
{
  "timestamp": "2025-01-04T12:00:00",
  "data": {
    "originProductNo": 1234567890,
    "smartstoreChannelProductNo": 9876543210
  }
}
```

### 상품 조회
```
GET /api/naver/products/{productNo}
Authorization: Bearer {access_token}

Response:
{
  "timestamp": "2025-01-04T12:00:00",
  "data": {
    "originProduct": {
      "statusType": "SALE",
      "name": "테스트 상품",
      "salePrice": 10000,
      ...
    }
  }
}
```

### 주문 목록 조회
```
POST /api/naver/orders/search
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "searchType": "ORDER_DATE",
  "searchStartDate": "2025-01-01",
  "searchEndDate": "2025-01-04",
  "orderStatusType": "PAY_COMPLETE",
  "pageIndex": 1,
  "pageSize": 20
}

Response:
{
  "timestamp": "2025-01-04T12:00:00",
  "data": {
    "count": 100,
    "moreSequence": "...",
    "contents": [
      {
        "orderId": "2025010412345",
        "orderDate": "2025-01-04T10:00:00",
        "orderStatusType": "PAY_COMPLETE",
        "totalPaymentAmount": 10000,
        "orderItems": [
          {
            "productOrderId": "20250104123451",
            "productId": 1234567890,
            "productName": "테스트 상품",
            "quantity": 1,
            "unitPrice": 10000,
            "shippingAddress": {
              "name": "홍길동",
              "tel1": "010-1234-5678",
              "baseAddress": "서울시 강남구...",
              "zipCode": "06000"
            }
          }
        ]
      }
    ]
  }
}
```

### 발송 처리
```
POST /api/naver/orders/{productOrderId}/ship
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "deliveryCompanyCode": "CJGLS",
  "trackingNumber": "1234567890"
}

Response:
{
  "timestamp": "2025-01-04T12:00:00",
  "data": {
    "success": true
  }
}
```

---

## 3. 카카오 스토어 Mock API

### 인증
```
POST /api/kakao/oauth/token
Content-Type: application/x-www-form-urlencoded

Request:
grant_type=client_credentials
&client_id=test-app-key
&client_secret=test-secret

Response:
{
  "access_token": "kakao-token-...",
  "token_type": "bearer",
  "expires_in": 7200
}
```

### 상품 등록
```
POST /api/kakao/products
Authorization: Bearer {access_token}
Content-Type: application/json

Request:
{
  "name": "테스트 상품",
  "categoryId": 100001,
  "price": 10000,
  "description": "상품 설명",
  "stock": 100,
  "status": "SALE",
  "images": [
    {
      "url": "https://example.com/image.jpg",
      "order": 1
    }
  ],
  "options": [
    {
      "name": "색상/빨강",
      "price": 0,
      "stock": 50
    }
  ],
  "delivery": {
    "type": "FREE",
    "fee": 0
  }
}

Response:
{
  "code": 0,
  "message": "success",
  "data": {
    "productId": "KAKAO-PRD-00001"
  }
}
```

### 주문 조회
```
GET /api/kakao/orders?fromDate={}&toDate={}&status={}&page={}&limit={}
Authorization: Bearer {access_token}

Response:
{
  "code": 0,
  "data": {
    "orders": [
      {
        "orderId": "KAKAO-ORD-00001",
        "orderDate": "2025-01-04T10:00:00",
        "status": "PAID",
        "buyer": {
          "name": "홍길동",
          "phone": "010-1234-5678"
        },
        "items": [...],
        "totalAmount": 10000,
        "delivery": {
          "name": "홍길동",
          "phone": "010-1234-5678",
          "address": "서울시..."
        }
      }
    ],
    "hasNext": true
  }
}
```

---

## 4. 토스 스토어 Mock API

### 인증
```
POST /api/toss/v1/auth/token
Content-Type: application/json

Request:
{
  "clientId": "test-client-id",
  "clientSecret": "test-client-secret"
}

Response:
{
  "accessToken": "toss-token-...",
  "expiresAt": "2025-01-04T14:00:00"
}
```

### 상품 등록
```
POST /api/toss/v1/products
Authorization: Bearer {accessToken}
Content-Type: application/json

Request:
{
  "name": "테스트 상품",
  "categoryCode": "C001",
  "originalPrice": 12000,
  "sellingPrice": 10000,
  "stockQuantity": 100,
  "description": "상품 설명",
  "mainImageUrl": "https://example.com/main.jpg",
  "detailImageUrls": [],
  "options": [
    {
      "optionGroupName": "색상",
      "optionName": "빨강",
      "additionalPrice": 0,
      "stockQuantity": 50
    }
  ]
}

Response:
{
  "result": "SUCCESS",
  "data": {
    "productId": "TOSS-PRD-00001",
    "status": "PENDING_APPROVAL"
  }
}
```

### 주문 목록
```
GET /api/toss/v1/orders?startDateTime={}&endDateTime={}&status={}&page={}
Authorization: Bearer {accessToken}

Response:
{
  "result": "SUCCESS",
  "data": {
    "orders": [
      {
        "orderId": "TOSS-ORD-00001",
        "orderDateTime": "2025-01-04T10:00:00",
        "status": "PAYMENT_COMPLETED",
        "totalAmount": 10000,
        "buyer": {
          "name": "홍길동",
          "phoneNumber": "01012345678"
        },
        "orderItems": [...],
        "shippingInfo": {...}
      }
    ],
    "page": {
      "number": 1,
      "size": 20,
      "totalElements": 100
    }
  }
}
```

---

## 5. 쿠팡 Mock API

### 인증 (HMAC-SHA256)
```
// 쿠팡은 HMAC 서명 방식 사용
// Mock에서는 단순화된 API Key 방식 사용

Headers:
Authorization: HMAC-SHA256 <access-key>:<signature>
X-Coupang-Date: 2025-01-04T12:00:00Z
```

### 상품 등록
```
POST /api/coupang/v2/products
Authorization: HMAC-SHA256 ...
Content-Type: application/json

Request:
{
  "sellerProductName": "테스트 상품",
  "vendorId": "A00000001",
  "displayCategoryCode": 1001001001,
  "categoryId": 1001001001,
  "brand": "테스트브랜드",
  "returnCenterCode": "RC00001",
  "deliveryInfo": {
    "deliveryType": "NORMAL",
    "deliveryCharge": 0,
    "freeShipOverAmount": 0
  },
  "items": [
    {
      "itemName": "테스트 상품 - 빨강",
      "originalPrice": 12000,
      "salePrice": 10000,
      "maximumBuyCount": 10,
      "maximumBuyForPerson": 5,
      "outboundShippingTimeDay": 1,
      "unitCount": 1,
      "vendorItemId": "ITEM-001",
      "images": [
        {
          "imageOrder": 1,
          "imageType": "MAIN",
          "cdnPath": "https://cdn.example.com/image.jpg"
        }
      ],
      "contents": [
        {
          "contentsType": "HTML",
          "contentDetails": "<p>상품 상세</p>"
        }
      ],
      "attributes": [
        {
          "attributeTypeName": "색상",
          "attributeValueName": "빨강"
        }
      ]
    }
  ]
}

Response:
{
  "code": "SUCCESS",
  "message": "",
  "data": {
    "sellerProductId": 1234567890
  }
}
```

### 주문 목록
```
GET /api/coupang/v2/orders?vendorId={}&createdAtFrom={}&createdAtTo={}&status={}&nextToken={}
Authorization: HMAC-SHA256 ...

Response:
{
  "code": "SUCCESS",
  "data": {
    "orderId": 1234567890,
    "orderItems": [
      {
        "shipmentBoxId": 9876543210,
        "orderId": 1234567890,
        "vendorItemId": "ITEM-001",
        "vendorItemName": "테스트 상품 - 빨강",
        "quantity": 1,
        "shippingPrice": 0,
        "orderPrice": 10000,
        "receiverName": "홍길동",
        "receiverPhone": "010-1234-5678",
        "postCode": "06000",
        "addr1": "서울시 강남구",
        "addr2": "역삼동 123",
        "statusName": "ACCEPT",
        "orderedAt": "2025-01-04T10:00:00"
      }
    ],
    "nextToken": "..."
  }
}
```

### 발송 처리
```
PUT /api/coupang/v2/orders/{shipmentBoxId}/invoice
Authorization: HMAC-SHA256 ...
Content-Type: application/json

Request:
{
  "vendorId": "A00000001",
  "shipmentBoxId": 9876543210,
  "deliveryCompanyCode": "CJGLS",
  "invoiceNumber": "1234567890"
}

Response:
{
  "code": "SUCCESS",
  "message": "송장 등록 완료"
}
```

---

## Mock Server 구현

### 폴더 구조
```
mock-servers/channel-mock/
├── src/
│   ├── app.ts                    # Express 앱
│   ├── routes/
│   │   ├── index.ts
│   │   ├── st11/                 # 11번가
│   │   │   ├── auth.ts
│   │   │   ├── products.ts
│   │   │   └── orders.ts
│   │   ├── naver/                # 네이버
│   │   │   ├── auth.ts
│   │   │   ├── products.ts
│   │   │   └── orders.ts
│   │   ├── kakao/                # 카카오
│   │   │   └── ...
│   │   ├── toss/                 # 토스
│   │   │   └── ...
│   │   └── coupang/              # 쿠팡
│   │       └── ...
│   ├── services/
│   │   ├── product.service.ts
│   │   ├── order.service.ts
│   │   └── auth.service.ts
│   ├── db/
│   │   ├── index.ts              # SQLite 연결
│   │   └── schema.sql
│   ├── scenarios/                # 테스트 시나리오
│   │   ├── success.ts
│   │   ├── error.ts
│   │   └── delay.ts
│   ├── middleware/
│   │   ├── auth.ts
│   │   └── scenario.ts
│   └── types/
│       └── index.ts
├── Dockerfile
├── package.json
└── tsconfig.json
```

### 테스트 시나리오 설정
```typescript
// scenarios/index.ts

// 헤더로 시나리오 제어
// X-Mock-Scenario: error-auth
// X-Mock-Scenario: delay-5000
// X-Mock-Scenario: error-stock

export const scenarios = {
  'success': {
    status: 200,
    delay: 0
  },
  'error-auth': {
    status: 401,
    body: { code: '401', message: '인증 실패' }
  },
  'error-stock': {
    status: 400,
    body: { code: 'INSUFFICIENT_STOCK', message: '재고 부족' }
  },
  'delay-5000': {
    status: 200,
    delay: 5000
  }
};
```

### API 응답 예시 (Express 라우터)
```typescript
// routes/st11/products.ts
import { Router } from 'express';
import { productService } from '../../services/product.service';

const router = Router();

router.post('/', async (req, res) => {
  const { body, headers } = req;
  const scenario = headers['x-mock-scenario'];
  
  // 시나리오 처리
  if (scenario) {
    const scenarioConfig = scenarios[scenario];
    if (scenarioConfig.delay) {
      await delay(scenarioConfig.delay);
    }
    if (scenarioConfig.status !== 200) {
      return res.status(scenarioConfig.status).json(scenarioConfig.body);
    }
  }
  
  // 정상 처리
  const product = await productService.create({
    channel: 'ST11',
    ...body
  });
  
  res.json({
    code: '200',
    message: '성공',
    data: {
      productNo: `ST11-PRD-${product.id}`,
      status: 'WAIT_APPROVAL',
      createdAt: new Date().toISOString()
    }
  });
});

export default router;
```

---

## 백엔드 연동 어댑터 패턴

### 인터페이스 정의
```kotlin
// 포트 인터페이스
interface ChannelAdapter {
    val channel: SalesChannel
    
    suspend fun authenticate(): ChannelAuthResult
    suspend fun registerProduct(product: Product): ChannelProductResult
    suspend fun updateProduct(channelProductId: String, product: Product): ChannelProductResult
    suspend fun deleteProduct(channelProductId: String): Boolean
    suspend fun getOrders(request: OrderSearchRequest): List<ChannelOrder>
    suspend fun shipOrder(channelOrderId: String, shipment: ShipmentInfo): Boolean
}

// 결과 클래스
data class ChannelProductResult(
    val success: Boolean,
    val channelProductId: String?,
    val status: String?,
    val errorCode: String?,
    val errorMessage: String?
)
```

### 구현 예시
```kotlin
@Component
class St11Adapter(
    private val st11Client: St11Client,
    private val st11Properties: St11Properties
) : ChannelAdapter {
    
    override val channel = SalesChannel.ST11
    
    override suspend fun registerProduct(product: Product): ChannelProductResult {
        val request = St11ProductRequest(
            productName = product.name,
            categoryCode = mapCategory(product.categoryId),
            sellingPrice = product.price.toInt(),
            stockQuantity = product.totalStock,
            productDetail = product.description,
            images = product.images.map { ... },
            options = product.options.map { ... }
        )
        
        return try {
            val response = st11Client.createProduct(request)
            ChannelProductResult(
                success = true,
                channelProductId = response.data.productNo,
                status = response.data.status
            )
        } catch (e: Exception) {
            ChannelProductResult(
                success = false,
                errorCode = "ST11_ERROR",
                errorMessage = e.message
            )
        }
    }
}
```

---

## 배송사 코드 매핑

| 공통코드 | 11번가 | 네이버 | 카카오 | 토스 | 쿠팡 |
|---------|--------|--------|--------|------|------|
| CJ | CJ | CJGLS | CJGLS | CJGLS | CJGLS |
| HANJIN | HANJIN | HANJIN | HANJIN | HANJIN | HANJIN |
| LOTTE | LOTTE | LOTTE | LOTTE | LOTTE | LOTTE |
| POST | POST | EPOST | EPOST | EPOST | EPOST |
| LOGEN | LOGEN | LOGEN | LOGEN | LOGEN | LOGEN |
