# closet-external-api

> 외부 PG사 및 택배사 API를 모사(Mock)하는 서버 -- 개발/테스트 환경에서 실제 외부 연동 없이 결제/배송 흐름을 검증

## 역할

closet-external-api는 실제 PG사(Toss Payments, Kakao Pay, Naver Pay, Danal)와 택배사(CJ대한통운, 우체국, 로젠, 롯데글로벌) API를 모사하는 Mock 서버이다.
각 PG사/택배사의 실제 API 스펙을 참고하여 동일한 요청/응답 형식을 제공하며, MySQL에 결제/배송 데이터를 저장하여 상태를 관리한다.
개발 및 테스트 환경에서 외부 서비스 의존 없이 전체 결제/배송 플로우를 검증할 수 있다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Data JPA | 엔티티 매핑, Repository |
| MySQL 8.0 (Flyway) | Mock 결제/배송 데이터 저장 (closet_external DB) |

## 도메인 모델

### MockPayment
Mock 결제 데이터 엔티티. `provider`(PG사), `paymentKey`, `orderId`, `status`(READY/DONE/CANCELED/REFUNDED), `method`, `totalAmount`, `balanceAmount`, `cancelAmount`, `orderName`, `buyerName`, `cardNumber`, `approveNo`, `approvedAt`, `canceledAt` 등 필드를 가진다.
`approve()` 메서드로 결제 승인, `cancel()` 메서드로 전체/부분 취소를 처리한다.

### MockShipment
Mock 배송 데이터 엔티티. `carrier`(택배사), `trackingNumber`, `orderId`, `status`(ACCEPTED/IN_TRANSIT/OUT_FOR_DELIVERY/DELIVERED), `senderName`, `receiverName`, `receiverAddress`, `receiverPhone`, `deliveredAt` 필드를 가진다.
`addTrackingEvent()` 메서드로 배송 추적 이벤트를 기록한다.

### MockTrackingHistory
배송 추적 이력 엔티티. `shipment`(ManyToOne), `status`, `description`, `location`, `trackedAt` 필드.

## API

### PG사 Mock API

#### Toss Payments (`/toss/v1/payments`)

| Method | Path | 설명 |
|--------|------|------|
| POST | /toss/v1/payments/confirm | 결제 승인 |
| POST | /toss/v1/payments/{paymentKey}/cancel | 결제 취소 |
| GET | /toss/v1/payments/{paymentKey} | paymentKey로 결제 조회 |
| GET | /toss/v1/payments/orders/{orderId} | orderId로 결제 조회 |

#### Kakao Pay (`/kakaopay/online/v1/payment`)

| Method | Path | 설명 |
|--------|------|------|
| POST | /kakaopay/online/v1/payment/ready | 결제 준비 (tid 발급) |
| POST | /kakaopay/online/v1/payment/approve | 결제 승인 |
| POST | /kakaopay/online/v1/payment/cancel | 결제 취소 |
| GET | /kakaopay/online/v1/payment/order | 주문 조회 |

#### Naver Pay (`/naverpay/payments`)

| Method | Path | 설명 |
|--------|------|------|
| POST | /naverpay/payments/v2.2/reserve | 결제 예약 |
| POST | /naverpay/payments/v2.2/apply/payment | 결제 승인 |
| POST | /naverpay/payments/v1/cancel | 결제 취소 |
| GET | /naverpay/payments/v2.2/list/history | 결제 이력 조회 |

#### Danal (`/danal/payments`)

| Method | Path | 설명 |
|--------|------|------|
| POST | /danal/payments/ready | 결제 준비 (tid 발급) |
| POST | /danal/payments/approve | 결제 승인 |
| POST | /danal/payments/{tid}/cancel | 결제 취소 |
| GET | /danal/payments/{tid} | 결제 조회 |

### 택배사 Mock API

각 택배사 공통 API 구조 (CJ대한통운/우체국/로젠/롯데글로벌):

| Method | Path | 설명 |
|--------|------|------|
| POST | /carrier/{carrier}/api/v1/shipments | 배송 등록 |
| GET | /carrier/{carrier}/api/v1/tracking/{trackingNumber} | 배송 추적 조회 |
| POST | /carrier/{carrier}/api/v1/tracking/{trackingNumber}/advance | 배송 상태 진행 (테스트용) |

carrier: `cj`, `epost`, `logen`, `lotte`

## 패키지 구조

```
src/main/kotlin/com/closet/external/
├── carrier/
│   ├── CarrierService          # 택배 공통 서비스
│   ├── cj/CjLogisticsController
│   ├── epost/EpostController
│   ├── logen/LogenController
│   └── lotte/LotteGlobalController
├── domain/
│   ├── MockPayment             # Mock 결제 엔티티
│   ├── MockPaymentRepository
│   ├── MockShipment            # Mock 배송 엔티티 + MockTrackingHistory
│   └── ...
└── payment/
    ├── PaymentPgService        # PG 공통 서비스
    ├── danal/DanalController
    ├── kakaopay/KakaoPayController
    ├── naverpay/NaverPayController
    └── toss/TossPaymentsController
```

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| mock_payment | Mock PG 결제 데이터 (provider, payment_key, order_id, status, method, total_amount, balance_amount, cancel_amount 등) |
| mock_shipment | Mock 택배 배송 데이터 (carrier, tracking_number, order_id, status, sender/receiver 정보) |
| mock_tracking_history | Mock 배송 추적 이력 (shipment_id, status, description, location, tracked_at) |

## 포트

- 서버 포트: 9090

## 의존 서비스

독립 서비스 (다른 closet 서비스에 의존하지 않음, 별도 DB closet_external 사용)
