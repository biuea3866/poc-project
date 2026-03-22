# 성능 개선 계획

> 작성일: 2026-03-23
> 작성자: BE Architecture Team
> 상태: Draft

---

## 1. 현재 성능 기준선

### 1.1 k6 부하 테스트 환경

기존 `load-test/realistic-traffic.js`에 15가지 유저 행동 패턴 시뮬레이터가 구현되어 있다.

- 총 VU: ~75 (상시)
- 시나리오 분포: 윈도우 쇼퍼 20%, 비교 쇼퍼 11%, 충동 구매자 7%, 장바구니 모아두기 11%, 신규 회원 7%, 단골 구매자 7%, 반복 방문자 3%, 대량 구매자 3%, 셀러 입점 4%, 리뷰 작성자 4%, 배송 관리자 3%, 검색 유저 7%, 재고 관리자 3%, 주문 취소 3%, BFF 통합 4%
- 커스텀 메트릭: page_views, searches, cart_abandoned, orders_completed, revenue_total, error_rate, order_flow_duration

### 1.2 예상 성능 목표치

현재 성능 측정이 미흡하므로 아래 목표를 기준으로 개선한다.

| API 유형 | 목표 P50 | 목표 P95 | 목표 P99 | 에러율 |
|----------|---------|---------|---------|--------|
| 상품 목록 조회 | < 100ms | < 300ms | < 500ms | < 0.1% |
| 상품 상세 조회 | < 50ms | < 150ms | < 300ms | < 0.1% |
| 검색 (키워드) | < 100ms | < 300ms | < 500ms | < 0.1% |
| 주문 생성 | < 200ms | < 500ms | < 1000ms | < 0.5% |
| 결제 승인 | < 500ms | < 1000ms | < 3000ms | < 1.0% |
| 장바구니 추가 | < 50ms | < 100ms | < 200ms | < 0.1% |
| 메인 페이지 (BFF) | < 200ms | < 500ms | < 800ms | < 0.1% |
| 마이페이지 (BFF) | < 150ms | < 400ms | < 600ms | < 0.1% |

### 1.3 SLO (Service Level Objective)

| 지표 | 목표 | 측정 기간 |
|------|------|----------|
| 가용성 | 99.9% (월 43분 다운타임) | 월간 |
| P95 응답 시간 | < 500ms (읽기), < 1s (쓰기) | 일간 |
| 에러율 | < 0.5% | 일간 |
| 주문 처리량 | > 100 TPS | 피크 시간 |

---

## 2. 병목 지점 분석

### 2.1 DB 쿼리 병목

#### 2.1.1 N+1 문제 가능 서비스

| 서비스 | 엔티티 | N+1 포인트 | 현재 대응 | 심각도 |
|--------|--------|-----------|----------|--------|
| Product | Product → options (OneToMany, LAZY) | 상품 목록에서 옵션 조회 시 | `FetchType.LAZY` 설정만 | 높음 |
| Product | Product → images (OneToMany, LAZY) | 상품 상세에서 이미지 조회 시 | `FetchType.LAZY` 설정만 | 높음 |
| Product | Product → sizeGuides (OneToMany, LAZY) | 상품 상세 시 | `FetchType.LAZY` 설정만 | 중간 |
| Order | Order → OrderItems (별도 테이블) | 주문 목록에서 아이템 조회 시 | 없음 | 높음 |
| Review | Review → images (OneToMany, LAZY) | 리뷰 목록에서 이미지 조회 시 | `FetchType.LAZY` 설정만 | 중간 |
| Display | Exhibition → ExhibitionProduct (OneToMany) | 기획전 목록에서 상품 조회 시 | 없음 | 중간 |
| Content | Coordination → CoordinationProduct | 코디 상세에서 상품 조회 시 | 없음 | 낮음 |

**코드 분석 -- Product 서비스:**

```kotlin
// Product 엔티티 -- 3개의 OneToMany LAZY 관계
@OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
val options: MutableList<ProductOption> = mutableListOf()

@OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
val images: MutableList<ProductImage> = mutableListOf()

@OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
val sizeGuides: MutableList<SizeGuide> = mutableListOf()
```

