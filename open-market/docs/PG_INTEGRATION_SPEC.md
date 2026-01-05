# 💳 PG사 연동 스펙 (PG_INTEGRATION_SPEC.md)

> **담당**: LLM Agent (Mock Server 구현)
> **목적**: 실제 PG사 API를 모방한 Mock Server 구현

---

## 개요

### 연동 대상 PG사
| PG사 | 코드 | Mock Port | 실제 API 기반 |
|------|------|-----------|--------------|
| 토스페이먼츠 | TOSS_PAYMENTS | 8081 | TossPayments API v1 |
| 카카오페이 | KAKAO_PAY | 8081 | 카카오페이 단건결제 API |
| 네이버페이 | NAVER_PAY | 8081 | 네이버페이 결제형 API |
| 다날 | DANAL | 8081 | 다날 결제 API |

### 공통 결제 플로우
```
┌─────────────────────────────────────────────────────────────────────┐
│                       Payment Flow                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   [Frontend]         [Backend]          [PG Mock]                   │
│       │                  │                  │                        │
│       │  1. 결제 요청     │                  │                        │
│       │ ───────────────> │                  │                        │
│       │                  │  2. 결제 준비     │                        │
│       │                  │ ───────────────> │                        │
│       │                  │  paymentKey      │                        │
│       │                  │ <─────────────── │                        │
│       │  3. 결제창 URL    │                  │                        │
│       │ <─────────────── │                  │                        │
│       │                  │                  │                        │
│       │  4. 결제창 호출   │                  │                        │
│       │ ────────────────────────────────── >│                        │
│       │                  │                  │                        │
│       │  5. 결제 완료 콜백 (redirect)        │                        │
│       │ <─────────────────────────────────  │                        │
│       │                  │                  │                        │
│       │  6. 결제 승인 요청 │                  │                        │
│       │ ───────────────> │  7. 결제 승인    │                        │
│       │                  │ ───────────────> │                        │
│       │                  │  승인 결과       │                        │
│       │                  │ <─────────────── │                        │
│       │  8. 결제 완료     │                  │                        │
│       │ <─────────────── │                  │                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 1. 토스페이먼츠 Mock API

### 인증
```
모든 API 요청에 Basic Auth 사용
Authorization: Basic {base64(secretKey + ":")}
```

### 결제 준비
```
POST /api/toss/v1/payments
Authorization: Basic dGVzdF9za18uLi4=
Content-Type: application/json

Request:
{
  "amount": 10000,
  "orderId": "ORDER-12345",
  "orderName": "테스트 상품 외 1건",
  "successUrl": "https://yoursite.com/success",
  "failUrl": "https://yoursite.com/fail",
  "customerEmail": "customer@example.com",
  "customerName": "홍길동",
  "customerMobilePhone": "01012345678"
}

Response:
{
  "paymentKey": "toss_payment_key_12345",
  "orderId": "ORDER-12345",
  "status": "READY",
  "requestedAt": "2025-01-04T12:00:00+09:00",
  "checkout": {
    "url": "http://localhost:8081/mock/toss/checkout?paymentKey=toss_payment_key_12345"
  }
}
```

### Mock 결제창
```
GET /mock/toss/checkout?paymentKey={paymentKey}

HTML 페이지 반환:
- 결제 정보 표시
- "결제하기" 버튼 클릭 시 successUrl로 리다이렉트
- "취소" 버튼 클릭 시 failUrl로 리다이렉트

성공 리다이렉트:
{successUrl}?paymentKey={paymentKey}&orderId={orderId}&amount={amount}

실패 리다이렉트:
{failUrl}?code=USER_CANCEL&message=사용자 취소
```

### 결제 승인
```
POST /api/toss/v1/payments/confirm
Authorization: Basic dGVzdF9za18uLi4=
Content-Type: application/json

Request:
{
  "paymentKey": "toss_payment_key_12345",
  "orderId": "ORDER-12345",
  "amount": 10000
}

Response (성공):
{
  "paymentKey": "toss_payment_key_12345",
  "orderId": "ORDER-12345",
  "status": "DONE",
  "totalAmount": 10000,
  "balanceAmount": 10000,
  "method": "카드",
  "approvedAt": "2025-01-04T12:01:00+09:00",
  "card": {
    "company": "신한",
    "number": "4321-****-****-1234",
    "installmentPlanMonths": 0,
    "isInterestFree": false,
    "approveNo": "12345678"
  },
  "receipt": {
    "url": "http://localhost:8081/mock/toss/receipt/toss_payment_key_12345"
  }
}

