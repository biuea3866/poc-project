# Phase 2 비기능 요구사항 분석

> 작성일: 2026-04-04
> 프로젝트: Closet E-commerce
> Phase: 2 (성장) - 배송 + 재고 + 검색 + 리뷰
> 분석 대상 PRD: `.analysis/prd/results/2026-03-22_phase2-prd/phase2_prd.md`

---

## 1. 성능 요구사항

### 1.1 응답시간 수치 목표

| API 유형 | P50 | P95 | P99 | 비고 |
|----------|-----|-----|-----|------|
| 재고 조회 (`GET /inventories`) | 20ms | 50ms | 100ms | Redis 캐시 활용 시 |
| 재고 차감 (`POST /inventories/{id}/deduct`) | 30ms | 80ms | 200ms | 분산 락 포함 |
| 배송 조회 (`GET /shippings/{id}/tracking`) | 30ms | 100ms | 300ms | Redis 캐시 히트 시 30ms, 택배사 API fallback 시 300ms |
| 검색 (`GET /search/products`) | 50ms | 150ms | 300ms | Elasticsearch 쿼리 + facet 집계 |
| 자동완성 (`GET /search/autocomplete`) | 10ms | 30ms | 50ms | PRD 명시: 50ms 이내 |
| 인기 검색어 (`GET /search/popular-keywords`) | 5ms | 10ms | 20ms | PRD 명시: 10ms 이내 (Redis 직접 조회) |
| 리뷰 목록 (`GET /reviews`) | 30ms | 80ms | 150ms | 페이지네이션 적용 |
| 리뷰 작성 (`POST /reviews`) | 100ms | 300ms | 500ms | S3 업로드 포함 시 별도 비동기 |
| 리뷰 집계 (`GET /reviews/summary`) | 10ms | 30ms | 50ms | Redis 캐시 |
| 송장 등록 (`POST /shippings`) | 30ms | 80ms | 150ms | Kafka 이벤트 발행 포함 |
| 반품 신청 (`POST /returns`) | 30ms | 80ms | 200ms | |
| 교환 신청 (`POST /exchanges`) | 50ms | 120ms | 250ms | 재고 선점(분산 락) 포함 |

### 1.2 TPS 및 동시 사용자 수

| 지표 | 목표치 | 산출 근거 |
|------|--------|----------|
| 동시 사용자 (CCU) | 500명 | Phase 2 MAU 15,000명 기준, 피크 시간대 동시접속률 3.3% |
| 검색 TPS | 100 req/s | CCU 500명, 평균 12초에 1회 검색 |
| 자동완성 TPS | 300 req/s | 검색어 입력 중 글자당 요청, 검색 TPS의 3배 |
| 인기 검색어 TPS | 50 req/s | 검색 페이지 진입 시 1회 |
| 재고 조회 TPS | 200 req/s | 상품 상세 조회 시 재고 확인 |
| 재고 차감 TPS | 30 req/s | 일 50건 주문 + 피크 집중도 x10 (타임세일 등) |
| 배송 추적 TPS | 50 req/s | 배송 중 주문 건 반복 조회 |
| 리뷰 조회 TPS | 150 req/s | 상품 상세 진입 시 리뷰 로딩 |
| 리뷰 작성 TPS | 5 req/s | 일 10건 리뷰 기준 |

### 1.3 재고 차감: 동시성 제어 상세 분석

PRD US-602에서 Redis 분산 락(Redisson) + 낙관적 락(@Version)을 이중 안전장치로 명시하고 있다.

#### 분산 락 설계

```
[요청] -> [Redisson Lock 획득 시도 (5s timeout)]
              |
         성공 -> [DB 재고 차감 (SELECT FOR UPDATE or @Version)]
              |        |
              |    성공 -> [재고 이력 기록] -> [Kafka 이벤트 발행] -> [Lock 해제]
              |    실패 (OptimisticLockException) -> [Lock 해제] -> [재시도 (최대 3회)]
              |
         실패 -> [재시도 (최대 3회)] -> [실패 응답]
```

| 항목 | 설계 값 | 근거 |
|------|--------|------|
| 락 키 | `inventory:lock:{sku}` | SKU 단위 격리로 서로 다른 상품 간 경합 방지 |
| 락 대기 시간 (waitTime) | 5초 | PRD 명시. 피크 시 최대 30 TPS 기준 평균 대기 33ms, 5초면 충분 |
| 락 유지 시간 (leaseTime) | 3초 | PRD 명시. DB 쓰기 + Kafka 발행 = 일반적으로 50ms 이내 |
| 재시도 횟수 | 3회 | PRD 명시 |
| 재시도 간격 | 100ms (exponential backoff 권장) | 즉시 재시도 시 경합 악화 방지 |

#### 성능 테스트 기준 (PRD 명시)

- 100개 스레드 동시 차감 시 최종 재고 수량 정확성 100% 보장
- 초과 판매(overselling) 0건

#### 권장사항