상품 목록 20개 조회 시 최악의 경우: `1(Product 목록) + 20(options) + 20(images) = 41 쿼리`

#### 2.1.2 인덱스 미적용 쿼리 추정

| 테이블 | 예상 쿼리 패턴 | 필요 인덱스 | 근거 |
|--------|--------------|-----------|------|
| `product` | WHERE category_id = ? AND status = 'ACTIVE' | `idx_product_category_status` | 카테고리별 상품 목록 |
| `product` | WHERE brand_id = ? AND status = 'ACTIVE' | `idx_product_brand_status` | 브랜드별 상품 목록 |
| `product` | WHERE gender = ? AND season = ? AND status = 'ACTIVE' | `idx_product_gender_season_status` | 성별/시즌 필터 |
| `orders` | WHERE member_id = ? AND status != 'CANCELLED' ORDER BY created_at DESC | `idx_order_member_status_created` | 마이페이지 주문 목록 |
| `orders` | WHERE seller_id = ? AND status = ? | `idx_order_seller_status` | 셀러 주문 관리 |
| `order_item` | WHERE order_id = ? | `idx_orderitem_order` | 주문 상세 |
| `inventory_item` | WHERE product_option_id = ? | UK 존재 (unique) | 재고 조회 -- 이미 있음 |
| `review` | WHERE product_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC | `idx_review_product_status_created` | 상품 리뷰 목록 |
| `review` | WHERE member_id = ? ORDER BY created_at DESC | `idx_review_member_created` | 내 리뷰 목록 |
| `member_coupon` | WHERE member_id = ? AND status = 'AVAILABLE' | `idx_membercoupon_member_status` | 사용 가능 쿠폰 |
| `settlement_item` | WHERE seller_id = ? AND settlement_id = ? | `idx_settlementitem_seller_settlement` | 정산 항목 조회 |
| `shipment` | WHERE order_id = ? | `idx_shipment_order` | 배송 조회 |
| `notification` | WHERE member_id = ? AND is_read = 0 ORDER BY created_at DESC | `idx_notification_member_read` | 읽지 않은 알림 |

#### 2.1.3 대용량 테이블 예측 (1년 후)

| 테이블 | 예상 데이터량 (1년) | 성장률/월 | 파티셔닝 필요 시점 |
|--------|-------------------|---------|-----------------|
| `orders` | 500만+ | 40만/월 | 1000만 도달 시 |
| `order_item` | 1500만+ | 120만/월 | 1000만 도달 시 |
| `inventory_transaction` | 3000만+ | 250만/월 | 500만 도달 시 |
| `review` | 200만+ | 15만/월 | 1000만 도달 시 |
| `notification` | 2000만+ | 200만/월 | 500만 도달 시 |
| `processed_event` | 5000만+ (이벤트 통합 후) | 500만/월 | TTL로 관리 |
| `outbox_event` | TTL 관리 (7일) | - | TTL로 관리 |
| `point_history` | 500만+ | 50만/월 | 2000만 도달 시 |

**파티셔닝 전략:**

```sql
-- inventory_transaction: 월별 RANGE 파티셔닝
ALTER TABLE inventory_transaction
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- notification: 월별 파티셔닝 + 3개월 이전 아카이빙
-- processed_event: 30일 TTL 스케줄러로 삭제
```

### 2.2 네트워크 병목

#### 2.2.1 BFF Fan-out 문제

현재 BFF의 Facade 패턴이 서비스를 순차 호출한다.

```
HomeBffFacade:
  1. ProductService.getRecommendedProducts() — 50ms
  2. DisplayService.getBanners() — 30ms
  3. DisplayService.getRankings() — 80ms
  4. ProductService.getNewArrivals() — 50ms
  5. ContentService.getLatestMagazines() — 30ms
  총합: ~240ms (순차 호출 시)
```

