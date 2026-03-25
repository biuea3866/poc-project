# [Ticket #10] TossPaymentGateway 구현

## 개요
- TDD 참조: tdd.md 섹션 4.3, 4.2 (infrastructure/pg/TossPaymentGateway.kt)
- 선행 티켓: #9
- 크기: M

## 작업 내용

### 변경 사항

1. **TossPaymentGateway 구현**
   - `PaymentGateway` 인터페이스 구현
   - `gatewayName = "TOSS"`
   - 기존 payment-server의 Toss API 연동 코드를 이 구현체로 리팩토링

2. **chargeByBillingKey() 구현**
   - Toss Payments 빌링 API 호출: `POST /v1/billing/{billingKey}`
   - Request: billingKey, amount, orderId, orderName, customerKey
   - Response → PaymentResult 매핑
   - 실패 시 Toss 에러 코드를 도메인 예외로 변환

3. **confirmPayment() 구현**
   - Toss Payments 승인 API 호출: `POST /v1/payments/confirm`
   - Request: paymentKey, orderId, amount
   - Response → PaymentResult 매핑
   - amount 불일치 시 `PaymentAmountMismatchException`

4. **cancelPayment() 구현**
   - Toss Payments 취소 API 호출: `POST /v1/payments/{paymentKey}/cancel`
   - Request: cancelReason, cancelAmount (부분 취소 지원)
   - Response → PaymentResult 매핑

5. **에러 코드 매핑**
   - Toss 에러 코드 → 도메인 예외 변환 테이블:
     | Toss 에러 코드 | 도메인 예외 |
     |---------------|-----------|
     | `INVALID_CARD_COMPANY` | `PaymentCardException` |
     | `EXCEED_MAX_AMOUNT` | `PaymentAmountExceededException` |
     | `NOT_FOUND_PAYMENT` | `PaymentNotFoundException` |
     | `ALREADY_PROCESSED_PAYMENT` | 멱등 처리 (성공 반환) |
     | `INVALID_BILLING_KEY` | `InvalidBillingKeyException` |
     | 그 외 | `PaymentGatewayException(code, message)` |

6. **Request/Response 로깅**
   - 모든 PG API 호출에 대해 request/response 로깅
   - 민감 정보 마스킹: billingKey 값은 앞 8자리만, 나머지 `****`
   - 로그 레벨: 성공=INFO, 실패=WARN
   - 타임아웃, 네트워크 에러 시 ERROR 레벨

7. **HTTP Client 설정**
   - 기존 RestTemplate/WebClient 설정 재사용
   - Connection timeout: 5초
   - Read timeout: 30초
   - Retry: 네트워크 에러 시 1회 재시도 (idempotent 요청만)

8. **Toss API 인증**
   - Basic Auth: `Base64(secretKey + ":")`
   - secretKey는 기존 설정값 (application.yml) 재사용

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | infrastructure | infrastructure/pg/TossPaymentGateway.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/pg/TossApiClient.kt | 신규 또는 리팩토링 |
| greeting_payment-server | infrastructure | infrastructure/pg/TossErrorCodeMapper.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/pg/dto/TossPaymentRequest.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/pg/dto/TossPaymentResponse.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/pg/dto/TossCancelRequest.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/pg/dto/TossBillingRequest.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/PaymentGatewayException.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/PaymentCardException.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/InvalidBillingKeyException.kt | 신규 |
| greeting_payment-server | config | config/TossPaymentConfig.kt | 수정 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T10-01 | 빌링키 결제 성공 | 유효한 billingKey, amount=30000 | chargeByBillingKey() | PaymentResult(success=true, paymentKey 존재) |
| T10-02 | 카드 결제 승인 성공 | 유효한 paymentKey, amount 일치 | confirmPayment() | PaymentResult(success=true, approvedAt 존재) |
| T10-03 | 결제 취소 성공 | 유효한 paymentKey | cancelPayment(paymentKey, 30000, "고객 요청") | PaymentResult(success=true) |
| T10-04 | 부분 취소 성공 | 전체 30000원 중 10000원 취소 | cancelPayment(paymentKey, 10000, "부분 환불") | PaymentResult(success=true) |
| T10-05 | API 요청/응답 로깅 | 정상 호출 | chargeByBillingKey() | INFO 레벨 로그 출력 |
| T10-06 | gatewayName 확인 | - | gateway.gatewayName | "TOSS" |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T10-E01 | 유효하지 않은 카드사 | Toss → INVALID_CARD_COMPANY | chargeByBillingKey() | PaymentCardException |
| T10-E02 | 금액 초과 | Toss → EXCEED_MAX_AMOUNT | chargeByBillingKey() | PaymentAmountExceededException |
| T10-E03 | 유효하지 않은 빌링키 | Toss → INVALID_BILLING_KEY | chargeByBillingKey() | InvalidBillingKeyException |
| T10-E04 | 이미 처리된 결제 (멱등) | Toss → ALREADY_PROCESSED_PAYMENT | confirmPayment() | PaymentResult(success=true) 반환 (에러 아님) |
| T10-E05 | 네트워크 타임아웃 | Connection timeout | chargeByBillingKey() | PaymentGatewayException, ERROR 로그 |
| T10-E06 | 알 수 없는 에러 코드 | Toss → UNKNOWN_ERROR | chargeByBillingKey() | PaymentGatewayException(code, message) |
| T10-E07 | billingKey 마스킹 확인 | billingKey="abcdefghijklmnop" | 로그 출력 | "abcdefgh****" 형태 |
| T10-E08 | 금액 불일치 | Toss 응답 amount ≠ 요청 amount | confirmPayment() | PaymentAmountMismatchException |

## 기대 결과 (AC)
- [ ] TossPaymentGateway가 PaymentGateway 인터페이스를 구현하며 chargeByBillingKey, confirmPayment, cancelPayment를 모두 지원한다
- [ ] 기존 Toss API 연동 코드가 이 구현체로 통합/리팩토링된다
- [ ] Toss 에러 코드가 도메인 예외로 정확히 매핑된다
- [ ] ALREADY_PROCESSED_PAYMENT 에러는 멱등하게 성공으로 처리된다
- [ ] 모든 PG API 호출에 request/response 로깅이 포함되며 민감 정보가 마스킹된다
- [ ] 네트워크 에러 시 1회 재시도가 동작한다
- [ ] 단위 테스트 커버리지 80% 이상 (PG API 호출은 Mock 처리)