1. **Redisson watchdog 활용**: leaseTime을 -1로 설정하면 watchdog이 30초마다 갱신. 다만 PRD에서 3초 명시이므로 leaseTime 3초를 유지하되, DB 트랜잭션이 예상 외로 길어지는 경우 대비 모니터링 필수.
2. **Lua 스크립트 대안 검토**: 단순 차감이면 Redis WATCH + MULTI/EXEC 또는 Lua 스크립트로 원자적 처리 가능. 다만 DB 이력 기록이 필수이므로 Redisson 분산 락이 적합.
3. **HikariCP 커넥션 풀**: 재고 차감 피크 TPS 30 기준, 커넥션 풀 최소 20개 권장 (3초 leaseTime 내 처리 가능).

### 1.4 배송 추적: 택배사 API 호출 전략

PRD US-502에서 Redis 5분 캐싱을 명시하고 있다.

#### 호출 빈도 산정

| 시나리오 | 계산 | 결과 |
|----------|------|------|
| 배송 중 주문 건수 (일 평균) | 일 50건 x 평균 배송 2일 | 100건 |
| 구매자 조회 빈도 (건당) | 일 3회 (아침/점심/저녁) | 300회/일 |
| Redis 캐시 미스율 (TTL 5분) | 300회 / (24h x 60min / 5min) = 300 / 288 | 약 100% 미스 불가, 실제 미스율 약 30% |
| 실제 택배사 API 호출 | 300 x 0.3 | 약 90회/일 |

#### Redis 캐싱 전략

```
캐시 키: shipping:tracking:{carrier}:{trackingNumber}
TTL: 300초 (5분) — PRD 명시
값: JSON (currentStatus, trackingLogs[], estimatedDelivery)
```

| 전략 | 설명 |
|------|------|
| Cache-Aside | 조회 시 Redis 먼저 확인 -> 미스 시 택배사 API 호출 -> 결과 캐시 저장 |
| Fallback | 택배사 API 장애 시 만료된 캐시라도 반환 (stale-while-error). PRD 명시 |
| Circuit Breaker | 택배사 API 연속 실패 5회 시 30초 차단 (Resilience4j) |
| Rate Limiting | 택배사 API 호출 제한 준수 (스마트택배: 분당 60회 기준) |

#### 개선 권장사항

- **배송 상태 변경 시점 집중 갱신**: 배송 완료 직전(IN_TRANSIT 후반) 캐시 TTL을 2분으로 단축하여 배송 완료 감지 빠르게.
- **스케줄러 기반 사전 갱신**: Spring Scheduler로 30분 간격 배송 중 건 일괄 업데이트. 캐시 웜업 효과.
- **벌크 API 활용**: 택배사가 벌크 조회 API를 제공하면 건별 호출 대비 효율적.

### 1.5 검색: Elasticsearch 쿼리 성능

#### 인덱스 설계 성능 고려사항

| 항목 | PRD 설계 | 성능 영향 | 권장 |
|------|----------|----------|------|
| nori_analyzer | 한글 형태소 분석 | 인덱싱 시간 증가 (2-3x) | 검색 품질 우선, 인덱싱은 비동기이므로 수용 |
| edge_ngram (자동완성) | min_gram=1, max_gram=20 | 인덱스 크기 3-5x 증가 | min_gram=2로 상향 권장 (PRD: 2글자 이상 입력 시) |
| facet 집계 | 카테고리/브랜드/가격/색상/사이즈 | 5개 aggregation 병렬 실행 | 상품 10만건 이하: 100ms 이내 가능 |
| 하이라이팅 | 검색 결과 하이라이팅 | 10-20ms 추가 | 수용 가능 |

#### 쿼리 응답시간 예상

| 쿼리 유형 | 상품 1만건 | 상품 10만건 | 상품 100만건 |
|-----------|-----------|------------|-------------|
| 키워드 검색 | 10ms | 30ms | 80ms |
| 키워드 + 5개 필터 | 20ms | 50ms | 120ms |
| 키워드 + 필터 + facet | 30ms | 80ms | 200ms |
| 자동완성 (edge_ngram) | 5ms | 10ms | 25ms |

#### 인덱싱 지연 목표: 3초 이내 (PRD 명시)

```
product-service -> Kafka (product.created/updated) -> search-service -> Elasticsearch
                   ~10ms                                ~50ms            ~200ms bulk
총 지연: ~260ms (3초 목표 대비 충분한 여유)
```

#### 권장사항

1. **Replica 설정**: 단일 노드 환경에서는 replica=0, 프로덕션에서는 replica=1 이상.
2. **Refresh interval**: 기본 1초. 인덱싱 부하 시 5초로 조정 가능 (실시간성 트레이드오프).
3. **Slow query log**: 200ms 초과 쿼리 로깅으로 성능 저하 조기 감지.
4. **ES JVM 힙**: docker-compose에서 512MB 설정 중. 상품 10만건 이상 시 1GB로 증가 필요.

