# Mock 외부 API 서버 (closet-external-api)

> 작성일: 2026-04-04
> 모듈: closet-external-api (port 9090)
> DB: closet_external (MySQL)

## 개요

외부 연동이 필요한 PG사 4개 + 택배사 4개를 자체 Mock 서버로 구축하여,
로컬 환경에서 전체 결제-배송 플로우를 테스트하고 정산 데이터를 축적한다.

## PG사 API

### Toss Payments

| Method | Path | 설명 |
|--------|------|------|
| POST | `/toss/v1/payments/confirm` | 결제 승인 (paymentKey, orderId, amount) |
| POST | `/toss/v1/payments/{paymentKey}/cancel` | 결제 취소 (cancelReason, cancelAmount?) |
| GET | `/toss/v1/payments/{paymentKey}` | 결제 조회 (paymentKey) |
| GET | `/toss/v1/payments/orders/{orderId}` | 결제 조회 (orderId) |

### Kakao Pay

| Method | Path | 설명 |
|--------|------|------|
| POST | `/kakaopay/online/v1/payment/ready` | 결제 준비 (partner_order_id, total_amount, item_name) |
| POST | `/kakaopay/online/v1/payment/approve` | 결제 승인 (tid, pg_token) |
| POST | `/kakaopay/online/v1/payment/cancel` | 결제 취소 (tid, cancel_amount) |
| GET | `/kakaopay/online/v1/payment/order?tid=` | 결제 조회 |

### Naver Pay

| Method | Path | 설명 |
|--------|------|------|
| POST | `/naverpay/payments/v2.2/reserve` | 결제 예약 (merchantPayKey, totalPayAmount) |
| POST | `/naverpay/payments/v2.2/apply/payment` | 결제 승인 (paymentId) |
| POST | `/naverpay/payments/v1/cancel` | 결제 취소 (paymentId, cancelAmount) |
| GET | `/naverpay/payments/v2.2/list/history?paymentId=` | 결제 내역 조회 |

### Danal

| Method | Path | 설명 |
|--------|------|------|
| POST | `/danal/payments/ready` | 결제 준비 (orderId, amount) |
| POST | `/danal/payments/approve` | 결제 승인 (tid) |
| POST | `/danal/payments/{tid}/cancel` | 결제 취소 (cancelReason) |
| GET | `/danal/payments/{tid}` | 결제 조회 |

## 택배사 API

모든 택배사는 동일한 API 구조. `/advance` 엔드포인트로 배송 상태를 수동 진행 가능.

배송 상태 흐름: `ACCEPTED → IN_TRANSIT → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED`

### CJ 대한통운

| Method | Path | 설명 |
|--------|------|------|
| POST | `/carrier/cj/api/v1/shipments` | 택배 접수 (orderId, receiverName, receiverAddress, receiverPhone) |
| GET | `/carrier/cj/api/v1/tracking/{trackingNumber}` | 배송 추적 |
| POST | `/carrier/cj/api/v1/tracking/{trackingNumber}/advance` | 배송 상태 진행 (테스트용) |

### 로젠택배

| Method | Path | 설명 |
|--------|------|------|
| POST | `/carrier/logen/api/v1/shipments` | 택배 접수 |
| GET | `/carrier/logen/api/v1/tracking/{trackingNumber}` | 배송 추적 |
| POST | `/carrier/logen/api/v1/tracking/{trackingNumber}/advance` | 배송 상태 진행 |

### 롯데글로벌로지스

| Method | Path | 설명 |
|--------|------|------|
| POST | `/carrier/lotte/api/v1/shipments` | 택배 접수 |
| GET | `/carrier/lotte/api/v1/tracking/{trackingNumber}` | 배송 추적 |
| POST | `/carrier/lotte/api/v1/tracking/{trackingNumber}/advance` | 배송 상태 진행 |

### 우체국택배

| Method | Path | 설명 |
|--------|------|------|
| POST | `/carrier/epost/api/v1/shipments` | 택배 접수 |
| GET | `/carrier/epost/api/v1/tracking/{trackingNumber}` | 배송 추적 |
| POST | `/carrier/epost/api/v1/tracking/{trackingNumber}/advance` | 배송 상태 진행 |

## DB 스키마

### mock_payment

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| provider | VARCHAR(30) | TOSS, KAKAO_PAY, NAVER_PAY, DANAL |
| payment_key | VARCHAR(200) UK | PG사 결제키 |
| order_id | VARCHAR(100) | 가맹점 주문 ID |
| status | VARCHAR(30) | READY, DONE, CANCELED, REFUNDED |
| method | VARCHAR(30) | CARD, MONEY, PHONE 등 |
| total_amount | BIGINT | 총 결제 금액 |
| balance_amount | BIGINT | 잔여 금액 (부분취소 후) |
| cancel_amount | BIGINT | 취소 금액 |
| cancel_reason | VARCHAR(200) | 취소 사유 |
| approve_no | VARCHAR(30) | 승인번호 |
| approved_at | DATETIME(6) | 승인 일시 |
| canceled_at | DATETIME(6) | 취소 일시 |
| extra_data | TEXT | PG사별 추가 데이터 (JSON) |

### mock_shipment

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| carrier | VARCHAR(30) | CJ_LOGISTICS, LOGEN, LOTTE_GLOBAL, EPOST |
| tracking_number | VARCHAR(50) UK | 운송장 번호 |
| order_id | VARCHAR(100) | 주문 ID |
| status | VARCHAR(30) | ACCEPTED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED |
| sender_name | VARCHAR(50) | 보내는 사람 |
| receiver_name | VARCHAR(50) | 받는 사람 |
| receiver_address | VARCHAR(500) | 받는 주소 |
| receiver_phone | VARCHAR(20) | 받는 연락처 |
| delivered_at | DATETIME(6) | 배송 완료 일시 |

### mock_tracking_history

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| shipment_id | BIGINT | FK → mock_shipment.id |
| status | VARCHAR(30) | 상태 |
| description | VARCHAR(200) | 상세 설명 |
| location | VARCHAR(100) | 위치 |
| tracked_at | DATETIME(6) | 추적 일시 |

## 실행 방법

```bash
# 1. DB 생성
mysql -u root -p -e "CREATE DATABASE closet_external DEFAULT CHARACTER SET utf8mb4;"

# 2. 서버 실행
./gradlew :closet-external-api:bootRun

# 3. 테스트 — Toss 결제 승인
curl -X POST http://localhost:9090/toss/v1/payments/confirm \
  -H "Content-Type: application/json" \
  -d '{"paymentKey":"test_pk_001","orderId":"ORDER_001","amount":50000}'

# 4. 테스트 — CJ 택배 접수
curl -X POST http://localhost:9090/carrier/cj/api/v1/shipments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORDER_001","receiverName":"홍길동","receiverAddress":"서울시 강남구","receiverPhone":"010-1234-5678"}'

# 5. 테스트 — 배송 상태 진행
curl -X POST http://localhost:9090/carrier/cj/api/v1/tracking/CJ1000000000/advance
```

## 정산 활용

mock_payment + mock_shipment 데이터를 기반으로 정산 시스템에서:
- `mock_payment` JOIN `orders` ON order_id → 주문별 결제 현황
- `mock_shipment` JOIN `orders` ON order_id → 주문별 배송 현황
- PG사별/택배사별 집계, 수수료 계산 등에 활용
