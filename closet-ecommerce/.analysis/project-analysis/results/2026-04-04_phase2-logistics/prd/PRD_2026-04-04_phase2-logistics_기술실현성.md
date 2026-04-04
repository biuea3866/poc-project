# Phase 2 PRD 기술 실현성 분석

> 분석일: 2026-04-04
> PRD: Phase 2 상세 PRD (2026-03-22)
> 대상 도메인: 배송, 재고, 검색, 리뷰

---

## 1. 영향 서비스 식별

### 1.1 신규 생성 서비스 (4개)

| 서비스 | 모듈명 | 포트 | 현재 상태 | 비고 |
|--------|--------|------|----------|------|
| shipping-service | closet-shipping | 8084 | **빌드 아티팩트만 존재** (src 없음) | 도메인 클래스가 build/에서 확인됨 (ShipmentService, ReturnRequestService, Shipment, ReturnRequest, ShipmentStatusHistory) |
| inventory-service | closet-inventory | 8085 | **빌드 아티팩트만 존재** (src 없음) | 도메인 클래스 확인됨 (InventoryService, InventoryItem, InventoryTransaction, InventoryLockService, InventoryEventListener) |
| search-service | closet-search (미생성) | 8086 | **미존재** | 완전 신규 생성 필요 |
| review-service | closet-review (미생성) | 8087 | **미존재** | 완전 신규 생성 필요 |

**핵심 발견**: `closet-shipping`과 `closet-inventory`는 이전에 개발 이력이 있으나, `src/` 디렉토리가 누락되고 `build/` 아티팩트만 남아있다. `settings.gradle.kts`에도 포함되어 있지 않다. 소스 복구 또는 재개발이 필요하다.

### 1.2 수정 필요 서비스 (4개)

| 서비스 | 변경 내용 |
|--------|----------|
| **closet-order** | OrderStatus enum에 RETURN_REQUESTED, EXCHANGING 상태 추가, OrderItemStatus에 EXCHANGE_REQUESTED 추가, 구매확정 API, 반품/교환 연동 |
| **closet-payment** | PaymentStatus에 REFUNDED 상태 활용(이미 존재), 부분 환불 로직 추가 |
| **closet-bff** | ShippingServiceClient, InventoryServiceClient, SearchServiceClient, ReviewServiceClient 추가, 기존 OrderBffFacade 내 `shipment = null // Phase 2` 부분 실구현 |
| **closet-gateway** | 신규 서비스 4개 라우팅 추가 (shipping, inventory, search, review) |

### 1.3 간접 영향 서비스 (2개)

| 서비스 | 영향 |
|--------|------|
| **closet-member** | 리뷰 포인트 적립 이벤트 Consumer 추가 (`point.earn` Kafka 이벤트) |
| **closet-external-api** | 이미 Mock 택배사 API 구현 완료 (CJ, Epost, Logen, Lotte). shipping-service에서 호출하는 구조로 연동 |

---

## 2. 기존 코드 패턴 분석

### 2.1 OrderStatus enum 상태 전이 패턴

**현재 구현** (`closet-order/.../OrderStatus.kt`):
```
PENDING -> STOCK_RESERVED -> PAID -> PREPARING -> SHIPPED -> DELIVERED -> CONFIRMED
                                \-> CANCELLED        \-> CANCELLED (불가)
```

**Phase 2 필요 변경**:
- `DELIVERED -> RETURN_REQUESTED` 전이 추가 필요
- `DELIVERED -> EXCHANGE_REQUESTED` 전이 추가 필요
- `PREPARING` 상태에서의 `CANCELLED` 전이는 이미 허용됨 (PRD 요구사항 충족)
- `CONFIRMED` 이후 반품/교환 불가 (PRD AC와 일치 - 현재 `CONFIRMED -> false`)

**GAP 분석**:
현재 `OrderStatus.canTransitionTo()`에서 `SHIPPED -> DELIVERED`만 허용되고, `PAID -> SHIPPING` 전이는 정의되어 있지 않다. PRD에서는 `PAID -> SHIPPING`을 명시하지만, 기존 코드는 `PAID -> PREPARING -> SHIPPED` 흐름이다. **PRD의 `SHIPPING` 상태는 기존 코드의 `SHIPPED`에 매핑하되, 송장 등록 시 `PAID -> PREPARING`이 아닌 `PAID -> SHIPPED`로 전이하거나, PRD 문서를 코드에 맞춰 조정해야 한다.**