---

## 2. 보안 요구사항

### 2.1 인증/인가

| 항목 | 요구사항 | 구현 방안 |
|------|----------|----------|
| API 인증 | 모든 Phase 2 API에 Bearer Token 필수 (PRD 전 API에 명시) | Phase 1 member-service JWT 인증 재사용 |
| 판매자 권한 | 송장 등록(US-501), 반품 승인/거절(US-504) | Role: `SELLER`. Gateway에서 role 체크 |
| 구매자 권한 | 배송 조회(US-502), 반품/교환 신청(US-504, US-505) | 본인 주문 건에 대해서만 접근 허용 |
| 관리자 권한 | 벌크 인덱싱(US-701), 리뷰 집계 재계산(US-804) | Role: `ADMIN` |
| 내부 서비스 호출 | 재고 차감(inventory-service <- order-service) | 내부 서비스 간 인증 (서비스 토큰 or IP 화이트리스트) |
| 리뷰 작성 제한 | 구매확정 주문에 대해서만 1회 가능 | 주문 상태 + 기존 리뷰 존재 여부 검증 |

#### PRD 미명시 보안 Gap

| Gap | 리스크 | 권장 |
|-----|--------|------|
| 내부 서비스 간 인증 미정의 | 재고 차감 API 외부 노출 시 악용 가능 | API Gateway에서 internal API 라우팅 차단 + 서비스 토큰 |
| 리뷰 스팸 방지 미정의 | 악의적 다수 리뷰 작성 | 포인트 적립 한도 5,000P/일 외에 리뷰 작성 건수 제한 (일 10건 등) |
| 검색 Rate Limiting 미정의 | 검색 API 무차별 호출 | IP 기준 분당 120회, 사용자 기준 분당 60회 |

### 2.2 개인정보 보호 (배송지 정보 마스킹)

#### 마스킹 대상 데이터

| 데이터 | 저장 형태 | 조회 시 마스킹 | 근거 |
|--------|----------|--------------|------|
| 배송 tracking log의 location | 평문 | "서울 강남 **점" | 집하점 상세 정보 노출 방지 |
| 반품/교환 사유 상세 (reason_detail) | 평문 | 본인 외 마스킹 | 개인 사유 보호 |
| 리뷰 사이즈 정보 (height, weight) | 평문 | 구간 표시 (170-175cm, 65-70kg) | 신체 정보 보호 |
| 리뷰 작성자 | member_id | 닉네임 + 아이디 부분 마스킹 ("김**") | PII 보호 |

#### PRD 미명시 보안 Gap

| Gap | 리스크 | 권장 |
|-----|--------|------|
| shipping 테이블에 수취인 정보 부재 | Phase 1 order 테이블에 배송지 정보 있을 것으로 추정. 개인정보 분리 필요 | 배송지 정보는 암호화 저장 (AES-256). 조회 시 복호화 |
| 리뷰 이미지 S3 URL 직접 노출 | URL 유추로 타인 이미지 접근 가능 | CloudFront Signed URL 또는 S3 presigned URL (만료시간 1시간) |
| 검색 쿼리 로깅 | 개인정보 포함 검색어 로깅 위험 | 검색 로그에서 PII 필터링. 분석용 데이터는 해시 처리 |

---

## 3. 확장성 요구사항

### 3.1 데이터 증가 예측

| 테이블 | 월간 증가량 | 연간 누적 | 인덱스 크기 | 쿼리 패턴 |
|--------|-----------|----------|------------|----------|
| inventory | +500 (신규 SKU) | 6,000 rows | 작음 | PK/UK 조회. 문제 없음 |
| inventory_history | +3,000 (일 100건 x 30일) | 36,000 rows | 중간 | idx_inventory_id 조회. 1년 이내 문제 없음 |
| shipping | +1,500 (일 50건 x 30일) | 18,000 rows | 작음 | uk_order_id 조회. 문제 없음 |
| shipping_tracking_log | +15,000 (배송 건당 평균 10개 로그) | 180,000 rows | 중간 | idx_shipping_id 조회 |
| return_request | +150 (반품률 10%) | 1,800 rows | 작음 | |
| exchange_request | +75 (교환률 5%) | 900 rows | 작음 | |
| review | +300 (리뷰 작성률 20%) | 3,600 rows | 작음 | idx_product_id 조회 |
| review_image | +450 (포토 리뷰 30% x 평균 5장) | 5,400 rows | 작음 | |
| review_size_info | +120 (사이즈 리뷰 40%) | 1,440 rows | 작음 | |
| review_summary | +50 (신규 상품) | 600 rows | 작음 | uk_product_id 조회 |
| restock_notification | +200 | 2,400 rows | 작음 | |
| ES closet-products 인덱스 | +500 docs | 6,000 docs | ~50MB | 충분히 작음 |

#### 결론