Response (실패 - 금액 불일치):
{
  "code": "INVALID_REQUEST",
  "message": "결제 금액이 일치하지 않습니다."
}
```

### 결제 조회
```
GET /api/toss/v1/payments/{paymentKey}
Authorization: Basic dGVzdF9za18uLi4=

Response:
{
  "paymentKey": "toss_payment_key_12345",
  "orderId": "ORDER-12345",
  "status": "DONE",
  "totalAmount": 10000,
  "method": "카드",
  ...
}
```

### 결제 취소
```
POST /api/toss/v1/payments/{paymentKey}/cancel
Authorization: Basic dGVzdF9za18uLi4=
Content-Type: application/json

Request:
{
  "cancelReason": "고객 요청"
}

Response:
{
  "paymentKey": "toss_payment_key_12345",
  "orderId": "ORDER-12345",
  "status": "CANCELED",
  "cancels": [
    {
      "transactionKey": "cancel_txn_12345",
      "cancelReason": "고객 요청",
      "canceledAt": "2025-01-04T13:00:00+09:00",
      "cancelAmount": 10000
    }
  ]
}
```

### 부분 취소
```
POST /api/toss/v1/payments/{paymentKey}/cancel
Authorization: Basic dGVzdF9za18uLi4=
Content-Type: application/json

Request:
{
  "cancelReason": "부분 환불",
  "cancelAmount": 5000
}

Response:
{
  "paymentKey": "toss_payment_key_12345",
  "status": "PARTIAL_CANCELED",
  "totalAmount": 10000,
  "balanceAmount": 5000,
  "cancels": [...]
}
```

---

## 2. 카카오페이 Mock API

### 인증
```
Authorization: KakaoAK {admin_key}
```

### 결제 준비
```
POST /api/kakao/v1/payment/ready
Authorization: KakaoAK test_admin_key
Content-Type: application/x-www-form-urlencoded

Request:
cid=TC0ONETIME
&partner_order_id=ORDER-12345
&partner_user_id=USER-001
&item_name=테스트 상품 외 1건
&quantity=2
&total_amount=10000
&tax_free_amount=0
&approval_url=https://yoursite.com/kakao/success
&cancel_url=https://yoursite.com/kakao/cancel
&fail_url=https://yoursite.com/kakao/fail

Response:
{
  "tid": "T1234567890123456789",
  "next_redirect_app_url": "kakaolink://...",
  "next_redirect_mobile_url": "https://mockpay.kakao.com/...",
  "next_redirect_pc_url": "http://localhost:8081/mock/kakao/checkout?tid=T1234567890123456789",
  "android_app_scheme": "kakaotalk://...",
  "ios_app_scheme": "kakaotalk://...",
  "created_at": "2025-01-04T12:00:00"
}
```

### Mock 결제창
```
GET /mock/kakao/checkout?tid={tid}

결제 승인 시:
{approval_url}?pg_token={pg_token}
```

### 결제 승인
```
POST /api/kakao/v1/payment/approve
Authorization: KakaoAK test_admin_key
Content-Type: application/x-www-form-urlencoded

Request:
cid=TC0ONETIME
&tid=T1234567890123456789
&partner_order_id=ORDER-12345
&partner_user_id=USER-001
&pg_token=mock_pg_token_12345

Response:
{
  "aid": "A1234567890123456789",
  "tid": "T1234567890123456789",
  "cid": "TC0ONETIME",
  "partner_order_id": "ORDER-12345",
  "partner_user_id": "USER-001",
  "payment_method_type": "CARD",
  "item_name": "테스트 상품 외 1건",
  "quantity": 2,
  "amount": {
    "total": 10000,
    "tax_free": 0,
    "vat": 909,
    "point": 0,
    "discount": 0
  },
  "card_info": {
    "purchase_corp": "신한카드",
    "purchase_corp_code": "SHINHAN",
    "issuer_corp": "신한카드",
    "issuer_corp_code": "SHINHAN",
    "bin": "432112",
    "card_type": "신용",
    "install_month": "00",
    "approved_id": "12345678",
    "card_mid": "****1234"
  },
  "created_at": "2025-01-04T12:00:00",
  "approved_at": "2025-01-04T12:01:00"
}
```

### 결제 취소
```
POST /api/kakao/v1/payment/cancel
Authorization: KakaoAK test_admin_key
Content-Type: application/x-www-form-urlencoded

Request:
cid=TC0ONETIME
&tid=T1234567890123456789
&cancel_amount=10000
&cancel_tax_free_amount=0