```
OrderBffFacade (주문 생성):
  1. MemberService.getMember() — 20ms
  2. ProductService.getProducts() — 50ms
  3. PromotionService.validateCoupon() — 30ms
  4. OrderService.createOrder() — 100ms
  총합: ~200ms (순차 호출 시)
```

**문제:** 순차 호출 시 총 응답 시간 = 각 호출 시간의 합

#### 2.2.2 Gateway 오버헤드

```
Client → Gateway (JWT 검증: ~5ms, Rate Limit 체크: ~2ms, 로깅: ~1ms)
       → BFF (~10ms 라우팅)
       → Service
추가 오버헤드: ~18ms per request
```

### 2.3 메모리/리소스 병목

#### 2.3.1 JVM 설정 (예상 필요 리소스)

| 서비스 | 예상 메모리 | 힙 설정 | Connection Pool | 비고 |
|--------|-----------|---------|----------------|------|
| Product | 높음 (캐시 + 옵션) | -Xmx512m | 20 | 읽기 빈도 최고 |
| Order | 높음 (트랜잭션 + Saga) | -Xmx512m | 20 | 쓰기 빈도 높음 |
| Payment | 중간 | -Xmx256m | 10 | PG 연동 대기 |
| Inventory | 높음 (동시성) | -Xmx512m | 20 | Optimistic Lock 재시도 |
| Search | 높음 (ES 클라이언트) | -Xmx512m | - | ES 커넥션 별도 |
| BFF | 높음 (Fan-out) | -Xmx512m | 서비스당 10 | Feign 커넥션 풀 |
| Gateway | 중간 | -Xmx256m | - | 비차단 I/O |
| 기타 | 낮음-중간 | -Xmx256m | 10 | |

#### 2.3.2 Connection Pool 경쟁

현재 모든 서비스가 단일 DB를 공유하므로:
- 총 Connection 필요: 17 서비스 * 평균 15 connections = ~255 connections
- MySQL 기본 max_connections: 151
- **결론:** Connection Pool 부족 위험 높음

---

## 3. 개선 방안 (후보군 + 트레이드오프)

### 3.1 읽기 성능 개선

#### 후보 A) Redis Cache + TTL

상품, 카테고리, 브랜드 등 읽기 빈도가 높은 데이터를 Redis에 캐싱한다.

```kotlin
@Cacheable(value = ["product-detail"], key = "#productId", unless = "#result == null")
fun getProductDetail(productId: Long): ProductDetailResponse { ... }
```

| 장점 | 단점 |
|------|------|
| 구현 간단 (Spring Cache) | 캐시 무효화 전략 필요 |
| DB 부하 대폭 감소 | Redis 장애 시 DB 직접 조회로 전환 필요 |
| 응답 시간 크게 개선 | 데이터 신선도 vs 성능 트레이드오프 |

**예상 효과:**

| 대상 | 현재 예상 | 캐시 적용 후 | 개선율 |
|------|----------|-----------|--------|
| 상품 상세 | 30-50ms | 3-5ms | 90% |
| 카테고리 목록 | 10-20ms | 1ms (Local) | 95% |
| 랭킹 목록 | 80-100ms | 3-5ms | 95% |
| 상품 목록 (페이지) | 100-150ms | 10-15ms | 90% |

#### 후보 B) CQRS 읽기 모델

쓰기와 읽기를 분리하여, 읽기 전용 데이터 저장소(MongoDB 또는 ES)에 비정규화된 뷰를 유지한다.

```
쓰기: Product Service → MySQL (정규화)
      ↓ Event
읽기: Product View Service → MongoDB/ES (비정규화, 조인 불필요)
```

| 장점 | 단점 |
|------|------|
| 읽기 최적화된 스키마 | 복잡도 대폭 증가 (이벤트 + 프로젝션) |
| N+1 문제 원천 해결 | 데이터 지연 (Eventual Consistency) |
| 독립적 스케일링 | 추가 인프라 비용 (MongoDB/ES) |

#### 후보 C) Read Replica

MySQL Read Replica를 구성하여 읽기 트래픽을 분산한다.

