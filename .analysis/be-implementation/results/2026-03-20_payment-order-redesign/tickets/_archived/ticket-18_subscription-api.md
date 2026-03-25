# [Ticket #18] Subscription API (구독 조회/업그레이드/다운그레이드/해지)

## 개요
- TDD 참조: tdd.md 섹션 4.2 (presentation/SubscriptionController), 4.4 (플랜 구독 결제 흐름), 4.1.4 (Subscription 스키마), 8.1 (상품 유형별 주문 처리)
- 선행 티켓: #13
- 크기: M

## 작업 내용

### 변경 사항

#### 1. SubscriptionController 구현
- `GET /api/v1/subscriptions/current?workspaceId=` — 현재 구독 조회
  - workspace의 ACTIVE/PAST_DUE 상태 구독 반환
  - 구독 없으면 FREE 플랜 정보 기본 반환
  - Response: SubscriptionDetailResponse (구독 상태 + 상품 정보 + 기간 정보)

- `POST /api/v1/subscriptions/upgrade` — 플랜 업그레이드
  - Request: `{ workspaceId, targetProductCode }`
  - 내부적으로 Order(type=UPGRADE) 생성 → Payment 처리 → Subscription 변경
  - 프로레이션 계산: 현재 구독 남은 기간 금액 환불 + 새 플랜 전체 기간 결제
  - 즉시 적용 (current_period_start = now, 새 period 계산)
  - Response: SubscriptionUpgradeResponse (newSubscription, prorationDetail, order)

- `POST /api/v1/subscriptions/downgrade` — 플랜 다운그레이드
  - Request: `{ workspaceId, targetProductCode }`
  - 현재 구독 기간 만료 시점에 적용 (즉시 변경 아님)
  - Subscription에 pendingProductId 마킹, 갱신 시점에 실제 변경
  - Response: SubscriptionDowngradeResponse (currentSubscription, pendingChange, effectiveDate)

- `POST /api/v1/subscriptions/cancel` — 구독 해지
  - Request: `{ workspaceId, cancelReason? }`
  - auto_renew = false로 변경, 현재 기간까지는 유지
  - cancelled_at, cancel_reason 기록
  - Response: SubscriptionDetailResponse (status=ACTIVE, autoRenew=false, cancelledAt 포함)

- `GET /api/v1/subscriptions/upgrade/preview?workspaceId=&targetProductCode=` — 업그레이드 미리보기
  - 결제 전 프로레이션 계산 결과만 반환
  - Response: ProrationPreviewResponse (currentPlanRefund, newPlanCharge, netAmount)

#### 2. 프로레이션 계산 로직
- 남은 일수 기반 일할 계산: `refundAmount = (remainingDays / totalDays) * currentPlanPrice`
- 새 플랜 결제: 전체 billing_interval 기간 금액
- netAmount = newPlanCharge - currentPlanRefund (음수면 0 처리, 부분 환불만)

#### 3. Request/Response DTO 정의
- `SubscriptionUpgradeRequest`: workspaceId, targetProductCode
- `SubscriptionDowngradeRequest`: workspaceId, targetProductCode
- `SubscriptionCancelRequest`: workspaceId, cancelReason?
- `SubscriptionDetailResponse`: subscriptionId, workspaceId, productCode, productName, status, currentPeriodStart, currentPeriodEnd, billingIntervalMonths, autoRenew, cancelledAt, cancelReason
- `SubscriptionUpgradeResponse`: subscription(SubscriptionDetailResponse), prorationDetail(ProrationPreviewResponse), order(OrderSummaryResponse)
- `SubscriptionDowngradeResponse`: currentSubscription, pendingChange(productCode, productName), effectiveDate
- `ProrationPreviewResponse`: currentPlanCode, currentPlanPrice, remainingDays, totalDays, refundAmount, newPlanCode, newPlanPrice, chargeAmount, netAmount

#### 4. 권한 처리
- OWNER/MANAGER 역할만 허용
- workspaceId 소속 검증