권장: `PAID -> PREPARING`(송장 등록) -> `SHIPPED`(택배사 발송 확인) 2단계 유지. PRD AC를 `PAID -> PREPARING -> SHIPPED`로 수정.

### 2.2 이벤트 발행 패턴

**현재**: Spring `ApplicationEventPublisher`를 사용한 **인프로세스 이벤트** 방식.
```kotlin
eventPublisher.publishEvent(OrderCreatedEvent(...))
eventPublisher.publishEvent(OrderCancelledEvent(...))
```

**Phase 2 요구**: Kafka 기반 **분산 이벤트** (`order.status.changed`, `order.confirmed`, `inventory.low_stock` 등).

**GAP**: 현재는 Kafka 의존성이 `closet-order/build.gradle.kts`에 있지만 실제 KafkaTemplate/KafkaListener 사용은 없다. ApplicationEventPublisher 이벤트만 사용 중이다.

**실현 방안**:
1. 기존 `ApplicationEventPublisher` 패턴은 유지하되, `@TransactionalEventListener` + Kafka Producer를 추가하여 **Transactional Outbox 패턴** 구현
2. inventory-service의 build 아티팩트에서 `InventoryEventListener`(kafka 패키지)가 확인됨 -- 이전에 Kafka Consumer가 구현되었던 흔적이 있으므로, 해당 패턴을 복원/참고

### 2.3 Facade 패턴 (BFF)

**현재 패턴**: `OrderBffFacade`가 `CompletableFuture` + Virtual Thread로 병렬 Feign 호출.
```kotlin
private val executor = Executors.newVirtualThreadPerTaskExecutor()

fun getOrderDetail(orderId: Long): OrderDetailBffResponse {
    val orderFuture = CompletableFuture.supplyAsync({ orderClient.getOrder(orderId) }, executor)
    val paymentFuture = CompletableFuture.supplyAsync({ paymentClient.getPaymentByOrderId(orderId) }, executor)
    // ...
    return OrderDetailBffResponse(order = ..., payment = ..., shipment = null) // Phase 2
}
```

**Phase 2 실현**: `ShipmentResponse` DTO는 이미 BFF에 placeholder로 정의됨 (`BffResponses.kt` line 217-221). `ShippingServiceClient`를 FeignClient로 추가하고, Facade에서 병렬 호출에 포함하면 된다. **구조적으로 매끄럽게 확장 가능**.

### 2.4 Controller -> Service 패턴

모든 서비스가 `@RestController -> @Service -> JPA Repository` 계층을 따름. 엔티티에 비즈니스 로직 캡슐화 (DDD Lite). 이 패턴을 신규 서비스에도 동일하게 적용.

---

## 3. DB 스키마 변경 분석

### 3.1 신규 테이블 (10개)

| 서비스 | 테이블 | Flyway 파일 | 비고 |
|--------|--------|------------|------|
| shipping | `shipping` | V1__init_shipping.sql (신규) | order_id UNIQUE KEY |
| shipping | `shipping_tracking_log` | V1__init_shipping.sql | shipping_id FK(논리적) |
| shipping | `return_request` | V2__add_return_exchange.sql (신규) | 반품 요청 |
| shipping | `exchange_request` | V2__add_return_exchange.sql | 교환 요청 |
| inventory | `inventory` | V1__init_inventory.sql (신규) | SKU UNIQUE, product+option UNIQUE |
| inventory | `inventory_history` | V1__init_inventory.sql | 변경 이력 |
| inventory | `restock_notification` | V2__add_restock_notification.sql (신규) | 재입고 알림 |
| review | `review` | V1__init_review.sql (신규) | order_item_id UNIQUE |
| review | `review_image` | V1__init_review.sql | S3 URL 저장 |
| review | `review_size_info` | V1__init_review.sql | review_id UNIQUE |
| review | `review_summary` | V1__init_review.sql | product_id UNIQUE, 집계 데이터 |

### 3.2 기존 테이블 변경

| 서비스 | 테이블 | 변경 | Flyway 파일 |
|--------|--------|------|------------|
| inventory | `inventory` | `version BIGINT NOT NULL DEFAULT 0` 컬럼 추가 | V3__add_optimistic_lock.sql |
| order | `orders` | 직접 스키마 변경 불필요. OrderStatus enum 값 추가는 VARCHAR 컬럼이므로 DDL 변경 없음 | - |
| order | `order_item` | 직접 스키마 변경 불필요. OrderItemStatus enum 값 추가도 VARCHAR이므로 DDL 변경 없음 | - |