```
쓰기: Primary MySQL
읽기: Read Replica MySQL (1-2개)
```

| 장점 | 단점 |
|------|------|
| 애플리케이션 변경 최소 | 리플리케이션 지연 (0.1-1초) |
| 쓰기/읽기 부하 분산 | 추가 DB 인스턴스 비용 |
| 기존 쿼리 그대로 사용 | Connection 라우팅 설정 필요 |

#### 결정: A) Redis Cache (단기) + C) Read Replica (중기)

**근거:**
1. Redis 캐시는 즉각적인 효과가 크고 구현 비용이 낮음
2. 대부분의 읽기 병목은 캐시로 해결 가능
3. CQRS는 현 단계에서 과도한 복잡도
4. Read Replica는 데이터량 증가 시 (6개월 후) 도입

### 3.2 쓰기 성능 개선

#### 후보 A) Async Processing (Kafka)

주문/결제 후속 처리를 비동기로 전환한다.

```
현재: Order 생성 → (동기) 재고 예약 → (동기) 결제 → (동기) 알림 — 총 300ms
개선: Order 생성 → (동기) 쿠폰 검증 → Order 저장 → (비동기) 재고 예약 → 결제 — 즉시 응답 100ms
```

| 장점 | 단점 |
|------|------|
| 사용자 응답 시간 단축 | 최종 결과 확인 지연 |
| 서비스 간 결합도 감소 | Saga 복잡도 증가 |
| 처리량(Throughput) 증가 | 메시지 순서/중복 처리 필요 |

#### 후보 B) Batch Write

대량 데이터 쓰기를 배치로 처리한다.

```kotlin
// 정산 집계 -- 개별 INSERT 대신 Batch
@Modifying
@Query("INSERT INTO settlement_item (...) VALUES (:items)", nativeQuery = true)
fun batchInsert(items: List<SettlementItem>)
```

| 장점 | 단점 |
|------|------|
| DB 쓰기 효율 대폭 증가 | 실시간성 감소 |
| 네트워크 왕복 감소 | 배치 크기 조절 필요 |
| 트랜잭션 크기 증가 가능 | |

#### 후보 C) Optimistic Lock → 분산 락 (Redis)

재고의 Optimistic Lock 충돌률이 높아질 경우 Redis 분산 락으로 전환한다.

```kotlin
// 현재: Optimistic Lock (@Version)
@Version
var version: Long = 0

// 개선: Redis 분산 락
fun reserveWithLock(productOptionId: Long, quantity: Int, orderId: String) {
    val lockKey = "lock:inventory:$productOptionId"
    redisLockService.withLock(lockKey, timeout = 3.seconds) {
        val item = inventoryRepository.findByProductOptionId(productOptionId)
        item.reserve(quantity, orderId)
        inventoryRepository.save(item)
    }
}
```

| 기준 | Optimistic Lock | Redis 분산 락 |
|------|----------------|-------------|
| **동시 접근 낮음** | 최적 (재시도 드묾) | 오버헤드 |
| **동시 접근 높음** | 재시도 급증, 성능 저하 | 안정적 |
| **구현 복잡도** | 낮음 (@Version) | 중간 (Redis + TTL 관리) |
| **성능** | 충돌 없으면 최고 | Redis 네트워크 홉 추가 |

#### 결정: A) Async Processing (필수) + B) Batch Write (정산/알림) + C) 상황별 선택

**근거:**
1. Async Processing은 도메인 통합 설계에서 이미 결정 (Kafka Saga)
2. Batch Write는 정산 집계, 알림 대량 발송, 이벤트 처리 등에 적용
3. Optimistic Lock은 현재 유지하되, 인기 상품 재고 경쟁 시 Redis 분산 락으로 전환
   - 전환 기준: Optimistic Lock 재시도율 > 10%

### 3.3 검색 성능 개선

#### 후보 A) Elasticsearch 튜닝

