# [Ticket #13] Subscription 도메인 — 구독 생명주기

## 개요
- TDD 참조: tdd.md 섹션 4.1.4, 4.2 (domain/subscription/), 4.4, 4.6, 8.1, 8.4
- 선행 티켓: #8, #9
- 크기: L

## 작업 내용

### 변경 사항

1. **SubscriptionStatus enum + 상태 전이 규칙**
   - `ACTIVE`: 정상 구독 중
   - `PAST_DUE`: 갱신 결제 실패, 재시도 중 (retry_count < 5)
   - `CANCELLED`: 사용자/관리자에 의한 해지
   - `EXPIRED`: 갱신 실패 최종 만료 (retry_count >= 5)
   - 전이 규칙:
     - `ACTIVE` → `PAST_DUE`, `CANCELLED`
     - `PAST_DUE` → `ACTIVE` (재시도 성공), `EXPIRED`, `CANCELLED`
     - `CANCELLED`, `EXPIRED` → 종료 상태 (전이 불가)
   - `canTransitionTo(target: SubscriptionStatus): Boolean`

2. **Subscription entity 구현**
   - `workspaceId`: 구독자 워크스페이스
   - `productId`: 구독 상품 (Product FK)
   - `status`: SubscriptionStatus (기본 ACTIVE)
   - `currentPeriodStart` / `currentPeriodEnd`: 현재 구독 기간
   - `billingIntervalMonths`: 결제 주기 (1=월, 12=연)
   - `autoRenew`: 자동 갱신 여부 (기본 true)
   - `retryCount`: 갱신 실패 재시도 횟수 (기본 0, 최대 5)
   - `lastOrderId`: 마지막 주문 ID
   - `cancelledAt`, `cancelReason`: 해지 정보
   - `version`: Optimistic Lock
   - BaseEntity 상속

3. **SubscriptionPolicy 구현**
   - **업그레이드 프로레이션 (미사용일 환불)**:
     - 남은 일수 계산: `currentPeriodEnd - today`
     - 미사용 금액 = `(남은 일수 / 전체 일수) * 기존 가격`
     - 새 상품 가격에서 미사용 금액 차감
     - 즉시 적용: 새 구독 기간 시작
   - **다운그레이드 (기간 종료 시 적용)**:
     - 현재 구독 기간이 끝날 때까지 기존 상품 유지
     - `scheduledProductId` 또는 `pendingDowngrade` 플래그로 예약
     - 다음 갱신 시 새 상품으로 변경
   - `calculateProration(subscription, newProduct, newPrice): Int` — 프로레이션 금액 계산
   - `isUpgrade(currentProduct, newProduct): Boolean`
   - `isDowngrade(currentProduct, newProduct): Boolean`

4. **SubscriptionRepository 구현**
   - `findByWorkspaceIdAndStatusIn(workspaceId: Int, statuses: List<SubscriptionStatus>): Subscription?`
   - `findByWorkspaceIdAndStatus(workspaceId: Int, status: SubscriptionStatus): Subscription?`
   - `findAllByCurrentPeriodEndBeforeAndAutoRenewTrueAndStatusIn(date: LocalDateTime, statuses: List<SubscriptionStatus>): List<Subscription>`
   - `findByIdAndDeletedAtIsNull(id: Long): Subscription?`

5. **SubscriptionService 구현**
   - `create(order: Order): Subscription`:
     - Order의 product 정보로 Subscription 생성
     - currentPeriodStart = now, currentPeriodEnd = now + billingIntervalMonths
     - status = ACTIVE, autoRenew = true
     - lastOrderId = order.id
   - `renew(subscription: Subscription, order: Order)`:
     - currentPeriodStart = 기존 currentPeriodEnd
     - currentPeriodEnd = currentPeriodStart + billingIntervalMonths
     - retryCount = 0
     - status = ACTIVE (PAST_DUE에서 복구 포함)
     - lastOrderId = order.id
   - `upgrade(workspaceId: Int, newProductCode: String)`:
     - 현재 ACTIVE 구독 조회
     - SubscriptionPolicy.isUpgrade() 검증
     - 프로레이션 계산 → UPGRADE 주문 생성 → 결제
     - 성공: 즉시 새 구독 기간 시작, 상품 변경
   - `downgrade(workspaceId: Int, newProductCode: String)`:
     - SubscriptionPolicy.isDowngrade() 검증
     - 현재 기간 유지, 다음 갱신 시 새 상품 적용 예약
   - `cancel(workspaceId: Int, reason: String)`:
     - status → CANCELLED
     - cancelledAt = now, cancelReason 기록
     - autoRenew = false
     - 현재 기간 끝까지는 서비스 유지 (즉시 해지 아님)
   - `expire(subscription: Subscription)`:
     - status → EXPIRED (retry_count >= 5일 때)
     - autoRenew = false

6. **retry_count 핸들링**
   - 갱신 실패 시: `retryCount++`
   - `retryCount < 5`: status → PAST_DUE, `currentPeriodEnd += 1 day` (버퍼)
   - `retryCount >= 5`: `expire()` 호출
   - 갱신 성공 시: `retryCount = 0`, status → ACTIVE