#### 5. 검증 규칙
- 업그레이드: targetProduct의 plan_level이 현재보다 높아야 함 (product_metadata 활용)
- 다운그레이드: targetProduct의 plan_level이 현재보다 낮아야 함
- 동일 플랜 변경 불가
- EXPIRED/CANCELLED 상태에서는 upgrade/downgrade 불가 (새 구독 필요)

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | presentation | presentation/SubscriptionController.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionUpgradeRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionDowngradeRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionCancelRequest.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionDetailResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionUpgradeResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/SubscriptionDowngradeResponse.kt | 신규 |
| greeting_payment-server | presentation | presentation/dto/subscription/ProrationPreviewResponse.kt | 신규 |
| greeting_payment-server | application | application/SubscriptionService.kt | 수정 (API용 메서드 추가: upgrade, downgrade, cancel, preview) |
| greeting_payment-server | domain | domain/subscription/SubscriptionPolicy.kt | 수정 (프로레이션 계산 로직) |
| greeting_payment-server | domain | domain/subscription/Subscription.kt | 수정 (pendingProductId 필드 추가) |
| greeting_payment-server | presentation | presentation/exception/PaymentExceptionHandler.kt | 수정 (구독 예외 핸들러 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T18-01 | 현재 구독 조회 | workspace에 ACTIVE Standard 구독 존재 | GET /api/v1/subscriptions/current?workspaceId=1 | 200 OK, Standard 구독 정보 반환 |
| T18-02 | 구독 없는 workspace 조회 | 구독 없음 | GET /api/v1/subscriptions/current?workspaceId=1 | 200 OK, FREE 플랜 기본 정보 |
| T18-03 | 업그레이드 미리보기 | Basic(33,000/월) 구독 중, 15일 남음 | GET /upgrade/preview?targetProductCode=PLAN_STANDARD | 200 OK, refund=16,500, charge=55,000, net=38,500 |
| T18-04 | 플랜 업그레이드 실행 | Basic → Standard 업그레이드 | POST /api/v1/subscriptions/upgrade | 200 OK, 새 Subscription(ACTIVE, Standard), Order(UPGRADE, COMPLETED) 생성 |
| T18-05 | 플랜 다운그레이드 예약 | Standard → Basic 다운그레이드 | POST /api/v1/subscriptions/downgrade | 200 OK, pendingChange=Basic, effectiveDate=현재 기간 종료일 |
| T18-06 | 구독 해지 | ACTIVE 구독 | POST /api/v1/subscriptions/cancel | 200 OK, autoRenew=false, cancelledAt 기록 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T18-E01 | 동일 플랜 업그레이드 시도 | Standard 구독 중 | POST /upgrade (targetProductCode=PLAN_STANDARD) | 400, SamePlanException |
| T18-E02 | 낮은 플랜으로 업그레이드 시도 | Standard 구독 중 | POST /upgrade (targetProductCode=PLAN_BASIC) | 400, InvalidPlanTransitionException |
| T18-E03 | 높은 플랜으로 다운그레이드 시도 | Basic 구독 중 | POST /downgrade (targetProductCode=PLAN_STANDARD) | 400, InvalidPlanTransitionException |
| T18-E04 | EXPIRED 상태에서 업그레이드 | 만료된 구독 | POST /upgrade | 409, SubscriptionNotActiveException |
| T18-E05 | 빌링키 없이 업그레이드 | 등록된 빌링키 없음 | POST /upgrade | 400, BillingKeyNotFoundException |
| T18-E06 | MEMBER 권한 접근 | MEMBER 역할 | POST /upgrade | 403 Forbidden |
| T18-E07 | 이미 해지 예약된 구독 재해지 | autoRenew=false 상태 | POST /cancel | 409, AlreadyCancelledException |

## 기대 결과 (AC)
- [ ] GET /api/v1/subscriptions/current로 workspace의 현재 구독 상태를 조회할 수 있다
- [ ] 업그레이드 미리보기로 프로레이션 금액(환불액, 신규 결제액, 순결제액)을 사전 확인할 수 있다
- [ ] POST /upgrade 시 Order(UPGRADE) 생성 → Payment → Subscription 변경이 원자적으로 처리된다
- [ ] POST /downgrade 시 즉시 변경 없이 다음 갱신 시점에 적용되도록 예약된다
- [ ] POST /cancel 시 autoRenew=false로 변경되고 현재 기간까지는 구독이 유지된다
- [ ] 플랜 레벨 검증: 업그레이드는 상위로만, 다운그레이드는 하위로만 허용된다
- [ ] OWNER/MANAGER 권한만 모든 구독 API에 접근 가능하다
