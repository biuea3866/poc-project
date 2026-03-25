# 요구사항 Gap 분석: 결제/주문 시스템 리팩토링

> 요구사항: 결제 DB 전환 (MongoDB→MySQL) + 상품/주문/결제 분리 + Soft Delete 전환
> 분석일: 2026-03-20
> **상태: 전 항목 확정 (추천안 기반)**

---

## 0. AS-IS 현황 요약

### 현재 아키텍처

```
greeting_payment-server (Kotlin/Spring Boot)
├── MySQL: PlanOnGroup, CreditOnGroup, CardInfoOnGroup, payment_transaction
├── MongoDB: PaymentLogsOnGroup, MessagePointLogsOnWorkspace, MessagePointChargeLogsOnWorkspace
└── 외부: Toss Payments (빌링키 기반 구독 결제)

greeting_plan-data-processor (Node.js/NestJS)
├── Kafka Consumer: plan.changed 이벤트 수신
└── 플랜 다운그레이드 시 기능 제거 (평가양식, 커스텀 도메인 등)
```

### 현재 상품 구조

| 상품 | 저장소 | 과금 방식 | 비고 |
|------|--------|----------|------|
| Plan (Free/Basic/Standard/Business) | MySQL `PlanOnGroup` | 구독 (월/연) | Toss 빌링키 |
| SMS/LMS 포인트 | MySQL `CreditOnGroup` + MongoDB 로그 | 건별 소진 | 충전식 |
| AI 서류평가 (예정) | 없음 | 미정 | 신규 상품 |

### 현재 문제점

1. **상품과 결제가 하드코딩으로 결합**: Plan 결제 로직과 SMS 포인트 로직이 완전 분리, 공통 주문/결제 개념 없음
2. **DB 이원화**: MySQL + MongoDB 혼용으로 트랜잭션 일관성 보장 불가
3. **이력 관리 부재**: payment_transaction은 있으나, 주문/상품 변경 이력이 체계적이지 않음
4. **상품 추가 시 코드 변경 필수**: 새 상품(AI 크레딧 등) 추가하려면 별도 도메인 전체 구현 필요

---

## 1. 확정된 요구사항 해석

| # | 요구사항 | 확정 결정 | 근거 |
|---|---------|----------|------|
| A-1 | "다양한 상품을 주문해서 결제" | **단일 상품 즉시 결제 (장바구니 없음)** | SaaS B2B에서 장바구니는 불필요. 1주문=1상품 원칙 |
| A-2 | "상품과 주문거래를 분리" | **같은 서비스(payment-server) 내 모듈 분리** | 현재 규모(월 결제 수천 건)에서 마이크로서비스 분리는 오버엔지니어링 |
| A-3 | "필요시 이력데이터를 관리" | **결제/주문/구독 관련 테이블만 이력 관리** | 전체 테이블 이력은 비용 대비 효과 낮음. 상태 변경 이력 테이블로 관리 |
| A-4 | "AI 서류 평가 과금 모델" | **구독형 + 건단위 결제형 + 포인트 충전형 전부 지원** | 3가지 ProductType(SUBSCRIPTION, ONE_TIME, CONSUMABLE)으로 모든 과금 모델 대응. AI 평가를 구독 포함으로 팔 수도, 건별로 팔 수도, 크레딧 충전으로 팔 수도 있음 |
| A-5 | "MongoDB 데이터 마이그레이션" | **완전 이관 (MongoDB 의존성 제거)** | 이원 체계 유지 시 운영 복잡도가 영구적으로 남음 |
| A-6 | "결제 시스템 서비스 구조" | **payment-server 독립 유지 + 내부 리팩토링** | 결제 도메인은 독립성이 중요. greeting-new-back 통합 시 책임 범위가 과도하게 커짐 |

---

## 2. 확정된 누락 요구사항 대응