Phase 2 초기(1년)에는 데이터 볼륨이 작아 파티셔닝이나 샤딩이 불필요하다. 단, 다음 시점에서 대비가 필요하다:

| 시점 | 조치 |
|------|------|
| inventory_history 100만건 초과 | created_at 기준 월별 파티셔닝 (RANGE PARTITION) |
| shipping_tracking_log 100만건 초과 | 같은 파티셔닝 전략 |
| ES 인덱스 100만 docs 초과 | 인덱스 alias + 롤링 인덱스 (월별) |
| 검색 TPS 500 초과 | ES 노드 수평 확장 (data node 추가) |

### 3.2 Kafka 파티션 전략

| 토픽 | 파티션 수 | 파티션 키 | 근거 |
|------|----------|----------|------|
| `order.created` | 3 | orderId | 같은 주문 건 순서 보장. 일 50건이면 3 파티션이면 충분 |
| `order.cancelled` | 3 | orderId | 동일 |
| `order.status.changed` | 3 | orderId | 동일 |
| `order.confirmed` | 3 | orderId | 동일 |
| `product.created` | 3 | productId | 동일 상품 이벤트 순서 보장 |
| `product.updated` | 3 | productId | 동일 |
| `product.deleted` | 3 | productId | 동일 |
| `inventory.insufficient` | 1 | - | 빈도 낮음 |
| `inventory.low_stock` | 1 | - | 알림 전용, 빈도 낮음 |
| `inventory.out_of_stock` | 1 | - | 동일 |
| `inventory.restock_notification` | 3 | memberId | 회원별 순서 보장 |
| `return.approved` | 3 | orderId | 반품 승인 -> 재고 복구 순서 보장 |
| `review.created` | 3 | memberId | 포인트 적립 회원별 순서 보장 |
| `review.summary.updated` | 3 | productId | ES 동기화 상품별 순서 보장 |
| `point.earn` | 3 | memberId | 회원별 포인트 순서 보장 |

#### Consumer Group 설계

| Consumer Group | 구독 토픽 | 인스턴스 수 | 비고 |
|---------------|----------|------------|------|
| `inventory-service` | order.created, order.cancelled, return.approved | 1 (단일 인스턴스) | 재고 정합성 최우선 |
| `shipping-service` | order.created, order.confirmed | 1 | |
| `search-service` | product.created/updated/deleted, review.summary.updated | 1 | |
| `review-service` | order.confirmed | 1 | 리뷰 작성 가능 상태 동기화 |
| `member-service` | review.created (point.earn) | 1 | 포인트 적립 |
| `notification-service` | inventory.low_stock, inventory.restock_notification | 1 | 알림 발송 |

#### 확장 시 고려사항

- 파티션 수는 consumer 인스턴스 수보다 크거나 같아야 한다. 현재 단일 인스턴스이므로 3 파티션은 향후 수평 확장 대비.
- `replication.factor=1` (단일 브로커). 프로덕션에서는 `replication.factor=3` 필수.

---

## 4. 마이그레이션: Phase 1 -> Phase 2 데이터 이관

### 4.1 마이그레이션 대상

| 항목 | Phase 1 상태 | Phase 2 필요 조치 | 복잡도 |
|------|-------------|-----------------|--------|
| 기존 주문에 재고 매핑 | order 테이블에 product_id, option_id 존재 | inventory 테이블 초기 데이터 생성 + 기존 주문과 SKU 매핑 | 높음 |
| 기존 상품 ES 인덱싱 | MySQL product 테이블에만 존재 | bulk indexing API로 전량 ES 동기화 | 중간 |
| 기존 결제 완료 주문의 배송 정보 | shipping 테이블 미존재 | 기존 PAID 상태 주문은 Phase 2 배송 흐름에서 제외 또는 수동 마이그레이션 | 중간 |
| 리뷰 집계 초기화 | review_summary 미존재 | 빈 집계 레코드 생성 (기존 리뷰 없으므로 0 값) | 낮음 |

### 4.2 마이그레이션 절차

#### Step 1: 재고 초기화 (inventory)

```sql
-- Phase 1 상품 옵션에서 inventory 레코드 생성
-- product_option 테이블 구조 확인 후 매핑
INSERT INTO inventory (product_id, option_id, sku, quantity, safety_stock, created_at, updated_at)
SELECT
    po.product_id,
    po.id AS option_id,
    CONCAT(p.code, '-', po.color_code, '-', po.size_code) AS sku,
    COALESCE(po.stock, 100) AS quantity,  -- 기본 재고 100 (확인 필요)
    10 AS safety_stock,
    NOW(6),
    NOW(6)
FROM product_option po
JOIN product p ON po.product_id = p.id
WHERE p.status = 'ACTIVE';
```

**주의사항**:
- Phase 1에서 재고 관리를 하지 않았다면, 초기 재고 수량을 판매자로부터 수집해야 한다.
- SKU 코드 생성 규칙은 실제 product_option 테이블 구조에 맞게 조정.