### 3.3 컨벤션 준수 확인

- FK 미사용: PRD 스키마가 FK 미사용 -- **준수** (INDEX만 사용)
- JSON/ENUM 미사용: VARCHAR로 상태 저장 -- **준수**
- TINYINT(1) for boolean: `review.has_photo TINYINT(1)` -- **준수**
- DATETIME(6): 모든 시간 컬럼 -- **준수**
- COMMENT 필수: 모든 컬럼에 COMMENT -- **준수**

### 3.4 주의사항

- **Elasticsearch 인덱스**: `closet-products` 인덱스는 Flyway가 아닌 별도 인덱스 매핑 관리 필요. nori 분석기 플러그인 설치 필수.
- **각 서비스 별도 DB**: 현재 모든 서비스가 `closet` 단일 DB를 공유하고 있음(`jdbc:mysql://localhost:3306/closet`). Phase 2에서도 동일 DB 사용 시 Flyway `table` 설정을 서비스별로 분리해야 함 (예: `flyway_schema_history_shipping`).

---

## 4. Kafka 이벤트 분석

### 4.1 신규 토픽

| 토픽 | Producer | Consumer | 파티션 권장 | 비고 |
|------|----------|----------|-----------|------|
| `order.status.changed` | shipping-service | order-service | 6 (orderId 파티션 키) | 송장등록/배송완료 시 |
| `order.confirmed` | order-service | shipping-service | 6 | 구매확정 시 |
| `order.created` | order-service | inventory-service | 6 | 재고 차감 (현재 ApplicationEvent -> Kafka 전환 필요) |
| `order.cancelled` | order-service | inventory-service | 6 | 재고 복구 |
| `return.approved` | shipping-service | inventory-service | 6 | 반품 승인 시 재고 복구 |
| `inventory.insufficient` | inventory-service | order-service | 3 | 재고 부족 시 주문 거절 |
| `inventory.low_stock` | inventory-service | (알림 서비스) | 3 | 안전재고 이하 |
| `inventory.out_of_stock` | inventory-service | (알림 서비스) | 3 | 재고 0 |
| `inventory.restock_notification` | inventory-service | (알림 서비스) | 3 | 재입고 알림 |
| `product.created` | product-service | search-service | 6 | 인덱싱 |
| `product.updated` | product-service | search-service | 6 | 인덱스 업데이트 |
| `product.deleted` | product-service | search-service | 3 | 인덱스 삭제 |
| `review.created` | review-service | member-service, search-service | 6 | 포인트 적립 + ES 동기화 |
| `review.summary.updated` | review-service | search-service | 3 | 별점/리뷰수 ES 동기화 |
| `point.earn` | review-service | member-service | 6 | 포인트 적립 |

### 4.2 Consumer 그룹

| Consumer Group | 서비스 | 구독 토픽 |
|----------------|--------|----------|
| `inventory-order-consumer` | inventory-service | order.created, order.cancelled |
| `inventory-return-consumer` | inventory-service | return.approved |
| `order-shipping-consumer` | order-service | order.status.changed, inventory.insufficient |
| `search-product-consumer` | search-service | product.created, product.updated, product.deleted |
| `search-review-consumer` | search-service | review.summary.updated |
| `member-review-consumer` | member-service | review.created, point.earn |

### 4.3 Outbox 패턴 적용 범위

**필수 적용 대상** (데이터 정합성 필수):
1. `order.created` / `order.cancelled`: 재고 차감/복구와 주문 상태가 반드시 일치해야 함
2. `return.approved`: 환불 + 재고 복구가 원자적이어야 함

**권장 적용 대상**:
3. `order.status.changed`: 배송 상태와 주문 상태 동기화

**불필요** (최종 일관성 허용):
4. `review.created`, `review.summary.updated`: 검색 인덱스 동기화는 지연 허용
5. `inventory.low_stock`, `inventory.restock_notification`: 알림은 지연/누락 허용

### 4.4 순서 보장 전략