Response:
{
  "aid": "A9876543210987654321",
  "tid": "T1234567890123456789",
  "status": "CANCEL_PAYMENT",
  "approved_cancel_amount": {
    "total": 10000,
    "tax_free": 0,
    "vat": 909
  },
  "canceled_at": "2025-01-04T13:00:00"
}
```

---

## 3. 네이버페이 Mock API

### 인증
```
X-Naver-Client-Id: {client_id}
X-Naver-Client-Secret: {client_secret}
```

### 결제 준비
```
POST /api/naver/v1/payments/reserve
Content-Type: application/json
X-Naver-Client-Id: test_client_id
X-Naver-Client-Secret: test_client_secret

Request:
{
  "merchantPayKey": "ORDER-12345",
  "productName": "테스트 상품 외 1건",
  "productCount": 2,
  "totalPayAmount": 10000,
  "taxScopeAmount": 10000,
  "taxExScopeAmount": 0,
  "returnUrl": "https://yoursite.com/naver/complete",
  "merchantUserKey": "USER-001"
}

Response:
{
  "code": "Success",
  "message": "성공",
  "body": {
    "reserveId": "NAVER-RES-12345",
    "paymentUrl": "http://localhost:8081/mock/naver/checkout?reserveId=NAVER-RES-12345"
  }
}
```

### Mock 결제창
```
GET /mock/naver/checkout?reserveId={reserveId}

결제 완료 시:
{returnUrl}?resultCode=Success&paymentId={paymentId}&reserveId={reserveId}
```

### 결제 승인
```
POST /api/naver/v1/payments/{paymentId}/apply
Content-Type: application/json
X-Naver-Client-Id: test_client_id
X-Naver-Client-Secret: test_client_secret

Response:
{
  "code": "Success",
  "message": "성공",
  "body": {
    "paymentId": "NAVER-PAY-12345",
    "merchantPayKey": "ORDER-12345",
    "merchantUserKey": "USER-001",
    "paymentResult": {
      "paymentMethod": "CARD",
      "totalPayAmount": 10000,
      "cardCorpName": "신한카드",
      "cardNo": "4321-****-****-1234",
      "admissionYmdt": "20250104120100"
    },
    "detail": {
      "productName": "테스트 상품 외 1건",
      "productCount": 2
    }
  }
}
```

### 결제 취소
```
POST /api/naver/v1/payments/{paymentId}/cancel
Content-Type: application/json
X-Naver-Client-Id: test_client_id
X-Naver-Client-Secret: test_client_secret

Request:
{
  "cancelReason": "고객 요청",
  "cancelAmount": 10000,
  "taxScopeAmount": 10000,
  "taxExScopeAmount": 0
}

Response:
{
  "code": "Success",
  "message": "성공",
  "body": {
    "paymentId": "NAVER-PAY-12345",
    "cancelId": "NAVER-CANCEL-12345",
    "cancelAmount": 10000,
    "cancelledYmdt": "20250104130000"
  }
}
```

---

## 4. 다날 Mock API

### 인증
```
Headers:
CPID: {cp_id}
CPPassword: {cp_password}
```

### 결제 요청
```
POST /api/danal/v1/payment/ready
Content-Type: application/json
CPID: test_cp_id
CPPassword: test_cp_password

Request:
{
  "amount": "10000",
  "orderNo": "ORDER-12345",
  "itemName": "테스트 상품 외 1건",
  "userName": "홍길동",
  "userPhone": "01012345678",
  "returnUrl": "https://yoursite.com/danal/return",
  "cancelUrl": "https://yoursite.com/danal/cancel"
}

Response:
{
  "result": "0000",
  "message": "성공",
  "data": {
    "tid": "DANAL-TID-12345",
    "paymentUrl": "http://localhost:8081/mock/danal/checkout?tid=DANAL-TID-12345"
  }
}
```

### Mock 결제창
```
GET /mock/danal/checkout?tid={tid}

결제 완료 시 returnUrl로 POST:
tid={tid}&orderNo={orderNo}&amount={amount}
```

### 결제 승인
```
POST /api/danal/v1/payment/confirm
Content-Type: application/json
CPID: test_cp_id
CPPassword: test_cp_password

Request:
{
  "tid": "DANAL-TID-12345",
  "orderNo": "ORDER-12345",
  "amount": "10000"
}