| 튜닝 항목 | 현재 | 개선 | 효과 |
|----------|------|------|------|
| Shard 수 | 기본 1 | Primary 3, Replica 1 | 병렬 검색 3배 |
| Refresh Interval | 1초 (기본) | 5초 | 인덱싱 부하 감소 |
| Bulk Size | 개별 인덱싱 | 500건 단위 Bulk | 인덱싱 10배 빠름 |
| Request Cache | 비활성 | 활성 | 반복 쿼리 캐시 |
| Field Data | text 필드 | keyword + text 분리 | 정렬/집계 성능 향상 |

#### 후보 B) nori 분석기 최적화

```json
{
  "analysis": {
    "analyzer": {
      "closet_analyzer": {
        "type": "custom",
        "tokenizer": "nori_tokenizer",
        "filter": ["nori_readingform", "lowercase", "nori_part_of_speech_filter"]
      }
    },
    "filter": {
      "nori_part_of_speech_filter": {
        "type": "nori_part_of_speech",
        "stoptags": ["E", "IC", "J", "MAG", "MAJ", "MM", "SP", "SSC", "SSO", "SC", "SE", "XPN", "XSA", "XSN", "XSV", "UNA", "NA", "VSV"]
      }
    },
    "tokenizer": {
      "nori_tokenizer": {
        "type": "nori_tokenizer",
        "decompound_mode": "mixed",
        "user_dictionary_rules": [
          "무신사", "나이키에어맥스", "오버핏후드티", "슬림핏청바지"
        ]
      }
    }
  }
}
```

| 항목 | 효과 |
|------|------|
| decompound_mode: mixed | 복합어 분석 + 원형 보존 |
| user_dictionary | 의류 브랜드/상품명 분석 정확도 향상 |
| stoptags 최적화 | 불필요 품사 제거 -- 인덱스 크기 감소 |

#### 후보 C) 캐시 레이어 추가

```
검색 요청 → Redis 캐시 (인기 키워드 캐시) → ES 쿼리
           ↓ (miss)
           ES → 결과 → Redis 저장 (TTL 3분)
```

인기 검색어 TOP 100은 Redis에 사전 캐싱하여 ES 부하 감소.

#### 결정: A + B + C 모두 적용

**근거:** 검색은 이커머스의 핵심 기능이며, 각 개선이 독립적이므로 모두 적용한다.

**적용 순서:**
1. B) nori 분석기 최적화 -- 검색 품질 향상 (1주)
2. A) ES 튜닝 (shard, refresh, bulk) -- 성능 향상 (1주)
3. C) 인기 검색어 캐시 -- 부하 감소 (3일)

### 3.4 BFF Fan-out 최적화

#### 현재 문제: 순차 호출

```kotlin
// HomeBffFacade -- 현재 (순차)
fun getHomePage(): HomePageResponse {
    val products = productClient.getRecommendedProducts()  // 50ms
    val banners = displayClient.getBanners()                // 30ms
    val rankings = displayClient.getRankings()              // 80ms
    val newArrivals = productClient.getNewArrivals()        // 50ms
    val magazines = contentClient.getLatestMagazines()      // 30ms
    return HomePageResponse(products, banners, rankings, newArrivals, magazines)
    // 총 ~240ms
}
```

#### 개선: 병렬 호출 (CompletableFuture / Coroutines)

```kotlin
// HomeBffFacade -- 개선 (병렬)
suspend fun getHomePage(): HomePageResponse = coroutineScope {
    val products = async { productClient.getRecommendedProducts() }
    val banners = async { displayClient.getBanners() }
    val rankings = async { displayClient.getRankings() }
    val newArrivals = async { productClient.getNewArrivals() }
    val magazines = async { contentClient.getLatestMagazines() }
    HomePageResponse(
        products.await(),
        banners.await(),
        rankings.await(),
        newArrivals.await(),
        magazines.await()
    )
    // 총 ~80ms (가장 느린 호출 기준)
}
```

**예상 효과:**