- **orderId를 Kafka 파티션 키로 사용**: 동일 주문의 이벤트가 동일 파티션에 들어가므로 순서 보장
- **SKU를 파티션 키로 사용 (재고)**: 동일 SKU 재고 변경 이벤트 순서 보장
- DLQ(Dead Letter Queue) 설정: 처리 실패 이벤트를 `.dlq` 토픽으로 라우팅

---

## 5. 인증/인가 분석

### 5.1 API Gateway 라우팅 추가

현재 `closet-gateway/application.yml`에 5개 서비스 라우팅 정의. Phase 2에서 추가 필요:

```yaml
# 추가 필요 라우팅
- id: shipping-service
  uri: http://localhost:8084  # 포트 충돌! 현재 payment-service가 8084
  predicates:
    - Path=/api/v1/shippings/**, /api/v1/returns/**, /api/v1/exchanges/**

- id: inventory-service
  uri: http://localhost:8085  # 포트 충돌! 현재 bff-service가 8085
  predicates:
    - Path=/api/v1/inventories/**, /api/v1/restock-notifications/**

- id: search-service
  uri: http://localhost:8086
  predicates:
    - Path=/api/v1/search/**

- id: review-service
  uri: http://localhost:8087
  predicates:
    - Path=/api/v1/reviews/**
```

### 5.2 포트 충돌 이슈

| PRD 지정 포트 | PRD 서비스 | 기존 서비스 | 충돌 |
|-------------|-----------|-----------|------|
| 8084 | shipping-service | payment-service (8084) | **충돌** |
| 8085 | inventory-service | bff-service (8085) | **충돌** |
| 8086 | search-service | - | 사용 가능 |
| 8087 | review-service | - | 사용 가능 |

**해결 방안**: shipping-service를 8088, inventory-service를 8089로 변경하거나, PRD 포트를 8090~8093으로 재배정.

### 5.3 판매자 전용 API 인가

PRD에서 판매자 전용 API:
- `POST /api/v1/shippings` (송장 등록) -- 판매자만 호출 가능
- `PATCH /api/v1/inventories/{id}/safety-stock` -- 판매자만 호출 가능
- 반품 승인/거절 -- 판매자만

현재 Gateway에는 JWT 검증만 있고, **역할 기반 인가(RBAC)가 없다**. 판매자/구매자 구분을 위해:
1. JWT에 `role` claim 추가 (member-service 수정)
2. Gateway에 `RoleAuthorizationFilter` 추가
3. 또는 각 서비스에서 `X-Member-Role` 헤더 기반 인가 처리

**리스크**: RBAC 인프라가 아직 없으므로 Phase 2 초기에 인가 기반 구축이 선행되어야 함.

---

## 6. 기술 리스크 분석

### 6.1 동시성 제어 (재고) -- 리스크: **중**

**PRD 요구**: Redis 분산 락(Redisson) + JPA @Version 낙관적 락 이중 잠금.

**현재 상태**:
- `closet-inventory` build 아티팩트에서 `InventoryLockService` 클래스가 확인됨 (Redis 기반 분산 락 이전 구현 존재)
- `closet-order/build.gradle.kts`에 `spring-boot-starter-data-redis`, `spring-kafka` 의존성 이미 존재
- 그러나 src 코드가 없으므로 재구현 필요

**기술적 고려사항**:
- Redisson vs Lettuce: Redisson이 분산 락 API가 더 풍부 (RLock, tryLock with waitTime/leaseTime)
- 락 키 설계: `inventory:lock:{sku}` -- SKU 단위 세밀한 잠금으로 병렬성 확보
- Watch Dog: Redisson의 Lock Watchdog이 락 갱신을 자동 처리하므로 leaseTime=-1 권장
- 이중 잠금의 롤백 순서: DB 트랜잭션 실패 시 Redis 락 해제가 보장되어야 함 -> try-finally 필수

**실현 가능성**: **높음**. 이전 구현 흔적이 있고 일반적인 패턴임. 단, 100 스레드 동시성 테스트를 위한 Testcontainers Redis 설정 필요.

### 6.2 택배사 API 장애 대응 -- 리스크: **낮음**

**현재 상태**: `closet-external-api`에 Mock 택배사 API(CJ, Epost, Logen, Lotte)가 이미 구현됨. CarrierService가 등록/추적/상태진행 기능을 제공.

**PRD 요구**: 택배사 API 장애 시 마지막 캐싱된 정보 반환 + 에러 로깅.

