# [Ticket #11] ManualPaymentGateway 구현 (백오피스)

## 개요
- TDD 참조: tdd.md 섹션 4.3, 4.2 (infrastructure/pg/ManualPaymentGateway.kt), 8.4
- 선행 티켓: #9
- 크기: S

## 작업 내용

### 변경 사항

1. **ManualPaymentGateway 구현**
   - `PaymentGateway` 인터페이스 구현
   - `gatewayName = "MANUAL"`
   - 백오피스에서 관리자가 수동으로 플랜을 부여하거나 크레딧을 지급할 때 사용
   - 실제 PG 호출 없이 즉시 승인 처리 (amount=0)

2. **chargeByBillingKey() 구현**
   - PG 호출 없음 — 즉시 성공 PaymentResult 반환
   - `paymentKey`: `MANUAL-{UUID}` 형식으로 자체 생성
   - `approvedAt`: 현재 시각
   - `receiptUrl`: null
   - amount=0 검증: 0이 아닌 금액이 들어오면 `ManualPaymentAmountException`

3. **confirmPayment() 구현**
   - Manual gateway에서는 사용하지 않음
   - 호출 시 `UnsupportedOperationException` — Manual은 빌링키 방식만 지원

4. **cancelPayment() 구현**
   - PG 호출 없음 — 즉시 성공 PaymentResult 반환
   - 취소 사유 기록

5. **감사 로그 (Audit Log)**
   - 모든 Manual 결제에 대해 감사 로그 기록
   - 기록 항목: 실행자(admin), 대상 워크스페이스, 상품, 금액(0), 사유
   - 기존 `User_planlogsonbackoffice` 테이블 대체 용도
   - 로그 레벨: INFO

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | infrastructure | infrastructure/pg/ManualPaymentGateway.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/ManualPaymentAmountException.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T11-01 | 수동 결제 성공 (amount=0) | amount=0, billingKey=any | chargeByBillingKey() | PaymentResult(success=true, paymentKey="MANUAL-...") |
| T11-02 | paymentKey 형식 확인 | - | chargeByBillingKey() | paymentKey가 "MANUAL-" 접두사 |
| T11-03 | 수동 취소 성공 | 유효한 paymentKey | cancelPayment() | PaymentResult(success=true) |
| T11-04 | gatewayName 확인 | - | gateway.gatewayName | "MANUAL" |
| T11-05 | 감사 로그 기록 | 수동 결제 실행 | chargeByBillingKey() | INFO 로그에 admin, workspace, product 기록 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T11-E01 | amount > 0 거부 | amount=10000 | chargeByBillingKey() | ManualPaymentAmountException |
| T11-E02 | confirmPayment 미지원 | - | confirmPayment() | UnsupportedOperationException |

## 기대 결과 (AC)
- [ ] ManualPaymentGateway가 PaymentGateway 인터페이스를 구현하며 gatewayName="MANUAL"이다
- [ ] chargeByBillingKey()가 PG 호출 없이 즉시 성공 PaymentResult를 반환한다
- [ ] amount=0만 허용하며, 0이 아닌 금액은 ManualPaymentAmountException으로 거부한다
- [ ] paymentKey가 "MANUAL-{UUID}" 형식으로 자체 생성된다
- [ ] 모든 수동 결제에 대해 감사 목적의 INFO 로그가 기록된다
- [ ] 단위 테스트 커버리지 90% 이상