| # | 누락 항목 | 확정 대응 방안 | TDD 반영 위치 |
|---|----------|-------------|-------------|
| M-1 | **멱등성 보장** | `order.idempotency_key` + `payment.idempotency_key` UNIQUE 제약. PG 웹훅 수신 시 payment_key로 중복 체크 | 스키마: order, payment 테이블 |
| M-2 | **결제 보상 트랜잭션** | 보상 트랜잭션 패턴 채택. Order PAID → 상품 지급 실패 시 PaymentService.cancelPayment() 자동 호출. OrderStatusHistory에 보상 사유 기록 | 시퀀스: 결제 흐름 |
| M-3 | **구독 갱신 실패 정책** | 기존 정책 유지: 5회 재시도, 실패마다 만료일+1일 버퍼, 5회 초과 시 EXPIRED. `subscription.retry_count` 필드로 관리 | 스키마: subscription 테이블 |
| M-4 | **환불 정책** | 기존 프로레이션 유지: (남은일수/전체일수) × 결제금액. Order type=REFUND로 별도 주문 생성 | Order 도메인 |
| M-5 | **동시성 제어** | `order.version` + `credit_balance.version` Optimistic Lock. 구독 변경은 workspace_id 기반 분산락(Redis) | 스키마: version 필드 |
| M-6 | **빌링키 관리** | `billing_key` 테이블로 MySQL 이관. `is_primary` 플래그로 기본 결제 수단 관리. 카드 삭제 시 soft delete | 스키마: billing_key 테이블 |
| M-7 | **세금계산서/영수증** | `payment.receipt_url` 유지. 세금계산서 자동 발행은 **Out of Scope** (향후 별도 프로젝트) | Out of Scope |
| M-8 | **백오피스 수동 플랜 부여** | `ManualPaymentGateway` 구현체로 통합. Order type=NEW, payment_method=MANUAL, amount=0으로 동일 파이프라인 | Payment 도메인 |
| M-9 | **크레딧 만료 정책** | `credit_ledger.expired_at`으로 충전 건별 만료일 관리. 일별 스케줄러가 만료 건 자동 차감, `credit_balance` 갱신 | Credit 도메인 |
| M-10 | **데이터 마이그레이션** | 배치 기반 이관 + 검증 쿼리. 1) 스키마 생성 → 2) 데이터 이관 → 3) 건수/금액 검증 → 4) MongoDB 제거 | 마이그레이션 티켓 |

---

## 3. 확정된 추가 고려사항

| # | 고려사항 | 확정 대응 |
|---|---------|----------|
| C-1 | Toss Payments API 호환성 | `PaymentGateway` 인터페이스 + `TossPaymentGateway` 구현체. 기존 billingKey 흐름 그대로 유지 |
| C-2 | Kafka 이벤트 호환성 | 신규 이벤트(`order.event.v1` — OrderEvent 단일 이벤트, 터미널 상태에서만 발행) + 기존 `plan.changed` 레거시 어댑터 발행 병행 |
| C-3 | plan-data-processor 이관 | **greeting-new-back으로 이관 확정**. 플랜 다운그레이드 시 기능 제거는 ATS 도메인 데이터 소유자가 처리해야 함. payment-server는 이벤트 발행만, greeting-new-back이 소비하여 자체 데이터 정리 |
| C-4 | 결제 데이터 보관 의무 | 전 테이블 `deleted_at` soft delete. 물리 삭제 없음. 5년 보관 정책은 별도 아카이브 배치로 대응 |
| C-5 | 카드 정보 암호화 | 기존 encryption_key 기반 암호화 로직 그대로 `billing_key` 테이블에 적용 |
| C-6 | 환율/다국어 | `product_price.currency`, `order.currency` 필드 추가 (기본값 'KRW'). 실제 다통화는 Out of Scope |
| C-7 | 트랜잭션 경계 | MongoDB 제거로 단일 MySQL 트랜잭션 가능. Order+Payment+Subscription 상태 변경을 하나의 트랜잭션으로 |

---

## 4. 확정된 의사결정

| # | 질문 | 확정 결정 | 근거 |
|---|------|----------|------|
| Q-1 | 상품 카탈로그 관리 방식 | **하이브리드: ProductType은 enum, 가격/메타는 DB** | 상품 유형은 코드 레벨 안전성 확보, 가격 변경은 배포 없이 가능 |
| Q-2 | 주문 번호 체계 | **`ORD-{yyyyMMdd}-{UUID 8자리}`** (예: ORD-20260320-A1B2C3D4) | 날짜 기반 정렬 + UUID 유니크성. Toss orderId 호환 |
| Q-3 | MongoDB 마이그레이션 시점 | **4월: 스키마+이관 선행, 5월: 구조 개선과 동시에 MongoDB 완전 제거** | DB 전환을 먼저 완료해야 구조 개선이 안전 |
| Q-4 | payment-server 서비스 구조 | **독립 서비스 유지, 내부 리팩토링** | greeting-new-back은 이미 거대. 결제는 별도 배포/스케일링 필요 |
| Q-5 | AI 크레딧 과금 모델 | **3가지 모두 지원: 구독 포함 / 건별 결제 / 포인트 충전** | ProductType 3종으로 어떤 과금 모델이든 상품 등록만으로 대응 |
| Q-6 | Kafka 이벤트 스키마 변경 | **하위호환 유지 + 신규 v2 이벤트 병행 발행** | plan-data-processor 수정 최소화. 향후 v1 deprecation |