**실현 방안**:
1. shipping-service에서 외부 택배사 API 호출 시 Circuit Breaker(Resilience4j) 적용
2. Redis 캐시(5분 TTL)에 추적 정보 저장 -- 장애 시 캐시 fallback
3. Mock API 환경에서는 장애 시뮬레이션을 위한 `/chaos` 엔드포인트 추가 고려

**실현 가능성**: **높음**. Mock API 기반이므로 장애 시나리오 테스트가 용이.

### 6.3 Kafka 순서 보장 -- 리스크: **중**

**이슈**: 하나의 주문에 대해 `order.created` -> `order.status.changed` (배송중) -> `order.status.changed` (배송완료) -> `order.confirmed` 순서가 보장되어야 함.

**해결**:
- orderId를 파티션 키로 사용하여 동일 주문 이벤트가 같은 파티션에 적재
- Consumer에서 idempotent 처리 (중복 이벤트 방어)
- 이벤트에 `eventTimestamp` + `sequenceNumber` 포함

**주의**: Consumer가 여러 파티션을 처리할 때, 서로 다른 주문 간에는 순서 보장이 불필요하므로 파티션 수를 늘려 처리량 확보 가능.

### 6.4 Elasticsearch 운영 -- 리스크: **중상**

**새로운 기술 스택 도입**: 프로젝트에 Elasticsearch가 아직 없음.

**필요 작업**:
1. Docker Compose에 Elasticsearch 8.x + nori 플러그인 추가
2. Spring Data Elasticsearch 또는 RestHighLevelClient 의존성 추가
3. 인덱스 매핑 관리 (ILM, 재인덱싱 전략)
4. nori 토크나이저 + edge_ngram 자동완성 분석기 설정
5. Testcontainers Elasticsearch 설정

**리스크 요인**:
- nori 형태소 분석기의 한글 검색 품질 튜닝 (사전 관리, 유의어 설정)
- Elasticsearch 클러스터 운영 경험 필요 (샤드/레플리카, 메모리 설정)
- 인덱싱 지연 3초 이내 목표 -- Kafka Consumer의 bulk indexing 최적화 필요

**실현 가능성**: **중간**. 기술적으로 가능하나 튜닝에 시간 소요. Sprint 7 전체(2주)를 검색에 할당한 것은 적절.

### 6.5 이미지 업로드/리사이즈 -- 리스크: **낮음**

**PRD 요구**: S3 업로드 + 400x400 썸네일 리사이즈.

**실현 방안**:
1. Spring Multipart + AWS SDK S3 Client
2. 썸네일: 서비스 내 `java.awt.image.BufferedImage` 또는 Thumbnailator 라이브러리
3. 비동기 처리: `@Async` 또는 별도 이벤트로 썸네일 생성

**실현 가능성**: **높음**. 표준적인 패턴.

### 6.6 자동 구매확정 배치 -- 리스크: **낮음**

**PRD 요구**: Spring Scheduler로 매일 00:00에 배송완료 7일 경과 건 자동 CONFIRMED.

**주의사항**:
- 반품/교환 진행 중인 건 제외 조건 필요
- 대량 처리 시 페이지네이션 (chunk 단위)
- 배치 처리 중 장애 시 재시작 가능하도록 idempotent 처리

**실현 가능성**: **높음**.

---

## 7. WMS(배송/물류) 실현성 분석

### 7.1 배송 워크플로우

```
[판매자]                    [shipping-service]           [external-api]          [order-service]
   │                              │                          │                       │
   ├── 송장등록 ────────────────>│                          │                       │
   │                              ├── 배송정보 저장           │                       │
   │                              ├── 택배사 API 호출 ──────>│                       │
   │                              │<── trackingNumber ───────│                       │
   │                              ├── Kafka: order.status.changed ────────────────>│
   │                              │                          │              (PAID->PREPARING)
   │                              │                          │                       │
   [스케줄러]                      │                          │                       │
   │── 배송추적 polling ─────────>│                          │                       │
   │                              ├── 택배사 추적 API ──────>│                       │
   │                              │<── tracking logs ────────│                       │
   │                              ├── tracking_log 저장      │                       │
   │                              ├── Redis 캐시 갱신        │                       │
   │                              │                          │                       │
   │── (배송완료 감지) ──────────>│                          │                       │
   │                              ├── Kafka: order.status.changed ────────────────>│
   │                              │                          │              (SHIPPED->DELIVERED)
```

