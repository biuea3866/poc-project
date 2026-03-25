# [Ticket #19] Credit API (잔액 조회/충전 주문/사용 내역)

## 개요
- TDD 참조: tdd.md 섹션 4.2 (presentation/CreditController), 4.5 (크레딧 충전 흐름), 4.1.5 (Credit Ledger 스키마)
- 선행 티켓: #15
- 크기: M

## 작업 내용

### 변경 사항

#### 1. CreditController 구현
- `GET /api/v1/credits/balance?workspaceId=&creditType=` — 크레딧 잔액 조회
  - creditType 필수: SMS, AI_EVALUATION
  - CreditBalance 테이블에서 조회 (없으면 balance=0 반환)
  - Response: CreditBalanceResponse (workspaceId, creditType, balance, updatedAt)

- `GET /api/v1/credits/transactions?workspaceId=&creditType=&page=&size=` — 거래 내역 조회
  - 필수: workspaceId, creditType
  - 선택: transactionType (CHARGE/USE/REFUND/EXPIRE/GRANT), dateFrom, dateTo
  - 페이지네이션: page (0-based), size (default 20, max 100)
  - 정렬: createdAt DESC
  - Response: Page<CreditTransactionResponse>

- `POST /api/v1/credits/charge` — 크레딧 충전 (주문 생성)
  - Request: `{ workspaceId, productCode, idempotencyKey? }`
  - 내부적으로 Order(type=PURCHASE) 생성 → Payment 처리 → CreditLedger CHARGE + CreditBalance 증가
  - productCode로 상품 조회 후 product_metadata에서 credit_amount/sms_count 추출
  - Response: CreditChargeResponse (order, creditBalance, chargedAmount)

#### 2. Request/Response DTO 정의
- `CreditChargeRequest`: workspaceId, productCode, idempotencyKey?
- `CreditBalanceResponse`: workspaceId, creditType, balance, updatedAt
- `CreditTransactionResponse`: id, transactionType, amount, balanceAfter, description, orderId?, createdAt, expiredAt?
- `CreditChargeResponse`: order(OrderSummaryResponse), creditType, chargedAmount, balanceAfter

#### 3. 권한 처리
- OWNER/MANAGER 역할만 허용
- workspaceId 소속 검증

#### 4. 충전 처리 흐름
1. Product 조회 (type=CONSUMABLE 검증)
2. Order 생성 (type=PURCHASE, OrderItem에 수량=1, 가격 스냅샷)
3. BillingKey 조회
4. Payment 생성 → PG 결제 → Payment APPROVED
5. CreditLedger INSERT (type=CHARGE, amount=+N, balanceAfter 계산)
6. CreditBalance UPDATE (balance += N, Optimistic Lock)
7. Order → COMPLETED

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | presentation | presentation/CreditController.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/credit/CreditChargeRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/credit/CreditBalanceResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/credit/CreditTransactionResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/credit/CreditChargeResponse.kt | 신규 |
| greeting_payment-server | application | application/CreditService.kt | 수정 (API용 메서드 추가: getBalance, getTransactions, charge) |
| greeting_payment-server | application | application/OrderService.kt | 수정 (크레딧 충전 주문 생성 연동) |
| greeting_payment-server | application | application/PaymentService.kt | 수정 (크레딧 충전 결제 처리 연동) |
| greeting_payment-server | infrastructure | infrastructure/repository/CreditLedgerRepository.kt | 수정 (검색 쿼리 추가) |
| greeting_payment-server | presentation | presentation/exception/PaymentExceptionHandler.kt | 수정 (크레딧 예외 핸들러 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T19-01 | SMS 잔액 조회 | workspace SMS balance=500 | GET /api/v1/credits/balance?workspaceId=1&creditType=SMS | 200 OK, balance=500 |
| T19-02 | 잔액 없는 크레딧 조회 | CreditBalance 레코드 없음 | GET /api/v1/credits/balance?workspaceId=1&creditType=AI_EVALUATION | 200 OK, balance=0 |
| T19-03 | 거래 내역 조회 | CHARGE 3건, USE 5건 존재 | GET /api/v1/credits/transactions?workspaceId=1&creditType=SMS | 200 OK, 8건 반환 (최신순) |
| T19-04 | 거래 내역 타입 필터 | CHARGE 3건, USE 5건 | GET /transactions?transactionType=CHARGE | 200 OK, 3건만 반환 |
| T19-05 | SMS 크레딧 충전 성공 | SMS_PACK_1000 상품, 빌링키 존재 | POST /api/v1/credits/charge (productCode=SMS_PACK_1000) | 200 OK, Order(COMPLETED), balance += 1000 |
| T19-06 | AI 크레딧 충전 성공 | AI_CREDIT_100 상품, 빌링키 존재 | POST /api/v1/credits/charge (productCode=AI_CREDIT_100) | 200 OK, Order(COMPLETED), balance += 100 |
| T19-07 | 멱등성 키 충전 중복 방지 | 동일 idempotencyKey 주문 존재 | POST /charge (동일 idempotencyKey) | 200 OK, 기존 주문 반환 (중복 충전 안 함) |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T19-E01 | SUBSCRIPTION 상품으로 충전 시도 | productCode=PLAN_STANDARD | POST /charge | 400, InvalidProductTypeException (CONSUMABLE만 가능) |
| T19-E02 | 빌링키 없이 충전 시도 | 등록된 빌링키 없음 | POST /charge | 400, BillingKeyNotFoundException |
| T19-E03 | PG 결제 실패 | Toss 결제 거절 | POST /charge | 500, PaymentFailedException, CreditBalance 변경 없음 |
| T19-E04 | 잘못된 creditType | creditType=INVALID | GET /balance?creditType=INVALID | 400, InvalidCreditTypeException |
| T19-E05 | MEMBER 권한 접근 | MEMBER 역할 | POST /charge | 403 Forbidden |
| T19-E06 | 다른 workspace 잔액 조회 | workspaceId=2 사용자가 workspaceId=1 조회 | GET /balance?workspaceId=1 | 403, WorkspaceAccessDeniedException |
| T19-E07 | 동시 충전 Optimistic Lock 충돌 | 동일 workspace 동시 2건 충전 | POST /charge x 2 | 1건 성공, 1건 재시도 후 성공 (balance 정합성 보장) |

## 기대 결과 (AC)
- [ ] GET /api/v1/credits/balance로 workspace의 크레딧 타입별 잔액을 조회할 수 있다
- [ ] GET /api/v1/credits/transactions로 거래 내역을 페이지네이션하여 조회할 수 있다 (타입/기간 필터 지원)
- [ ] POST /api/v1/credits/charge로 크레딧 충전 시 Order 생성 → Payment → CreditLedger CHARGE → CreditBalance 증가가 원자적으로 처리된다
- [ ] CONSUMABLE 타입 상품만 충전 가능하고, 다른 타입은 거부된다
- [ ] CreditBalance의 Optimistic Lock으로 동시 충전 시 잔액 정합성이 보장된다
- [ ] idempotencyKey로 중복 충전이 방지된다
- [ ] OWNER/MANAGER 권한만 모든 크레딧 API에 접근 가능하다