#### Step 2: Elasticsearch 벌크 인덱싱

```
POST /api/v1/search/reindex  (US-701 관리자 API)
```

- Phase 1 상품 전량을 ES에 인덱싱
- 예상 소요: 상품 500건 기준 약 5초

#### Step 3: 기존 주문 처리 정책

| 주문 상태 | 처리 방안 |
|-----------|----------|
| PENDING, PAID (미발송) | Phase 2 배송 흐름 적용. shipping 레코드 생성 대기 |
| 이미 배송 완료된 주문 (Phase 1에서 수동 처리) | shipping 레코드 미생성. Phase 2 이전 주문으로 구분 |
| CANCELLED | 변경 없음 |

#### Step 4: review_summary 초기화

```sql
-- 모든 활성 상품에 대해 빈 집계 레코드 생성
INSERT INTO review_summary (product_id, review_count, avg_rating, created_at, updated_at)
SELECT id, 0, 0.0, NOW(6), NOW(6)
FROM product
WHERE status = 'ACTIVE';
```

### 4.3 마이그레이션 실행 계획

| 단계 | 작업 | 예상 시간 | 롤백 |
|------|------|----------|------|
| 1 | Flyway 마이그레이션 (DDL) | 30초 | Flyway undo 또는 수동 DROP TABLE |
| 2 | inventory 초기 데이터 | 1분 | TRUNCATE inventory |
| 3 | ES 벌크 인덱싱 | 5분 | 인덱스 삭제 후 재생성 |
| 4 | review_summary 초기화 | 10초 | TRUNCATE review_summary |
| 5 | Feature Flag ON (Phase 2 기능 활성화) | 즉시 | Feature Flag OFF |
| **총 소요** | | **~7분** | |

---

## 5. 롤백 전략

### 5.1 배포 실패 시 Kafka 이벤트 정합성

Phase 2 서비스들은 Kafka를 통해 강하게 연결되어 있다. 배포 롤백 시 이벤트 정합성이 깨질 수 있는 시나리오:

#### 시나리오 분석

| 시나리오 | 위험도 | 영향 | 대응 |
|----------|--------|------|------|
| inventory-service 롤백, order-service 유지 | 높음 | 주문은 생성되지만 재고 미차감 -> 초과판매 | Consumer offset 롤백 불가. 보상 트랜잭션으로 수동 복구 |
| search-service 롤백 | 낮음 | 검색 인덱스 동기화 중단. 이전 데이터로 검색 | 복구 후 벌크 리인덱싱으로 해결 |
| shipping-service 롤백 | 중간 | 송장 등록 불가. 주문 상태 변경 이벤트 유실 | 주문 상태 수동 변경. Consumer 재처리 |
| review-service 롤백 | 낮음 | 리뷰 작성 불가. 포인트 적립 이벤트 유실 | 복구 후 이벤트 재처리 |

#### Kafka 이벤트 정합성 보장 전략

```
[배포 전]
1. Feature Flag로 신규 기능 비활성화
2. Kafka Consumer 정지 (graceful shutdown)
3. Consumer Lag 0 확인

[배포 중]
4. 롤링 업데이트 (zero-downtime)
5. Health Check 통과 후 트래픽 유입

[배포 후]
6. Consumer 재시작
7. Consumer Lag 모니터링
8. 이상 시 Feature Flag OFF -> 이전 버전 동작

[롤백 시]
9. Feature Flag OFF (즉시)
10. 이전 버전 재배포
11. Consumer offset 확인 + 미처리 이벤트 수동 재처리
12. 데이터 정합성 검증 스크립트 실행
```

### 5.2 Feature Flag 전략

Phase 2 전체 기능을 Feature Flag로 제어하여 배포와 기능 릴리즈를 분리한다.

#### Feature Flag 정의

| Flag Key | 설명 | 기본값 | 영향 범위 |
|----------|------|--------|----------|
| `PHASE2_SHIPPING_ENABLED` | 배송 도메인 전체 | OFF | shipping-service API, Kafka consumer |
| `PHASE2_INVENTORY_ENABLED` | 재고 도메인 전체 | OFF | inventory-service API, Kafka consumer |
| `PHASE2_SEARCH_ENABLED` | 검색 도메인 전체 | OFF | search-service API |
| `PHASE2_REVIEW_ENABLED` | 리뷰 도메인 전체 | OFF | review-service API, Kafka consumer |
| `INVENTORY_DISTRIBUTED_LOCK` | Redis 분산 락 | OFF | 분산 락 사용 여부. OFF 시 낙관적 락만 |
| `SEARCH_AUTOCOMPLETE` | 자동완성 | OFF | 자동완성 API |
| `SEARCH_POPULAR_KEYWORDS` | 인기 검색어 | OFF | 인기 검색어 API |
| `REVIEW_PHOTO_UPLOAD` | 포토 리뷰 | OFF | S3 업로드 |
| `REVIEW_SIZE_INFO` | 사이즈 후기 | OFF | 사이즈 정보 입력/조회 |
| `SHIPPING_AUTO_CONFIRM` | 자동 구매확정 | OFF | 배치 스케줄러 |

