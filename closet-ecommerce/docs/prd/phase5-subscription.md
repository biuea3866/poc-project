# PRD: 의류 구독/대여 서비스 (closet-subscription)

**Phase**: 5
**우선순위**: Critical
**모델**: C — 구매+대여 하이브리드
**벤치마킹**: 에어클로젯(JP), 렌트더런웨이(US), 프로젝트앤(KR)

---

## 1. 개요

월정액 구독으로 의류를 대여하고, 마음에 들면 할인 가격에 구매 전환할 수 있는 서비스. "소유에서 경험으로" 트렌드에 대응하며, 정기 매출(MRR)과 구매 전환 매출을 동시에 확보한다.

### 핵심 가치
- **고객**: 적은 비용으로 다양한 옷을 입어볼 수 있음
- **비즈니스**: MRR + 구매 전환 매출, 재고 활용도 극대화
- **셀러**: 신규 고객 접점, 대여→구매 전환율 데이터

---

## 2. 구독 플랜

| 플랜 | 월 가격 | 동시 대여 | 구매 전환 할인 | 연장 | 타겟 |
|------|---------|----------|--------------|------|------|
| Basic | 29,900원 | 3벌 | 30% OFF | 1회/+15일 | 입문/탐색 |
| Standard | 49,900원 | 5벌 | 40% OFF | 2회/+15일 | 메인 사용자 |
| Premium | 79,900원 | 8벌 | 50% OFF | 무제한 | 패션 헤비유저 |

### 쿨링오프
- 첫 결제 후 7일 이내, 대여 미이용 시 전액 환불

---

## 3. 비즈니스 플로우

```
1. 구독 가입 → 플랜 선택 → 빌링키 등록 → 첫 결제
2. 상품 선택 (큐레이션 추천 + 직접 선택)
3. 배송 (closet-fulfillment 연동)
4. 착용 (대여 기간: 30일)
5. 선택:
   a. 반납 → 검수 → 재입고 → 다음 아이템 선택
   b. 구매 전환 → 할인 적용 → 주문 생성 → 대여 슬롯 회복
   c. 연장 → 대여 기간 +15일
6. 다음 결제일 → 자동 갱신
```

---

## 4. 핵심 엔티티

### SubscriptionPlan (구독 플랜 정의)
- id, planName, monthlyPrice, maxRentalItems, purchaseDiscountRate
- extensionLimit, extensionDays, trialDays
- isActive, createdAt, updatedAt

### Subscription (사용자 구독)
- id, memberId, planId, status(TRIAL/ACTIVE/PAUSED/CANCELLED)
- billingKeyId, currentPeriodStartAt, currentPeriodEndAt, nextBillingAt
- cancelledAt, cancelReason, createdAt, updatedAt

### RentalItem (대여 아이템)
- id, subscriptionId, productId, productOptionId, sku
- status(RESERVED/SHIPPED/RENTED/RETURN_REQUESTED/RETURNED/INSPECTING/AVAILABLE/PURCHASE_CONVERTED/DAMAGED)
- rentedAt, dueAt, extendedCount, returnedAt, inspectedAt
- createdAt, updatedAt

### RentalHistory (대여/반납/구매 이력)
- id, subscriptionId, rentalItemId, action(RENT/RETURN/EXTEND/PURCHASE_CONVERT/DAMAGE_CHARGE)
- amount, description, createdAt

### ReturnInspection (반납 검수)
- id, rentalItemId, inspectorId, conditionGrade(EXCELLENT/GOOD/FAIR/DAMAGED/LOST)
- needsCleaning, needsRepair, damageDescription, chargeAmount
- inspectedAt, createdAt

---

## 5. 상태 머신

### Subscription
```
TRIAL → ACTIVE (첫 결제 후 or 7일 후)
ACTIVE → PAUSED (일시정지, 최대 2회/년)
PAUSED → ACTIVE (재개)
ACTIVE → CANCELLED (해지 요청, 모든 아이템 반납 완료 후 확정)
```