Response:
{
  "result": "0000",
  "message": "성공",
  "data": {
    "tid": "DANAL-TID-12345",
    "orderNo": "ORDER-12345",
    "amount": "10000",
    "payMethod": "CARD",
    "cardName": "신한카드",
    "cardNo": "4321********1234",
    "installMonth": "00",
    "authNo": "12345678",
    "transDate": "20250104120100"
  }
}
```

### 결제 취소
```
POST /api/danal/v1/payment/cancel
Content-Type: application/json
CPID: test_cp_id
CPPassword: test_cp_password

Request:
{
  "tid": "DANAL-TID-12345",
  "cancelReason": "고객 요청",
  "cancelAmount": "10000"
}

Response:
{
  "result": "0000",
  "message": "취소 완료",
  "data": {
    "tid": "DANAL-TID-12345",
    "cancelTid": "DANAL-CANCEL-12345",
    "cancelAmount": "10000",
    "cancelDate": "20250104130000"
  }
}
```

---

## Mock Server 구현

### 폴더 구조
```
mock-servers/pg-mock/
├── src/
│   ├── app.ts                    # Express 앱
│   ├── routes/
│   │   ├── index.ts
│   │   ├── toss/
│   │   │   ├── payments.ts
│   │   │   └── checkout.ts       # Mock 결제창
│   │   ├── kakao/
│   │   │   ├── payment.ts
│   │   │   └── checkout.ts
│   │   ├── naver/
│   │   │   ├── payments.ts
│   │   │   └── checkout.ts
│   │   └── danal/
│   │       ├── payment.ts
│   │       └── checkout.ts
│   ├── services/
│   │   └── payment.service.ts
│   ├── db/
│   │   ├── index.ts              # SQLite
│   │   └── schema.sql
│   ├── views/                    # Mock 결제창 템플릿
│   │   ├── toss-checkout.ejs
│   │   ├── kakao-checkout.ejs
│   │   ├── naver-checkout.ejs
│   │   └── danal-checkout.ejs
│   ├── scenarios/
│   │   ├── success.ts
│   │   ├── error.ts
│   │   └── timeout.ts
│   └── types/
│       └── index.ts
├── Dockerfile
├── package.json
└── tsconfig.json
```

### 테스트 시나리오
```typescript
// 헤더로 시나리오 제어
// X-Mock-Scenario: error-card-declined
// X-Mock-Scenario: error-insufficient-balance
// X-Mock-Scenario: delay-10000