#### 릴리즈 단계

```
Sprint 5 배포 -> PHASE2_INVENTORY_ENABLED = ON
                 PHASE2_SHIPPING_ENABLED = ON (송장 등록만)

Sprint 6 배포 -> PHASE2_SHIPPING_ENABLED 전체 ON
                 SHIPPING_AUTO_CONFIRM = ON

Sprint 7 배포 -> PHASE2_SEARCH_ENABLED = ON
                 SEARCH_AUTOCOMPLETE = ON
                 SEARCH_POPULAR_KEYWORDS = ON

Sprint 8 배포 -> PHASE2_REVIEW_ENABLED = ON
                 REVIEW_PHOTO_UPLOAD = ON
                 REVIEW_SIZE_INFO = ON
```

### 5.3 DB 스키마 롤백

- Flyway forward-only 전략 유지. 롤백 시 호환성 있는 마이그레이션 추가.
- Phase 2 테이블은 Phase 1 테이블과 완전히 분리되어 있으므로 (FK 미사용), 롤백 시 Phase 2 테이블만 DROP 가능.
- 단, inventory 테이블에 기존 주문 매핑 데이터가 있으면 주의 필요.

---

## 6. 멱등성 요구사항

### 6.1 Kafka Consumer 중복 처리 방지

모든 Kafka Consumer에 대해 `processed_event` 테이블 기반 멱등성을 보장한다.

#### processed_event 테이블 설계

```sql
CREATE TABLE processed_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    event_id        VARCHAR(100)    NOT NULL COMMENT '이벤트 고유 ID (UUID)',
    event_type      VARCHAR(50)     NOT NULL COMMENT '이벤트 유형',
    source_service  VARCHAR(30)     NOT NULL COMMENT '발행 서비스',
    processed_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_event_type_processed (event_type, processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='처리 완료 이벤트 (멱등성)';
```

#### 처리 흐름

```
[Kafka Consumer]
  -> event_id 추출
  -> processed_event 테이블 조회 (SELECT WHERE event_id = ?)
  -> 이미 존재? -> SKIP (로그 기록)
  -> 미존재? -> 비즈니스 로직 실행 + processed_event INSERT (같은 트랜잭션)
  -> 커밋 후 offset 커밋
```

#### 서비스별 멱등성 요구사항

| Consumer | 이벤트 | 중복 처리 위험 | 멱등성 중요도 |
|----------|--------|--------------|-------------|
| inventory: order.created | 재고 이중 차감 | 높음 - 초과판매 | **Critical** |
| inventory: order.cancelled | 재고 이중 복구 | 중간 - 재고 부풀림 | **High** |
| inventory: return.approved | 재고 이중 복구 | 중간 | **High** |
| search: product.* | ES 이중 인덱싱 | 낮음 - 동일 데이터 덮어쓰기 | Low (자연적 멱등) |
| shipping: order.created | 배송 정보 이중 생성 | 중간 - UK 제약으로 방지 | Medium |
| member: review.created | 포인트 이중 적립 | 높음 - 금전적 손해 | **Critical** |

#### 이벤트 발행 측 멱등성

Kafka Producer도 멱등성을 보장해야 한다:

```yaml
# application.yml (각 서비스)
spring:
  kafka:
    producer:
      properties:
        enable.idempotence: true
        acks: all
        retries: 3
        max.in.flight.requests.per.connection: 5
```

### 6.2 API 재시도 안전성

| API | HTTP 메서드 | 멱등성 | 구현 방안 |
|-----|-----------|--------|----------|
| 재고 조회 | GET | 자연적 멱등 | - |
| 재고 차감 | POST | 비멱등 (위험) | referenceType + referenceId 기반 중복 체크 (UK 제약) |
| 송장 등록 | POST | 비멱등 (위험) | uk_order_id 유니크 제약으로 중복 방지 |
| 반품 신청 | POST | 비멱등 (위험) | order_id + order_item_id + status 조합 체크 |
| 교환 신청 | POST | 비멱등 (위험) | 동일 |
| 검색 | GET | 자연적 멱등 | - |
| 리뷰 작성 | POST | 비멱등 (위험) | uk_order_item (order_item_id) 유니크 제약으로 중복 방지 |
| 구매확정 | POST | 비멱등 (위험) | 주문 상태 체크 (이미 CONFIRMED면 무시) |
| 리인덱싱 | POST | 멱등 (덮어쓰기) | - |

#### 권장: Idempotency Key 헤더

```
POST /api/v1/shippings
Idempotency-Key: {client-generated-uuid}
```

- 클라이언트가 고유 키를 생성하여 헤더에 포함.
- 서버에서 Redis에 60초 TTL로 저장. 동일 키 재요청 시 이전 응답 반환.
- 네트워크 타임아웃 후 재시도 시 안전성 보장.