| BFF API | 순차 호출 | 병렬 호출 | 개선율 |
|---------|----------|----------|--------|
| 홈 페이지 | ~240ms | ~80ms | 67% |
| 상품 상세 | ~130ms | ~50ms | 62% |
| 주문 생성 | ~200ms | ~100ms (일부 순차 필수) | 50% |
| 마이페이지 | ~180ms | ~60ms | 67% |

### 3.5 N+1 쿼리 해결

#### 해결 전략 1: EntityGraph / Fetch Join

```kotlin
// ProductRepository
@EntityGraph(attributePaths = ["options", "images"])
fun findByIdWithOptionsAndImages(productId: Long): Product?

// 또는 JPQL Fetch Join
@Query("SELECT p FROM Product p LEFT JOIN FETCH p.options LEFT JOIN FETCH p.images WHERE p.id = :id")
fun findByIdFetchAll(@Param("id") id: Long): Product?
```

#### 해결 전략 2: 배치 사이즈 설정

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

이렇게 하면 N+1이 `1 + ceil(N/100)` 쿼리로 줄어든다.

#### 해결 전략 3: DTO 직접 프로젝션

```kotlin
// 목록 조회에서는 엔티티 대신 DTO 프로젝션
@Query("""
    SELECT new com.closet.product.application.dto.ProductListDto(
        p.id, p.name, p.salePrice, p.discountRate, p.status,
        (SELECT pi.imageUrl FROM ProductImage pi WHERE pi.product = p AND pi.sortOrder = 0)
    )
    FROM Product p
    WHERE p.categoryId = :categoryId AND p.status = 'ACTIVE'
    ORDER BY p.createdAt DESC
""")
fun findProductListByCategoryId(categoryId: Long, pageable: Pageable): Page<ProductListDto>
```

#### 적용 전략

| 용도 | 전략 | 근거 |
|------|------|------|
| 상품 상세 | Fetch Join (options + images + sizeGuides) | 항상 모든 관계 필요 |
| 상품 목록 | DTO 프로젝션 (대표 이미지 1장만) | 옵션/사이즈 불필요, 이미지 1장만 |
| 주문 목록 | batch_fetch_size + DTO 프로젝션 | 주문 아이템 수가 다양 |
| 리뷰 목록 | batch_fetch_size | 이미지 유무가 다양 |
| 기획전 상세 | Fetch Join (ExhibitionProduct) | 항상 상품 목록 필요 |

**예상 효과:**

| 시나리오 | 현재 쿼리 수 | 개선 후 | 감소율 |
|---------|------------|--------|--------|
| 상품 목록 20건 | 41 (1 + 20 + 20) | 2 (DTO 프로젝션) | 95% |
| 상품 상세 1건 | 4 (1 + 1 + 1 + 1) | 1 (Fetch Join) | 75% |
| 주문 목록 10건 | 11 (1 + 10) | 2 (batch_fetch_size) | 82% |
| 리뷰 목록 20건 | 21 (1 + 20) | 2 (batch_fetch_size) | 90% |

### 3.6 Connection Pool 최적화

#### 현재 문제

17개 서비스 * 기본 HikariCP 10 = 170 connections. MySQL max_connections 기본 151에 근접.

#### 개선 방안

```yaml
# 서비스별 차등 설정
# 고부하 서비스: Order, Product, Inventory
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000

# 저부하 서비스: Content, CS, Seller
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
```

| 서비스 그룹 | 서비스 | Pool Size | 합계 |
|------------|--------|----------|------|
| 고부하 | Order, Product, Inventory, BFF | 20 | 80 |
| 중부하 | Payment, Member, Shipping, Promotion, Search | 10 | 50 |
| 저부하 | Display, Review, CS, Settlement, Notification, Content, Seller | 5 | 35 |
| Gateway | Gateway | 0 (비차단) | 0 |
| **총합** | | | **165** |

MySQL 설정:
```ini
[mysqld]
max_connections = 250
wait_timeout = 600
```

---

## 4. 단계별 개선 로드맵

### Phase 1: 즉각적 개선 (1-2주)