7. **Order 연동**
   - 구독 갱신 시 RENEWAL 타입 Order 자동 생성
   - 업그레이드 시 UPGRADE 타입 Order 생성 (프로레이션 반영 금액)
   - 다운그레이드 시 DOWNGRADE 타입 Order 생성 (다음 기간)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/subscription/SubscriptionStatus.kt | 신규 |
| greeting_payment-server | domain | domain/subscription/Subscription.kt | 신규 |
| greeting_payment-server | domain | domain/subscription/SubscriptionPolicy.kt | 신규 |
| greeting_payment-server | domain | domain/subscription/exception/SubscriptionNotFoundException.kt | 신규 |
| greeting_payment-server | domain | domain/subscription/exception/InvalidSubscriptionTransitionException.kt | 신규 |
| greeting_payment-server | domain | domain/subscription/exception/SubscriptionAlreadyExistsException.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/SubscriptionRepository.kt | 신규 |
| greeting_payment-server | application | application/SubscriptionService.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T13-01 | 구독 생성 | Order(PAID, SUBSCRIPTION) | create(order) | Subscription(ACTIVE, period 설정) |
| T13-02 | 구독 갱신 성공 | Subscription(ACTIVE, periodEnd=내일) | renew(sub, renewalOrder) | periodStart=기존End, periodEnd=+1month, retryCount=0 |
| T13-03 | 업그레이드 — 프로레이션 | BASIC→STANDARD, 잔여 15일/30일 | upgrade(ws1, PLAN_STANDARD) | 프로레이션 금액 차감된 Order 생성 |
| T13-04 | 다운그레이드 — 기간 종료 후 적용 | STANDARD→BASIC | downgrade(ws1, PLAN_BASIC) | 현재 기간 유지, 다음 갱신 시 BASIC 적용 |
| T13-05 | 해지 | Subscription(ACTIVE) | cancel(ws1, "더 이상 필요 없음") | status=CANCELLED, autoRenew=false |
| T13-06 | PAST_DUE → ACTIVE (재시도 성공) | Subscription(PAST_DUE, retryCount=2) | renew(sub, order) | status=ACTIVE, retryCount=0 |
| T13-07 | 프로레이션 계산 | 30일 중 15일 사용, 가격 30000원 | calculateProration() | 미사용 금액 = 15000원 |
| T13-08 | 만료 처리 | Subscription(PAST_DUE, retryCount=5) | expire(sub) | status=EXPIRED, autoRenew=false |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T13-E01 | 이미 ACTIVE 구독 있는데 생성 시도 | ws1에 ACTIVE 구독 존재 | create(order) | SubscriptionAlreadyExistsException |
| T13-E02 | CANCELLED 구독 갱신 시도 | Subscription(CANCELLED) | renew() | InvalidSubscriptionTransitionException |
| T13-E03 | EXPIRED 구독 갱신 시도 | Subscription(EXPIRED) | renew() | InvalidSubscriptionTransitionException |
| T13-E04 | 동일 상품으로 업그레이드 시도 | 현재 STANDARD → STANDARD | upgrade() | 동일 상품 업그레이드 불가 예외 |
| T13-E05 | 구독 없는 워크스페이스 해지 | ws에 ACTIVE 구독 없음 | cancel(ws) | SubscriptionNotFoundException |
| T13-E06 | 업그레이드인데 downgrade 호출 | BASIC → STANDARD는 업그레이드 | downgrade(ws1, PLAN_STANDARD) | 정책 검증 실패 예외 |
| T13-E07 | retryCount 버퍼 일수 확인 | retryCount=3 → 4 | 갱신 실패 | currentPeriodEnd += 1일, status=PAST_DUE |
| T13-E08 | Optimistic Lock 충돌 | 동시 업데이트 | renew() 동시 호출 | OptimisticLockException → 재시도 |

## 기대 결과 (AC)
- [ ] SubscriptionStatus가 ACTIVE, PAST_DUE, CANCELLED, EXPIRED 4개 상태와 전이 규칙을 구현한다
- [ ] Subscription entity가 workspaceId, productId, period, autoRenew, retryCount 등 핵심 필드를 가진다
- [ ] SubscriptionPolicy가 업그레이드 프로레이션(미사용일 환불)을 정확히 계산한다
- [ ] 다운그레이드는 현재 기간 종료 후 적용되도록 예약 처리된다
- [ ] cancel()이 즉시 해지가 아니라 기간 만료 후 해지로 동작한다 (autoRenew=false)
- [ ] retryCount가 5 이상이면 자동으로 EXPIRED 처리된다
- [ ] 갱신 실패 시 currentPeriodEnd에 +1일 버퍼가 추가된다
- [ ] RENEWAL/UPGRADE/DOWNGRADE 타입의 Order가 연동되어 생성된다
- [ ] 단위 테스트 커버리지 80% 이상
