# [Ticket #20] Toss Webhook Controller

## 개요
- TDD 참조: tdd.md 섹션 4.1.6 (pg_webhook_log 스키마), 4.2 (presentation/webhook/TossWebhookController)
- 선행 티켓: #10
- 크기: S

## 작업 내용

### 변경 사항

#### 1. TossWebhookController 구현
- `POST /api/v1/webhooks/toss` — Toss 결제 웹훅 수신
  - Toss에서 전달하는 이벤트를 수신하고 처리
  - **즉시 200 OK 응답** 후 비동기로 실제 처리 (Toss 타임아웃 방지)
  - 지원 이벤트 타입:
    - `PAYMENT_CONFIRMED`: 결제 승인 완료 (카드 직접 결제 시)
    - `PAYMENT_CANCELLED`: 결제 취소 완료
    - `BILLING_KEY_DELETED`: 빌링키 삭제 (사용자가 Toss에서 직접 삭제한 경우)

#### 2. Toss 시그니처 검증
- Toss 웹훅 시크릿 키를 사용한 HMAC-SHA256 시그니처 검증
- `Toss-Signature` 헤더 값과 payload 기반 계산 값 비교
- 시그니처 불일치 시 400 Bad Request 반환 (로깅 포함)

#### 3. 멱등성 보장 (pg_webhook_log)
- 웹훅 수신 즉시 pg_webhook_log 테이블에 INSERT 시도
- UNIQUE INDEX `(pg_provider, payment_key, event_type)`로 중복 체크
- 이미 처리된 웹훅: 200 OK 반환 + 로그에 IGNORED 기록
- 신규 웹훅: status=RECEIVED로 INSERT → 비동기 처리 → PROCESSED/FAILED 업데이트

#### 4. 비동기 처리 로직
- `@Async` 또는 `ApplicationEventPublisher`를 활용한 비동기 처리
- PAYMENT_CONFIRMED:
  - paymentKey로 Payment 조회 → Payment.approve() → Order 상태 전이
  - 상품 유형별 후속 처리 (Subscription 활성화 / Credit 충전)
- PAYMENT_CANCELLED:
  - paymentKey로 Payment 조회 → Payment.cancel() → Order 상태 전이
  - Refund 엔티티 상태 업데이트 (COMPLETED)
- BILLING_KEY_DELETED:
  - billingKey soft delete (deleted_at 기록)
  - 해당 workspace에 다른 active 빌링키 없으면 알림 발송 고려

#### 5. 에러 처리
- 비동기 처리 실패 시 pg_webhook_log.status=FAILED, error_message 기록
- 처리 실패해도 Toss에는 이미 200 OK 응답 완료 (재전송 방지)
- 실패 건 수동 재처리용 관리자 엔드포인트 또는 배치 고려 (이 티켓 범위 외)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | presentation | presentation/webhook/TossWebhookController.kt | 신규 |
| greeting_payment-server | presentation | presentation/webhook/dto/TossWebhookPayload.kt | 신규 |
| greeting_payment-server | application | application/WebhookProcessingService.kt | 신규 |
| greeting_payment-server | domain | domain/webhook/PgWebhookLog.kt | 신규 (엔티티) |
| greeting_payment-server | infrastructure | infrastructure/repository/PgWebhookLogRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/config/AsyncConfig.kt | 수정 (웹훅 처리용 ThreadPool 설정) |
| greeting_payment-server | application | application/PaymentService.kt | 수정 (웹훅 기반 상태 업데이트 메서드) |
| greeting_payment-server | infrastructure | infrastructure/config/TossWebhookProperties.kt | 신규 (시크릿 키 설정) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T20-01 | PAYMENT_CONFIRMED 웹훅 처리 | 유효한 시그니처, REQUESTED 상태 Payment 존재 | POST /webhooks/toss (PAYMENT_CONFIRMED) | 200 OK, Payment→APPROVED, Order 상태 전이, pg_webhook_log(PROCESSED) |
| T20-02 | PAYMENT_CANCELLED 웹훅 처리 | 유효한 시그니처, CANCEL_REQUESTED 상태 Payment | POST /webhooks/toss (PAYMENT_CANCELLED) | 200 OK, Payment→CANCELLED, Refund→COMPLETED |
| T20-03 | BILLING_KEY_DELETED 웹훅 처리 | 유효한 시그니처, active 빌링키 존재 | POST /webhooks/toss (BILLING_KEY_DELETED) | 200 OK, BillingKey soft delete |
| T20-04 | 중복 웹훅 멱등성 | 동일 (pg_provider, payment_key, event_type) 이미 PROCESSED | POST /webhooks/toss (동일 이벤트) | 200 OK, 재처리 안 함, pg_webhook_log(IGNORED) |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T20-E01 | 시그니처 검증 실패 | 잘못된 Toss-Signature 헤더 | POST /webhooks/toss | 400 Bad Request, pg_webhook_log INSERT 안 함 |
| T20-E02 | 존재하지 않는 paymentKey | paymentKey에 매칭되는 Payment 없음 | POST /webhooks/toss (PAYMENT_CONFIRMED) | 200 OK 즉시 응답, 비동기 처리 FAILED 기록 |
| T20-E03 | 비동기 처리 중 예외 | Payment 상태 전이 중 OptimisticLockException | POST /webhooks/toss | 200 OK 즉시 응답, pg_webhook_log(FAILED, error_message 기록) |
| T20-E04 | 빈 페이로드 | payload가 비어있거나 파싱 불가 | POST /webhooks/toss | 400 Bad Request |
| T20-E05 | 알 수 없는 이벤트 타입 | eventType=UNKNOWN_EVENT | POST /webhooks/toss | 200 OK, pg_webhook_log(IGNORED), 처리 스킵 |

## 기대 결과 (AC)
- [ ] POST /api/v1/webhooks/toss로 Toss 웹훅을 수신하고 시그니처 검증 후 200 OK를 즉시 응답한다
- [ ] PAYMENT_CONFIRMED 웹훅으로 Payment/Order 상태가 정상 전이된다
- [ ] PAYMENT_CANCELLED 웹훅으로 환불 완료 처리가 된다
- [ ] BILLING_KEY_DELETED 웹훅으로 빌링키가 soft delete 된다
- [ ] pg_webhook_log 테이블의 UNIQUE 인덱스로 동일 웹훅 중복 처리가 방지된다
- [ ] 비동기 처리 실패 시에도 Toss에 200 OK가 응답되며, 실패 내역이 pg_webhook_log에 기록된다
- [ ] 시그니처 검증 실패 시 400을 반환하고 처리하지 않는다