### 7.2 반품 워크플로우

```
상태 흐름: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> INSPECTING -> APPROVED/REJECTED

[구매자]                    [shipping-service]           [payment-service]     [inventory-service]
   │                              │                          │                       │
   ├── 반품신청 ────────────────>│                          │                       │
   │                              ├── return_request 저장    │                       │
   │                              ├── 배송비 계산            │                       │
   │                              │  (CHANGE_OF_MIND: 3000)  │                       │
   │                              │  (DEFECTIVE: 0)          │                       │
   │                              │                          │                       │
   [판매자]                       │                          │                       │
   │── 수거예약 ────────────────>│                          │                       │
   │                              ├── PICKUP_SCHEDULED       │                       │
   │── 수거완료 ────────────────>│                          │                       │
   │                              ├── PICKUP_COMPLETED       │                       │
   │── 검수시작 ────────────────>│                          │                       │
   │                              ├── INSPECTING             │                       │
   │── 반품승인 ────────────────>│                          │                       │
   │                              ├── APPROVED               │                       │
   │                              ├── 결제 취소 API ────────>│                       │
   │                              │<── 환불 완료 ────────────│                       │
   │                              ├── Kafka: return.approved ──────────────────────>│
   │                              │                          │           (재고 복구)  │
```

### 7.3 교환 워크플로우

```
상태 흐름: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> RESHIPPING -> COMPLETED

[구매자]                    [shipping-service]           [inventory-service]
   │                              │                          │
   ├── 교환신청 ────────────────>│                          │
   │  (exchangeOptionId 포함)     │                          │
   │                              ├── 재고 확인 API ────────>│
   │                              │<── 재고 있음 ────────────│
   │                              ├── 새 옵션 재고 선점 ────>│  (reserveStock)
   │                              ├── exchange_request 저장  │
   │                              │                          │
   [판매자]                       │                          │
   │── 수거완료 ────────────────>│                          │
   │                              ├── 기존 옵션 재고 복구 ──>│  (releaseStock)
   │── 재발송 ──────────────────>│                          │
   │                              ├── RESHIPPING             │
   │── 교환완료 ────────────────>│                          │
   │                              ├── COMPLETED              │
```

### 7.4 WMS 기술적 구현 방식

**현재 PRD 범위**: 풀 WMS가 아닌 **배송 추적 + 반품/교환 관리** 수준. 입고/피킹/출고 같은 창고 관리는 Phase 2 범위에 포함되지 않음.

**Phase 2 범위 내 구현 방식**:

| 기능 | 구현 방식 |
|------|----------|
| 송장 등록 | REST API + DB 저장 + Kafka 이벤트 |
| 배송 추적 | 스케줄러 polling (또는 택배사 Webhook) + Redis 캐시 |
| 자동 구매확정 | Spring @Scheduled + DB 조회 + Kafka 이벤트 |
| 반품 신청/처리 | REST API + 상태 머신 + Kafka 이벤트 + 결제 취소 연동 |
| 교환 신청/처리 | REST API + 상태 머신 + 재고 선점/복구 연동 |

**미포함 (Phase 3+ 후보)**:
- 입고/피킹/출고 워크플로우 (WMS)
- 묶음 배송 관리
- 창고 위치 관리
- 바코드/QR 스캔

---

## 8. 종합 실현성 평가

### 8.1 도메인별 실현성

| 도메인 | 난이도 | 실현성 | 소요 예상 | 비고 |
|--------|--------|--------|----------|------|
| **배송** (US-501~505) | **중** | **높음** | 2 Sprint | Mock API 이미 있음. 반품/교환 상태머신이 복잡도 높음 |
| **재고** (US-601~604) | **중상** | **높음** | 1.5 Sprint | 분산 락 구현이 핵심. 이전 구현 아티팩트 참고 가능 |
| **검색** (US-701~705) | **상** | **중간** | 2 Sprint | Elasticsearch 신규 도입. nori 튜닝 + 자동완성 구현 필요 |
| **리뷰** (US-801~804) | **중** | **높음** | 1.5 Sprint | 표준적인 CRUD + 이미지 업로드 + 집계 |

### 8.2 선행 과제 (Phase 2 시작 전)

1. **closet-shipping, closet-inventory src 복구 또는 재개발 결정**
   - build 아티팩트에서 구조 파악은 가능하나, src 복원이 불가하면 재개발
   - `settings.gradle.kts`에 모듈 추가 필요

