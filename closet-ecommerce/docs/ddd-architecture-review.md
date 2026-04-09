# Closet 의류 이커머스 — 멀티 모듈 아키텍처 리뷰 보고서

**작성일**: 2026-04-06
**검토자**: BE Senior / Tech Leader / Architect (3인 합동 리뷰)

---

## 1. 현재 모듈 구조 요약 (12개)

| 모듈 | 유형 | 의존 대상 | 인프라 |
|------|------|-----------|--------|
| **closet-common** | 공유 라이브러리 | 없음 | JPA, Redis, Kafka, S3, AOP |
| **closet-gateway** | 인프라 | 없음 (독립) | WebFlux, Spring Cloud Gateway |
| **closet-member** | 도메인 서비스 | closet-common | Redis, Kafka, JWT |
| **closet-product** | 도메인 서비스 | closet-common | Redis, Kafka |
| **closet-order** | 도메인 서비스 | closet-common | Redis, Kafka |
| **closet-payment** | 도메인 서비스 | closet-common | Redis, Kafka |
| **closet-inventory** | 도메인 서비스 | closet-common | Redis, Redisson, Kafka |
| **closet-shipping** | 도메인 서비스 | closet-common | Redis, Redisson, Kafka |
| **closet-search** | 도메인 서비스 | closet-common | Elasticsearch, Redis, Kafka |
| **closet-review** | 도메인 서비스 | closet-common | Redis, Kafka, Thumbnailator |
| **closet-bff** | 오케스트레이션 | closet-common | OpenFeign |
| **closet-external-api** | 외부 Mock | 없음 | Web only |

### 의존성 그래프

```
closet-gateway  (독립)
closet-external-api  (독립)

closet-common  <──  closet-member
               <──  closet-product
               <──  closet-order
               <──  closet-payment
               <──  closet-inventory
               <──  closet-shipping
               <──  closet-search
               <──  closet-review
               <──  closet-bff
```

**평가**: 모든 도메인 모듈이 closet-common만 의존, 모듈 간 직접 의존 없음. 매우 건전.

---

## 2. 바운디드 컨텍스트 매핑

| 모듈 | BC 대응 | 응집도 | 판정 |
|------|---------|--------|------|
| closet-member | Identity & Access + Loyalty | 혼합 주의 | **주의** |
| closet-product | Product Catalog | 양호 | **적합** |
| closet-order | Order Management | 양호 | **적합** |
| closet-payment | Payment | 양호 | **적합** |
| closet-inventory | Inventory | 양호 | **적합** |
| closet-shipping | Fulfillment (배송+반품+교환) | 비대화 주의 | **주의** |
| closet-search | Search & Discovery | 양호 | **적합** |
| closet-review | Review & Rating | 양호 | **적합** |
| closet-bff | BFF (조합 계층) | 양호 | **적합** |
| closet-common | Shared Kernel | 혼합 주의 | **주의** |

---

## 3. 컨텍스트 맵 (Kafka 이벤트 흐름)

```
┌────────────────────────────────────────────────────────────────────┐
│  order ──event.closet.order──> inventory (예약/해제)              │
│  order ──event.closet.order──> shipping  (배송 준비)             │
│  order ──event.closet.order──> review    (리뷰 가능 기록)        │
│                                                                    │
│  product ──event.closet.product──> search (ES 인덱싱)            │
│  review  ──event.closet.review──>  member (포인트 적립)          │
│  review  ──event.closet.review──>  search (avgRating 갱신)       │
│                                                                    │
│  shipping ──event.closet.shipping──> payment   (부분 환불)       │
│  shipping ──event.closet.shipping──> inventory  (재고 복구)      │
│  shipping ──event.closet.order(!)──> order      **도메인 침범**  │
│                                                                    │
│  bff ──HTTP (OpenFeign)──> 전체 서비스 (조합/오케스트레이션)     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 4. 문제점 목록

### HIGH 심각도

#### H-1. FitType 유비쿼터스 언어 충돌
- `closet-product/FitType`: OVERSIZED, REGULAR, SLIM (상품 핏 유형)
- `closet-review/FitType`: SMALL, PERFECT, LARGE (구매자 사이즈 체감)
- **권장**: Review의 FitType → `SizeFit` 이름 변경

#### H-2. Shipping 모듈의 Order 도메인 침범
- `closet-shipping/OrderController` + `confirmOrder()` + `AutoConfirmScheduler`
- Shipping이 `event.closet.order` 토픽에 `OrderConfirmed` 이벤트 발행
- **권장**: Shipping은 `event.closet.shipping`에 `DeliveryConfirmed` 발행, Order가 수신하여 자체 상태 전이

#### H-3. BaseEntity의 LocalDateTime 사용
- 컨벤션(ZonedDateTime 필수) 위반
- 전체 엔티티에 전파 (Product, Member, Order, Payment 등)

### MEDIUM 심각도

#### M-1. Consumer Facade 경유 불일치
- 위반: `ShippingStatusConsumer`(order), `ReturnApprovedConsumer`(payment) → Repository 직접 호출
- 준수: `InventoryFacade` 경유, `PointService` 경유

#### M-2. Outbox 발행 패턴 불일치
- Product/Order: Spring Event → Listener → Outbox
- Shipping: OutboxEventPublisher 직접 호출

#### M-3. common Test Fixture Map 기반 동기화 문제

---

## 5. 개선 제안 (우선순위별)

### Priority 1: 즉시 해결
1. **FitType → SizeFit 이름 변경** (Review 모듈)
2. **구매확정 이벤트 흐름 교정** (Shipping → DeliveryConfirmed, Order가 수신)
3. **Consumer Facade 경유 일관성** (ShippingStatusConsumer, ReturnApprovedConsumer)

### Priority 2: 단기 개선
1. BaseEntity ZonedDateTime 마이그레이션
2. Test Fixture 각 모듈로 이전
3. Outbox 발행 패턴 통일

### Priority 3: Phase 3 진입 전
1. closet-common 분리 검토 (closet-core + closet-infrastructure)
2. build 디렉토리 Phase 3 아티팩트 정리

---

## 6. Phase 3 확장 권장 구조

### CS vs Shipping 경계 정의

```
CS BC = 반품/교환 "접수" + 1:1 문의 + FAQ
Shipping BC = 반품 "물류" + 교환 "물류" (접수 이후)