### RentalItem
```
RESERVED → SHIPPED (배송 시작)
SHIPPED → RENTED (배송 완료)
RENTED → RETURN_REQUESTED (반납 신청)
RETURN_REQUESTED → RETURNED (수거 완료)
RETURNED → INSPECTING (검수 중)
INSPECTING → AVAILABLE (재입고, 상태 양호)
INSPECTING → DAMAGED (손상, 과금)

RENTED → PURCHASE_CONVERTED (구매 전환, 반납 불필요)
```

---

## 6. API 스펙

### 구독 관리
- `POST /api/v1/subscriptions` — 구독 가입 (플랜 선택 + 빌링키)
- `GET /api/v1/subscriptions/me` — 내 구독 조회
- `PUT /api/v1/subscriptions/me/pause` — 일시정지
- `PUT /api/v1/subscriptions/me/resume` — 재개
- `POST /api/v1/subscriptions/me/cancel` — 해지 요청
- `GET /api/v1/subscription-plans` — 플랜 목록

### 대여
- `POST /api/v1/rentals` — 아이템 대여 신청
- `GET /api/v1/rentals/me` — 내 대여 목록
- `POST /api/v1/rentals/{id}/extend` — 대여 연장
- `POST /api/v1/rentals/{id}/return` — 반납 신청
- `POST /api/v1/rentals/{id}/purchase` — 구매 전환

### 검수 (관리자)
- `POST /api/v1/admin/inspections` — 검수 결과 등록
- `GET /api/v1/admin/inspections?status=PENDING` — 검수 대기 목록

---

## 7. Kafka 이벤트

토픽: `event.closet.subscription`

| 이벤트 | 발행 시점 | Downstream |
|--------|----------|-----------|
| subscription.created | 구독 가입 | notification (환영 알림) |
| subscription.cancelled | 구독 해지 | notification, analytics |
| rental.shipped | 배송 시작 | notification |
| rental.returned | 반납 완료 | notification |
| rental.purchaseConverted | 구매 전환 | order (주문 생성), inventory (재고 차감), payment (결제) |
| inspection.completed | 검수 완료 | inventory (재입고), notification |
| inspection.damaged | 손상 감지 | payment (과금), notification |
| billing.success | 정기 결제 성공 | notification |
| billing.failed | 정기 결제 실패 | notification (재결제 안내), subscription (3회 실패 시 정지) |

---

## 8. 비즈니스 규칙

1. **대여 기한**: 대여일로부터 30일
2. **연장**: 플랜별 횟수 제한, 1회 +15일
3. **미반납 자동 구매 전환**: 기한 +7일 경과 시 정가 자동 청구
4. **손상 과금**: 경미(무료), 중(정가 30%), 분실(정가 100%)
5. **해지 조건**: 대여 중인 아이템 전부 반납 완료 후 해지 확정
6. **일시정지**: 연 2회, 최대 30일/회, 정지 중 결제 안 됨
7. **정기 결제 실패**: 3일 간격 재시도, 3회 실패 시 구독 정지
8. **구매 전환 시**: 대여 슬롯 즉시 회복, 할인가 = 정가 × (1 - 플랜 할인율)

---

## 9. KPI

| 지표 | 목표 |
|------|------|
| 구독 전환율 (가입/방문) | 3% |
| 월 이탈률 (Churn) | < 8% |
| 구매 전환율 (대여→구매) | 25% |
| 아이템 회전율 (월) | 2.5회 |
| NPS | > 40 |

---

## 10. 추후 고도화 (V2)

- AI 기반 개인화 큐레이션 (체형, 스타일 선호도)
- 시즌 구독 (SS/FW 시즌별 자동 교체)
- 기업 구독 (B2B, 유니폼/드레스코드)
- 중고 판매 연동 (대여 종료 아이템 → 중고 마켓)
