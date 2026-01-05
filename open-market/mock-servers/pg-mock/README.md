# PG Mock Server

PG (Payment Gateway) Mock Server for Open Market project.

## Features

- 🏦 **4 PG Providers**: Toss Payments, Kakao Pay, Naver Pay, Danal
- 💳 **Full Payment Flow**: Prepare → Checkout → Confirm → Cancel
- 🎭 **Mock Checkout Pages**: Interactive HTML checkout pages
- 🎯 **Error Scenarios**: Simulate card declined, insufficient balance, timeout, etc.
- 💾 **SQLite Storage**: Persistent payment data
- 🔌 **REST API**: Full compliance with actual PG APIs

## Quick Start

### Development

```bash
# Install dependencies
npm install

# Initialize database
npm run db:init

# Start dev server
npm run dev
```

### Production

```bash
# Build
npm run build

# Start server
npm start
```

### Docker

```bash
# Build image
docker build -t pg-mock .

# Run container
docker run -p 8081:8081 pg-mock
```

## API Documentation

### Toss Payments

```bash
# 1. Prepare payment
curl -X POST http://localhost:8081/api/toss/v1/payments \
  -H "Authorization: Basic dGVzdF9za18uLi4=" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10000,
    "orderId": "ORDER-123",
    "orderName": "Test Product",
    "successUrl": "http://localhost:3000/success",
    "failUrl": "http://localhost:3000/fail"
  }'

# 2. Open checkout page (returned in checkout.url)
# http://localhost:8081/mock/toss/checkout?paymentKey=...

# 3. Confirm payment
curl -X POST http://localhost:8081/api/toss/v1/payments/confirm \
  -H "Authorization: Basic dGVzdF9za18uLi4=" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentKey": "toss_payment_key_...",
    "orderId": "ORDER-123",
    "amount": 10000
  }'

# 4. Cancel payment
curl -X POST http://localhost:8081/api/toss/v1/payments/{paymentKey}/cancel \
  -H "Authorization: Basic dGVzdF9za18uLi4=" \
  -H "Content-Type: application/json" \
  -d '{
    "cancelReason": "Customer request"
  }'
```

### Kakao Pay

```bash
# 1. Ready
curl -X POST http://localhost:8081/api/kakao/v1/payment/ready \
  -H "Authorization: KakaoAK test_admin_key" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "cid=TC0ONETIME&partner_order_id=ORDER-123&partner_user_id=USER-001&item_name=Test&quantity=1&total_amount=10000&approval_url=http://localhost:3000/success&cancel_url=http://localhost:3000/cancel&fail_url=http://localhost:3000/fail"

# 2. Approve
curl -X POST http://localhost:8081/api/kakao/v1/payment/approve \
  -H "Authorization: KakaoAK test_admin_key" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "cid=TC0ONETIME&tid=T123&partner_order_id=ORDER-123&partner_user_id=USER-001&pg_token=mock_pg_token"
```

### Naver Pay

```bash
# 1. Reserve
curl -X POST http://localhost:8081/api/naver/v1/payments/reserve \
  -H "X-Naver-Client-Id: test_client_id" \
  -H "X-Naver-Client-Secret: test_client_secret" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantPayKey": "ORDER-123",
    "productName": "Test Product",
    "productCount": 1,
    "totalPayAmount": 10000,
    "taxScopeAmount": 10000,
    "taxExScopeAmount": 0,
    "returnUrl": "http://localhost:3000/complete",
    "merchantUserKey": "USER-001"
  }'

# 2. Apply
curl -X POST http://localhost:8081/api/naver/v1/payments/{paymentId}/apply \
  -H "X-Naver-Client-Id: test_client_id" \
  -H "X-Naver-Client-Secret: test_client_secret"
```

### Danal

```bash
# 1. Ready
curl -X POST http://localhost:8081/api/danal/v1/payment/ready \
  -H "CPID: test_cp_id" \
  -H "CPPassword: test_cp_password" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "10000",
    "orderNo": "ORDER-123",
    "itemName": "Test Product",
    "userName": "홍길동",
    "userPhone": "01012345678",
    "returnUrl": "http://localhost:3000/return",
    "cancelUrl": "http://localhost:3000/cancel"
  }'

# 2. Confirm
curl -X POST http://localhost:8081/api/danal/v1/payment/confirm \
  -H "CPID: test_cp_id" \
  -H "CPPassword: test_cp_password" \
  -H "Content-Type: application/json" \
  -d '{
    "tid": "DANAL-TID-123",
    "orderNo": "ORDER-123",
    "amount": "10000"
  }'
```

## Error Scenarios

Use `X-Mock-Scenario` header to simulate errors:

```bash
# Simulate card declined
curl -X POST http://localhost:8081/api/toss/v1/payments/confirm \
  -H "X-Mock-Scenario: error-card-declined" \
  -H "Authorization: Basic dGVzdF9za18uLi4=" \
  -H "Content-Type: application/json" \
  -d '...'
```

Available scenarios:
- `success` (default)
- `error-card-declined`
- `error-insufficient-balance`
- `error-invalid-card`
- `error-expired-card`
- `timeout`

## Project Structure

```
src/
├── app.ts                      # Express app
├── routes/
│   ├── index.ts
│   ├── toss/
│   │   ├── payments.ts
│   │   └── checkout.ts
│   ├── kakao/
│   │   ├── payment.ts
│   │   └── checkout.ts
│   ├── naver/
│   │   ├── payments.ts
│   │   └── checkout.ts
│   └── danal/
│       ├── payment.ts
│       └── checkout.ts
├── services/
│   └── payment.service.ts
├── db/
│   ├── index.ts
│   ├── init.ts
│   └── schema.sql
├── views/                      # EJS templates
│   ├── toss-checkout.ejs
│   ├── kakao-checkout.ejs
│   ├── naver-checkout.ejs
│   ├── danal-checkout.ejs
│   └── receipt.ejs
├── scenarios/
│   └── index.ts
└── types/
    └── index.ts
```

## Environment Variables

- `PORT`: Server port (default: 8081)

## License

MIT
