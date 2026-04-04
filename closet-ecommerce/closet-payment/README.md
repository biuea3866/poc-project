# closet-payment

> 결제 승인, 결제 취소, 주문별 결제 조회 서비스

## 역할

closet-payment는 결제 도메인을 담당하는 서비스이다.
PG 결제 승인/취소를 처리하고, 주문 ID를 기반으로 결제 정보를 조회한다.
Kafka를 통해 결제 이벤트를 발행하며, 결제 수단으로 카드/계좌이체/가상계좌/모바일을 지원한다.

## 기술 스택

| 기술 | 용도 |
|------|------|
| Spring Boot Starter Web | REST API |
| Spring Data JPA | 엔티티 매핑, Repository |
| MySQL 8.0 (Flyway) | 데이터 저장 |
| Spring Data Redis | 캐싱 |
| Spring Kafka | 결제 이벤트 발행 |
| Virtual Threads | 가상 스레드 활성화 |

## 도메인 모델

### Payment
결제 엔티티. `orderId`, `paymentKey`(PG사 결제키), `method`(PaymentMethod), `finalAmount`(Money VO), `status`(PaymentStatus) 필드를 가진다.
`create()` 팩토리 메서드로 생성하며, `confirm()` 메서드로 결제 승인(paymentKey, method 설정), `cancel()` 메서드로 결제 취소를 처리한다.

### PaymentStatus
결제 상태 enum: `PENDING`, `PAID`, `CANCELLED`, `REFUNDED`.

### PaymentMethod
결제 수단 enum: `CARD`, `BANK_TRANSFER`, `VIRTUAL_ACCOUNT`, `MOBILE`.

## API

| Method | Path | 설명 |
|--------|------|------|
| GET | /api/v1/payments/orders/{orderId} | 주문별 결제 조회 |
| POST | /api/v1/payments/confirm | 결제 승인 (paymentKey, orderId, amount) |
| POST | /api/v1/payments/{id}/cancel | 결제 취소 (reason) |

## 패키지 구조

```
src/main/kotlin/com/closet/payment/
├── application/       # PaymentService, ConfirmPaymentRequest, CancelPaymentRequest, PaymentResponse
├── domain/            # Payment, PaymentStatus, PaymentMethod, PaymentRepository
└── presentation/      # PaymentController
```

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| payment | 결제 정보 (order_id, payment_key, method, final_amount, status) |

## 포트

- 서버 포트: 8084

## 의존 서비스

- closet-common (공통 라이브러리)
- Redis (캐싱)
- Kafka (결제 이벤트 발행)