이벤트 흐름:
  CS: ReturnRequested(접수) → Shipping: 수거 물류 시작
  Shipping: ReturnApproved → Payment: 환불
  Shipping: ReturnApproved → Inventory: 재고 복구
```

### Promotion 통합
- **BFF 오케스트레이션 방식 권장**: Order가 Promotion을 모르게
- BFF에서 쿠폰 적용 가능 여부 확인 후 할인 금액을 Order에 전달

### 추가 Kafka 토픽
```
event.closet.promotion    (쿠폰 발행/사용/만료)
event.closet.cs           (문의 접수/답변/반품접수)
event.closet.settlement   (정산 확정/지급)
event.closet.seller       (입점 승인/상품 등록)
```

---

## 7. 종합 평가

| 평가 항목 | 점수 (5점) | 근거 |
|----------|-----------|------|
| 모듈 분리 적절성 | 4.0 | 1모듈=1BC 잘 준수, Shipping 비대화만 주의 |
| 의존성 방향 | 4.5 | 모듈 간 직접 의존 없음, 매우 건전 |
| 이벤트 기반 통합 | 4.0 | Outbox+Idempotency 우수, 패턴 불일치 감점 |
| 유비쿼터스 언어 | 3.5 | FitType 충돌, OrderConfirmed 침범 |
| 확장 준비도 | 4.0 | Phase 3 자연스럽게 추가 가능 |

---

## 8. BC 재설계 실행 결과 (2026-04-09)

### 해결된 문제

| 문제 | 해결 | PR |
|------|------|-----|
| **H-1. FitType 충돌** | Review의 `FitType` → `SizeFit`으로 rename (14개 파일) | feature/ddd-bc-restructuring |
| **H-2. Shipping 도메인 침범** | `OrderShippingStarted`/`OrderConfirmed` → `ShippingStarted`/`DeliveryConfirmed`로 변경, topic ORDER → SHIPPING | feature/ddd-bc-restructuring |
| **Point BC 오배치** | Point 도메인을 closet-promotion → closet-member로 이동 (19파일 이동, GradeType 삭제) | feature/ddd-bc-restructuring |
| **Filter 안티패턴** | SafetyStockService `findAll().filter{}` OOM 위험 제거, PointPolicyService/DiscountPolicyService QueryDSL 전환 | feature/ddd-bc-restructuring |
| **PaymentGateway dead code** | Port-Adapter 패턴 구현 (TossPaymentGateway + Factory), Service에서 Gateway 호출 후 DB 업데이트 | feature/ddd-bc-restructuring |

### 변경된 BC 매핑

| 모듈 | BC 대응 | 변경사항 |
|------|---------|---------|
| closet-member | Identity & Access + Loyalty + **Point(적립금)** | Point 도메인 편입, GradeType→MemberGrade 통합 |
| closet-promotion | **Coupon + TimeSale + Discount** (순수 프로모션만) | Point 제거, 프로모션 전용으로 응집도 향상 |
| closet-review | Review & Rating | FitType → SizeFit rename |
| closet-fulfillment | Fulfillment (배송+CS) | ShippingStarted/DeliveryConfirmed 이벤트로 교정 |
| closet-payment | Payment | PaymentGateway + Factory 패턴 추가 |

### 수정된 이벤트 흐름

```
shipping ──event.closet.shipping──> order (ShippingStarted → PREPARING)
shipping ──event.closet.shipping──> order (DeliveryConfirmed → CONFIRMED)
shipping ──event.closet.shipping──> payment (ReturnApproved → 환불)
```

`shipping → event.closet.order` 침범 완전 제거.

### Notification BC 구현 (2026-04-09)

| 컴포넌트 | 패턴 | 설명 |
|---------|------|------|
| NotificationSender | Strategy | EMAIL/SMS/PUSH 채널별 발송 전략 인터페이스 |
| NotificationDispatcher | Factory | CarrierAdapterFactory 동일 패턴, 채널별 Sender 라우팅 |
| NotificationPreference | Entity | 회원별 채널 수신동의, 마케팅 동의, 야간 알림(DND) 설정 |
| NotificationTopicSubscription | Entity | 상품/카테고리/브랜드/이벤트별 토픽 구독 |
| RestockEventConsumer | Kafka | event.closet.inventory → 재입고 알림 발송 |
| NotificationFacade | Facade | Consumer → Facade → Service 아키텍처 준수 |

이벤트 흐름 추가:
```
inventory ──event.closet.inventory──> notification (재입고 알림)
```