export const pgScenarios = {
  'success': {
    status: 200,
    delay: 0
  },
  'error-card-declined': {
    status: 400,
    body: {
      code: 'CARD_DECLINED',
      message: '카드가 거절되었습니다.'
    }
  },
  'error-insufficient-balance': {
    status: 400,
    body: {
      code: 'INSUFFICIENT_BALANCE',
      message: '잔액이 부족합니다.'
    }
  },
  'error-invalid-card': {
    status: 400,
    body: {
      code: 'INVALID_CARD',
      message: '유효하지 않은 카드입니다.'
    }
  },
  'error-expired-card': {
    status: 400,
    body: {
      code: 'EXPIRED_CARD',
      message: '만료된 카드입니다.'
    }
  },
  'timeout': {
    status: 504,
    delay: 30000,
    body: {
      code: 'TIMEOUT',
      message: '요청 시간이 초과되었습니다.'
    }
  }
};
```

### Mock 결제창 HTML (예: 토스)
```html
<!-- views/toss-checkout.ejs -->
<!DOCTYPE html>
<html>
<head>
  <title>토스페이먼츠 결제 (Mock)</title>
  <style>
    body { font-family: sans-serif; max-width: 400px; margin: 50px auto; }
    .payment-info { background: #f5f5f5; padding: 20px; border-radius: 8px; }
    .amount { font-size: 24px; font-weight: bold; color: #0064ff; }
    .buttons { margin-top: 20px; display: flex; gap: 10px; }
    button { flex: 1; padding: 15px; border: none; border-radius: 8px; cursor: pointer; }
    .pay-btn { background: #0064ff; color: white; }
    .cancel-btn { background: #e0e0e0; }
  </style>
</head>
<body>
  <h2>🧪 토스페이먼츠 Mock 결제창</h2>
  <div class="payment-info">
    <p>주문번호: <%= orderId %></p>
    <p>상품명: <%= orderName %></p>
    <p class="amount">결제금액: <%= amount.toLocaleString() %>원</p>
  </div>
  <div class="buttons">
    <button class="cancel-btn" onclick="cancel()">취소</button>
    <button class="pay-btn" onclick="pay()">결제하기</button>
  </div>
  
  <script>
    function pay() {
      window.location.href = '<%= successUrl %>?paymentKey=<%= paymentKey %>&orderId=<%= orderId %>&amount=<%= amount %>';
    }
    function cancel() {
      window.location.href = '<%= failUrl %>?code=USER_CANCEL&message=사용자가 결제를 취소했습니다';
    }
  </script>
</body>
</html>
```

---

## 백엔드 PG 어댑터

### 인터페이스
```kotlin
interface PgAdapter {
    val provider: PgProvider
    
    suspend fun prepare(request: PaymentPrepareRequest): PaymentPrepareResult
    suspend fun confirm(request: PaymentConfirmRequest): PaymentConfirmResult
    suspend fun cancel(paymentKey: String, reason: String, amount: Long?): PaymentCancelResult
    suspend fun getPayment(paymentKey: String): PaymentInfo
}

data class PaymentPrepareRequest(
    val orderId: String,
    val orderName: String,
    val amount: Long,
    val customerEmail: String?,
    val customerName: String?,
    val customerPhone: String?,
    val successUrl: String,
    val failUrl: String
)

data class PaymentPrepareResult(
    val success: Boolean,
    val paymentKey: String?,
    val checkoutUrl: String?,
    val errorCode: String?,
    val errorMessage: String?
)
```

### 구현 예시 (토스페이먼츠)
```kotlin
@Component
class TossPaymentsAdapter(
    private val tossClient: TossPaymentsClient,
    private val tossProperties: TossPaymentsProperties
) : PgAdapter {
    
    override val provider = PgProvider.TOSS_PAYMENTS
    
    override suspend fun prepare(request: PaymentPrepareRequest): PaymentPrepareResult {
        val response = tossClient.createPayment(
            TossPaymentRequest(
                amount = request.amount,
                orderId = request.orderId,
                orderName = request.orderName,
                successUrl = request.successUrl,
                failUrl = request.failUrl,
                customerEmail = request.customerEmail,
                customerName = request.customerName,
                customerMobilePhone = request.customerPhone
            )
        )
        
        return PaymentPrepareResult(
            success = true,
            paymentKey = response.paymentKey,
            checkoutUrl = response.checkout.url
        )
    }
    
    override suspend fun confirm(request: PaymentConfirmRequest): PaymentConfirmResult {
        val response = tossClient.confirmPayment(
            TossConfirmRequest(
                paymentKey = request.paymentKey,
                orderId = request.orderId,
                amount = request.amount
            )
        )
        
        return PaymentConfirmResult(
            success = true,
            paymentKey = response.paymentKey,
            status = response.status,
            approvedAt = response.approvedAt,
            cardInfo = response.card?.let {
                CardInfo(
                    company = it.company,
                    number = it.number,
                    installmentMonths = it.installmentPlanMonths
                )
            }
        )
    }
}
```

---

## Webhook (결제 결과 통지)

### 토스페이먼츠 Webhook
```
POST /api/v1/payments/webhook/toss
Content-Type: application/json

Request:
{
  "eventType": "PAYMENT.DONE",
  "createdAt": "2025-01-04T12:01:00+09:00",
  "data": {
    "paymentKey": "toss_payment_key_12345",
    "orderId": "ORDER-12345",
    "status": "DONE",
    "totalAmount": 10000
  }
}

Response:
HTTP 200 OK
```

### Mock Server Webhook 발송
```typescript
// 결제 완료 시 webhook 발송 (비동기)
async function sendWebhook(payment: Payment) {
  const webhookUrl = process.env.BACKEND_WEBHOOK_URL;
  
  await axios.post(webhookUrl, {
    eventType: 'PAYMENT.DONE',
    createdAt: new Date().toISOString(),
    data: {
      paymentKey: payment.paymentKey,
      orderId: payment.orderId,
      status: payment.status,
      totalAmount: payment.amount
    }
  });
}
```

---

## 에러 코드 매핑

| 상황 | 토스 | 카카오 | 네이버 | 다날 | 공통코드 |
|------|------|--------|--------|------|----------|
| 잔액부족 | INSUFFICIENT_BALANCE | -783 | InsufficientBalance | 8001 | INSUFFICIENT_BALANCE |
| 카드거절 | CARD_DECLINED | -784 | CardDeclined | 8002 | CARD_DECLINED |
| 카드만료 | CARD_EXPIRED | -785 | CardExpired | 8003 | CARD_EXPIRED |
| 사용자취소 | USER_CANCEL | -781 | UserCancel | 9001 | USER_CANCEL |
| 결제시간초과 | PAYMENT_TIMEOUT | -782 | PaymentTimeout | 9002 | PAYMENT_TIMEOUT |