2. **포트 재배정**
   - PRD 8084(shipping), 8085(inventory)가 기존 서비스와 충돌
   - payment: 8084 -> 유지, shipping: 8088, inventory: 8089로 변경 권장

3. **Kafka 인프라 구축**
   - Docker Compose에 Kafka/Zookeeper 추가
   - Spring Kafka 설정 표준화 (serializer, consumer group, error handling)

4. **ApplicationEvent -> Kafka 전환 (order-service)**
   - 현재 인프로세스 이벤트를 Kafka Producer로 전환
   - Transactional Outbox 패턴 도입

5. **RBAC 기반 인가 체계**
   - 판매자/구매자 역할 구분
   - JWT claim에 role 추가 또는 별도 인가 서비스

6. **Elasticsearch 환경 구축** (Sprint 7 시작 전까지)
   - Docker Compose에 ES + nori 플러그인
   - Testcontainers ES 설정

### 8.3 마일스톤 검증

PRD 마일스톤은 8주(Sprint 5~8)로 계획됨:

| Sprint | PRD 계획 | 실현성 평가 |
|--------|---------|-----------|
| Sprint 5 (Week 1-2) | 재고(US-601,602) + 송장등록(US-501) | **적절**. 단, 모듈 셋업 + Kafka 인프라 구축 시간 포함 필요. 첫 주는 인프라에 할애 가능성 높음 |
| Sprint 6 (Week 3-4) | 배송추적/반품/교환(US-502~505) + 재고알림(US-603,604) | **타이트**. 반품+교환 상태머신이 복잡. 교환의 재고 선점/복구 연동이 까다로움. 스프린트 오버 가능성 있음 |
| Sprint 7 (Week 5-6) | 검색(US-701~705) | **적절**. ES 환경 구축 + nori 설정 + 5개 US. 단, 유의어/오타교정은 MVP에서 제외하고 후속 개선 권장 |
| Sprint 8 (Week 7-8) | 리뷰(US-801~804) + 통합 테스트 | **타이트**. 이미지 업로드 + S3 + 포인트 연동 + 집계 + ES 동기화 + 통합테스트까지. 통합테스트를 별도 버퍼로 분리 권장 |

### 8.4 최종 판정

**Phase 2 PRD는 기술적으로 실현 가능하다.** 다만 아래 조건이 충족되어야 한다:

1. **closet-shipping / closet-inventory 소스 복원 또는 재개발 결정을 Sprint 5 시작 전에 완료**
2. **Kafka + Elasticsearch 인프라 셋업을 Sprint 5 첫 주에 병행**
3. **Sprint 6의 범위가 가장 위험** -- 반품/교환을 Sprint 7로 일부 이동하고, 검색의 고급 기능(유의어, 오타교정)을 Phase 3로 이관하는 것을 권장
4. **통합 테스트 버퍼(1주)를 별도 확보** -- Sprint 8 이후 또는 Sprint 8을 리뷰(1주) + 통합테스트(1주)로 분리

---

## 부록: 참조 코드 경로

| 파일 | 분석 포인트 |
|------|------------|
| `closet-order/.../OrderStatus.kt` | 상태 전이 규칙 (canTransitionTo 패턴) |
| `closet-order/.../OrderEvent.kt` | 이벤트 발행 패턴 (ApplicationEventPublisher) |
| `closet-order/.../Order.kt` | 엔티티 비즈니스 로직 캡슐화 패턴 |
| `closet-order/.../OrderService.kt` | Service 계층 패턴 (얇은 서비스) |
| `closet-payment/.../Payment.kt` | PaymentStatus.REFUNDED 이미 정의됨 |
| `closet-external-api/.../CarrierService.kt` | Mock 택배사 API 패턴 |
| `closet-external-api/.../MockShipment.kt` | 배송 추적 이력 모델 |
| `closet-bff/.../OrderBffFacade.kt` | BFF Facade + Virtual Thread 병렬 호출 패턴 |
| `closet-bff/.../BffResponses.kt` | Phase 2 placeholder (ShipmentResponse, ReviewSummaryResponse) |
| `closet-gateway/.../application.yml` | 라우팅 설정 + 포트 매핑 |
| `settings.gradle.kts` | 모듈 포함 목록 (shipping/inventory 미포함) |