---

## 7. WMS(창고관리) 성능 요구사항

PRD Phase 2에서는 본격적인 WMS를 다루지 않지만, 재고 관리(inventory-service)가 WMS의 기초가 된다. 향후 확장을 대비한 성능 요구사항을 정의한다.

### 7.1 피킹 지시 동시 처리

PRD에는 피킹 지시가 명시되어 있지 않으나, US-501(송장 등록) + US-601(재고 차감) 흐름이 피킹의 전 단계이다.

#### 현재 Phase 2 범위

```
주문 접수 -> 재고 차감 -> 판매자 송장 등록 -> 배송
```

#### 향후 WMS 확장 시 피킹 흐름

```
주문 접수 -> 재고 예약 -> 피킹 지시 생성 -> 피킹 완료 -> 패킹 -> 송장 등록 -> 출고
```

#### 동시 처리 성능 목표 (향후)

| 항목 | 목표 | 비고 |
|------|------|------|
| 동시 피킹 지시 처리 | 50건/분 | 일 50건 주문 피크 시간대 집중 |
| 피킹 지시 생성 응답시간 | < 100ms | 재고 예약(분산 락) 포함 |
| 피킹 완료 → 출고 지연 | < 1초 | 상태 변경 이벤트 기반 |

#### Phase 2에서의 대비 설계

| 항목 | 현재 설계 | WMS 확장 대비 |
|------|----------|-------------|
| 재고 차감 시점 | 주문 생성 시 즉시 차감 | 향후 "예약(RESERVED)" 상태 추가 가능. inventory_history.change_type에 RESERVE 추가 |
| 재고 상태 | quantity만 관리 | 향후 available/reserved/damaged 분리 |
| 분산 락 범위 | SKU 단위 | WMS에서도 동일 SKU 단위 락 유지 |

### 7.2 출고 대기열 관리

#### 현재 Phase 2 범위

출고 대기열은 Kafka `order.created` 이벤트가 사실상의 출고 대기열 역할을 한다.

```
order.created -> inventory-service (재고 차감)
             -> shipping-service (배송 준비 상태)
```

#### 성능 고려사항

| 항목 | 현재 | 향후 WMS |
|------|------|---------|
| 대기열 | Kafka topic (order.created) | 별도 picking_queue 테이블 또는 Kafka topic |
| 처리 순서 | FIFO (Kafka 파티션 내 순서 보장) | 우선순위 기반 (급송, 일반, 예약) |
| 처리량 | 50건/일 | 1,000건/일 이상 시 별도 서비스 분리 |
| 모니터링 | Kafka consumer lag | 출고 대기 건수 대시보드, SLA 초과 알림 |

#### 권장: Phase 2에서 미리 설계할 것

1. **inventory_history.change_type 확장성**: 현재 DEDUCT/RESTORE/INBOUND/ADJUST. 향후 RESERVE/RELEASE/PICK/PACK 추가를 고려하여 VARCHAR(20) -> VARCHAR(30) 권장.
2. **shipping 상태 확장**: 현재 READY/IN_TRANSIT/DELIVERED. 향후 PICKING/PACKING/READY_TO_SHIP 추가를 고려.
3. **주문-재고-배송 상태 정합성 모니터링**: 주문은 PAID인데 재고 미차감, 또는 재고 차감됐는데 배송 미등록 상태가 장시간 지속되면 알림.

---

## 8. 추가 비기능 요구사항

### 8.1 모니터링 확장

기존 Prometheus + Grafana 인프라에 Phase 2 메트릭을 추가한다.

#### 추가 Alert Rule

```yaml
# Phase 2 추가 알림 규칙
- alert: InventoryOverselling
  expr: sum(inventory_deduct_failure_total{reason="INSUFFICIENT"}) > 10
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "재고 부족 차감 실패 10건 초과 (5분)"

- alert: ShippingAPIFailure
  expr: rate(shipping_carrier_api_failure_total[5m]) > 0.3
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "택배사 API 실패율 30% 초과"

- alert: ESIndexingLag
  expr: elasticsearch_indexing_lag_seconds > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "ES 인덱싱 지연 10초 초과 (목표: 3초)"

- alert: DistributedLockTimeout
  expr: rate(inventory_lock_timeout_total[5m]) > 0.1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "분산 락 타임아웃 비율 10% 초과"

- alert: ReviewSpamDetected
  expr: rate(review_created_total[1h]) by (member_id) > 10
  for: 0m
  labels:
    severity: warning
  annotations:
    summary: "회원 {{ $labels.member_id }} 리뷰 스팸 의심 (시간당 10건 초과)"
```

#### 커스텀 비즈니스 메트릭