| # | 작업 | 예상 효과 | 난이도 | 소요 |
|---|------|----------|--------|------|
| 1.1 | hibernate.default_batch_fetch_size: 100 설정 | N+1 쿼리 70% 감소 | 낮음 | 0.5일 |
| 1.2 | 핵심 테이블 인덱스 추가 (14개) | 조회 성능 30-50% 향상 | 낮음 | 1일 |
| 1.3 | HikariCP Connection Pool 차등 설정 | DB 커넥션 안정화 | 낮음 | 0.5일 |
| 1.4 | MySQL max_connections 250으로 조정 | 커넥션 부족 방지 | 낮음 | 0.5일 |

**예상 총 효과:** DB 쿼리 성능 30-50% 향상, N+1 문제 대부분 해결

### Phase 2: 캐싱 (2-3주)

| # | 작업 | 예상 효과 | 난이도 | 소요 |
|---|------|----------|--------|------|
| 2.1 | Redis Cache-Aside: 카테고리, 브랜드 | DB 조회 99% 감소 (해당 대상) | 낮음 | 1일 |
| 2.2 | Redis Cache-Aside: 상품 상세 (TTL 5분) | 상품 상세 90% 캐시 히트 | 중간 | 2일 |
| 2.3 | Caffeine L1 캐시: 카테고리, 브랜드 | 네트워크 홉 제거 | 중간 | 1일 |
| 2.4 | 캐시 무효화 이벤트 연동 | 데이터 신선도 보장 | 중간 | 3일 |
| 2.5 | Redis ZSET 랭킹 고도화 | 랭킹 실시간성 향상 | 중간 | 2일 |

**예상 총 효과:** 읽기 API 응답 시간 70-90% 개선

### Phase 3: 비동기 처리 (3-4주)

| # | 작업 | 예상 효과 | 난이도 | 소요 |
|---|------|----------|--------|------|
| 3.1 | Kafka 이벤트 인프라 구축 | 비동기 처리 기반 | 중간 | 3일 |
| 3.2 | Transactional Outbox 구현 | 이벤트 유실 방지 | 중간 | 3일 |
| 3.3 | 주문 Saga 구현 | 주문 응답 시간 50% 개선 | 높음 | 1주 |
| 3.4 | 배송/리뷰 이벤트 연동 | 서비스 간 결합도 감소 | 중간 | 3일 |
| 3.5 | BFF 병렬 호출 (Coroutines) | BFF 응답 시간 60% 개선 | 중간 | 2일 |

**예상 총 효과:** 쓰기 API 50% 개선, BFF API 60% 개선

### Phase 4: 검색/쿼리 최적화 (2-3주)

| # | 작업 | 예상 효과 | 난이도 | 소요 |
|---|------|----------|--------|------|
| 4.1 | nori 분석기 최적화 + 사용자 사전 | 검색 정확도 향상 | 중간 | 3일 |
| 4.2 | ES shard/replica 튜닝 | 검색 처리량 3배 | 중간 | 1일 |
| 4.3 | ES Request Cache 활성화 | 반복 쿼리 캐시 | 낮음 | 0.5일 |
| 4.4 | 인기 검색어 Redis 캐시 | ES 부하 30% 감소 | 중간 | 2일 |
| 4.5 | 상품 목록 DTO 프로젝션 적용 | 목록 쿼리 95% 감소 | 중간 | 3일 |
| 4.6 | 상품 상세 Fetch Join 적용 | 상세 쿼리 75% 감소 | 낮음 | 1일 |

**예상 총 효과:** 검색 성능 50% 향상, 상품 조회 90% 쿼리 감소

### Phase 5: 인프라 확장 (4주+)

| # | 작업 | 예상 효과 | 난이도 | 소요 |
|---|------|----------|--------|------|
| 5.1 | Schema per Service 분리 | 서비스 독립성, 장애 격리 | 중간 | 1주 |
| 5.2 | MySQL Read Replica 구성 | 읽기 부하 분산 | 중간 | 3일 |
| 5.3 | Grafana 모니터링 대시보드 구축 | 성능 가시성 확보 | 중간 | 1주 |
| 5.4 | 대용량 테이블 파티셔닝 | 대용량 쿼리 성능 유지 | 높음 | 1주 |
| 5.5 | CDN (CloudFront) 상품 이미지 | 이미지 로딩 속도 개선 | 낮음 | 2일 |

---

## 5. 성능 모니터링 체계

### 5.1 핵심 메트릭 (RED Method)

| 메트릭 | 설명 | 수집 방법 | 알림 조건 |
|--------|------|----------|----------|
| **Rate** | 초당 요청 수 (RPS) | Micrometer + Prometheus | 급격한 변화 (>200%) |
| **Errors** | 에러 비율 (%) | Micrometer http_server_requests | > 1%: Warning, > 5%: Critical |
| **Duration** | 응답 시간 (ms) | Micrometer http_server_requests | P95 > 500ms: Warning |

### 5.2 DB 성능 메트릭

| 메트릭 | 설명 | 알림 조건 |
|--------|------|----------|
| HikariCP active connections | 활성 커넥션 수 | > 80% pool size |
| HikariCP pending threads | 대기 중 스레드 | > 0 (5분 이상) |
| Slow query count | 1초 이상 쿼리 수 | > 10/분 |
| DB replication lag | 리플리케이션 지연 | > 1초 |

### 5.3 Kafka 성능 메트릭

| 메트릭 | 설명 | 알림 조건 |
|--------|------|----------|
| Consumer lag | 컨슈머 처리 지연 | > 1000 |
| Producer send rate | 초당 메시지 발행 수 | 급격한 감소 |
| Consumer process time | 메시지 처리 시간 | P95 > 5초 |

### 5.4 성능 테스트 자동화

```bash
# 주간 성능 테스트 (CI/CD 파이프라인)
k6 run --out json=results.json load-test/realistic-traffic.js

# 성능 기준선 비교
# P95 응답 시간이 이전 대비 20% 이상 저하 시 알림
```

---

## 6. 예상 효과 요약

### 6.1 API별 예상 응답 시간 변화

| API | 현재 예상 | Phase 1 후 | Phase 2 후 | Phase 3 후 | 최종 목표 |
|-----|----------|-----------|-----------|-----------|----------|
| 상품 목록 | 150ms | 80ms | 15ms (캐시 히트) | 15ms | < 100ms |
| 상품 상세 | 100ms | 40ms | 5ms (캐시 히트) | 5ms | < 50ms |
| 검색 | 200ms | 150ms | 120ms | 100ms | < 100ms |
| 주문 생성 | 300ms | 250ms | 200ms | 100ms (비동기) | < 200ms |
| 메인 페이지 (BFF) | 300ms | 200ms | 50ms | 50ms (병렬) | < 200ms |
| 마이페이지 (BFF) | 250ms | 150ms | 40ms | 40ms (병렬) | < 150ms |

### 6.2 리소스 효율성 예상

| 지표 | 현재 | 개선 후 | 개선율 |
|------|------|--------|--------|
| DB 쿼리 수 (상품 목록) | 41/요청 | 2/요청 | 95% |
| DB 커넥션 사용 | 최대 170 | 최대 165 (차등) | 안정화 |
| Redis 히트율 | 0% | 85%+ | - |
| BFF 응답 시간 | 240ms | 80ms | 67% |
| Kafka Consumer Lag | - | < 100 | - |

### 6.3 비용 대비 효과

| Phase | 소요 기간 | 예상 효과 | 우선순위 |
|-------|----------|----------|---------|
| Phase 1 | 1-2주 | 30-50% DB 성능 향상 | 최우선 (ROI 최고) |
| Phase 2 | 2-3주 | 70-90% 읽기 성능 향상 | 높음 |
| Phase 3 | 3-4주 | 50-60% 쓰기/BFF 성능 향상 | 높음 |
| Phase 4 | 2-3주 | 50% 검색 성능 향상 | 중간 |
| Phase 5 | 4주+ | 인프라 안정성 + 확장성 | 중기 계획 |