| 메트릭 | 유형 | 라벨 | 용도 |
|--------|------|------|------|
| `inventory_deduct_total` | Counter | sku, result(success/failure) | 재고 차감 성공/실패 추적 |
| `inventory_lock_duration_seconds` | Histogram | sku | 분산 락 보유 시간 |
| `shipping_carrier_api_duration_seconds` | Histogram | carrier | 택배사 API 응답 시간 |
| `search_query_duration_seconds` | Histogram | query_type(keyword/filter/autocomplete) | 검색 쿼리 응답 시간 |
| `search_result_count` | Histogram | - | 검색 결과 건수 분포 |
| `review_created_total` | Counter | has_photo, has_size_info | 리뷰 유형별 작성 건수 |
| `es_indexing_lag_seconds` | Gauge | - | ES 인덱싱 지연 시간 |

### 8.2 로깅 전략

| 서비스 | 로깅 대상 | 레벨 | 보관 기간 |
|--------|----------|------|----------|
| inventory-service | 재고 차감/복구 전 과정 | INFO | 90일 |
| inventory-service | 분산 락 획득/해제/타임아웃 | WARN | 90일 |
| shipping-service | 택배사 API 요청/응답 | INFO | 30일 |
| shipping-service | 택배사 API 장애 | ERROR | 90일 |
| search-service | 검색 쿼리 (키워드, 필터, 소요시간) | INFO | 30일 |
| search-service | ES 인덱싱 실패 | ERROR | 90일 |
| review-service | 리뷰 작성/수정/삭제 | INFO | 90일 |
| 전 서비스 | Kafka 이벤트 발행/수신 | INFO | 30일 |
| 전 서비스 | processed_event 중복 감지 | WARN | 90일 |

### 8.3 Graceful Shutdown

모든 Phase 2 서비스는 안전한 종료를 보장해야 한다:

```yaml
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

- Kafka Consumer: 현재 처리 중인 메시지 완료 후 종료. `max.poll.records=10`으로 배치 크기 제한.
- Redis 분산 락: 락 보유 중 종료 시 leaseTime(3초) 후 자동 해제. 추가로 shutdown hook에서 명시적 해제.
- Elasticsearch 벌크 인덱싱: 진행 중인 벌크 요청 완료 후 종료.
- Spring Scheduler (자동 구매확정): `@Scheduled` 작업 완료 대기.

---

## 9. 요약: PRD Gap 및 권장사항

### 9.1 PRD에 명시되어 있으나 상세화가 필요한 항목

| 항목 | PRD 수준 | 보완 필요 |
|------|----------|----------|
| 분산 락 설계 | 락 키, 대기/유지 시간 명시 | 재시도 간격(backoff), watchdog, 모니터링 추가 |
| Redis 캐싱 | TTL 5분 명시 | stale-while-error, circuit breaker, 캐시 웜업 전략 |
| ES 인덱싱 지연 3초 | 목표 명시 | DLQ 재처리 정책, refresh interval 튜닝 |
| 동시성 테스트 100스레드 | 테스트 기준 명시 | 테스트 환경(로컬/CI), 허용 실패율 명시 |

### 9.2 PRD에 미명시된 중요 비기능 요구사항

| 항목 | 중요도 | 권장 |
|------|--------|------|
| Kafka Consumer 멱등성 (processed_event) | Critical | 재고/포인트 Consumer에 필수 적용 |
| API 멱등성 (Idempotency Key) | High | 결제 영향 API(재고 차감, 환불) 에 적용 |
| Feature Flag 전략 | High | 서비스별 + 기능별 2단계 플래그 |
| 내부 서비스 간 인증 | High | 서비스 토큰 or mTLS |
| 개인정보 마스킹 | Medium | 배송지, 리뷰 작성자, 신체 정보 |
| S3 이미지 접근 제어 | Medium | CloudFront Signed URL |
| Graceful Shutdown | Medium | Kafka Consumer, 분산 락 안전 해제 |
| 검색 Rate Limiting | Medium | IP/사용자 기준 제한 |
| inventory_history 파티셔닝 준비 | Low | 1년 후 100만건 초과 시 대비 |
| WMS 확장 대비 상태 설계 | Low | 재고 상태(available/reserved) 분리 고려 |

### 9.3 기술 부채 리스크

| 리스크 | 영향 | 발생 시점 | 대응 |
|--------|------|----------|------|
| Kafka 단일 브로커 | 메시지 유실 가능 | 프로덕션 배포 시 | replication.factor=3, min.insync.replicas=2 |
| ES 단일 노드 | 검색 서비스 SPOF | 상품 10만건 초과 | 클러스터 구성 (master 3, data 2+) |
| Redis 단일 인스턴스 | 캐시/락 SPOF | 프로덕션 배포 시 | Redis Sentinel 또는 Cluster |
| 트랜잭션 아웃박스 미적용 | DB 커밋 성공 + Kafka 발행 실패 시 정합성 깨짐 | 네트워크 이슈 시 | Transactional Outbox 패턴 도입 권장 |
| docker-compose 단일 호스트 | 확장 불가 | 트래픽 증가 시 | Kubernetes (EKS) 마이그레이션 |
