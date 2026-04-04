# Phase 2 QA 테스트 계획서

> 작성일: 2026-04-04
> 프로젝트: Closet E-commerce Phase 2 (배송 + 재고 + 검색 + 리뷰)
> 작성자: QA Lead
> 기간: Sprint 5~8 (8주)

---

## 1. 테스트 전략

### 1.1 테스트 유형별 비율

| 유형 | 비율 | 목적 | 도구 |
|------|------|------|------|
| **Unit Test** | 60% | 도메인 로직, 상태 전이, 유효성 검증 | Kotest (BehaviorSpec), MockK |
| **Integration Test** | 25% | 서비스 간 연동, DB/Redis/Kafka/ES 연동 | Kotest, TestContainers, REST Assured |
| **E2E Test** | 10% | 주문->재고->결제->배송->리뷰 전체 흐름 | REST Assured, TestContainers |
| **Performance Test** | 5% | 동시성, TPS, 응답시간 | k6, Kotest (StressTest) |

### 1.2 테스트 원칙

1. **TDD 기반**: Jira 티켓의 AC를 테스트 케이스로 먼저 작성하고 구현한다.
2. **Given/When/Then**: Kotest BehaviorSpec 패턴을 모든 테스트에 적용한다.
3. **멱등성 검증**: Kafka Consumer 테스트는 동일 이벤트 재전송 시 중복 처리가 없음을 확인한다.
4. **상태 전이 검증**: 모든 도메인 엔티티의 상태 전이 규칙을 단위 테스트로 검증한다.
5. **경계값 우선**: 기간(168시간), 수량(0/음수), 금액(0원/3,000원/6,000원) 등 경계값 테스트를 필수 포함한다.

### 1.3 품질 게이트

| 지표 | 기준 |
|------|------|
| 코드 커버리지 (Line) | >= 80% |
| 코드 커버리지 (Branch) | >= 70% |
| P0 테스트 통과율 | 100% |
| P1 테스트 통과율 | >= 95% |
| P2 테스트 통과율 | >= 90% |
| 성능 테스트 P95 기준 충족 | 100% |

---

## 2. 테스트 범위

### 2.1 배송 도메인 (shipping-service)

| US | 기능 | 우선순위 | 자동화 | 담당 | 비고 |
|----|------|---------|--------|------|------|
| US-501 | 송장 등록 + 주문 상태 변경 | P0 | O | BE QA | CarrierAdapter(Strategy) 패턴 검증 포함 |
| US-501 | 송장번호 형식 검증 `[A-Z]{0,4}[0-9]{10,15}` | P0 | O | BE QA | PD-08 확정 형식 |
| US-501 | 시스템 자동 채번 + 수동 입력 병행 | P0 | O | BE QA | PD-07 확정 |
| US-502 | 택배사 API 연동 배송 추적 | P0 | O | BE QA | Mock 택배사 4사(CJ/로젠/롯데/우체국) |
| US-502 | 배송 상태 매핑 (READY/IN_TRANSIT/DELIVERED) | P0 | O | BE QA | PD-10 Mock 서버 상태 매핑 |
| US-502 | Redis 캐싱 (TTL 5분) | P1 | O | BE QA | PD-41 확정 |
| US-502 | orderId 기반 배송 추적 API | P1 | O | BE QA | PD-44 확정 |
| US-503 | 자동 구매확정 (168시간) | P0 | O | BE QA | PD-16/PD-17 확정 |
| US-503 | 수동 구매확정 | P1 | O | BE QA | |
| US-503 | 반품/교환 진행 중 자동확정 제외 | P0 | O | BE QA | |
| US-503 | D-1 사전 알림 이벤트 발행 | P1 | O | BE QA | PD-45 확정 |
| US-504 | 반품 신청 (4개 사유) | P0 | O | BE QA | |
| US-504 | 반품 사유별 배송비 부담 | P0 | O | BE QA | PD-11 매핑 테이블 |
| US-504 | 반품 배송비 공제 후 환불 | P0 | O | BE QA | PD-12 확정 |
| US-504 | 반품 검수 (3영업일 내 승인/거절, 초과 시 자동승인) | P1 | O | BE QA | PD-13 확정 |
| US-504 | 반품 상태 흐름 전체 | P0 | O | BE QA | |
| US-504 | 재고 복구 이벤트 발행 | P0 | O | BE QA | |
| US-505 | 교환 신청 (동일 상품 동일 가격만) | P1 | O | BE QA | PD-14 확정 |
| US-505 | 교환 시 재고 선점/해제 | P1 | O | BE QA | |
| US-505 | 교환 배송비 (왕복 6,000원) | P1 | O | BE QA | PD-15 확정 |
| - | 주문 상태 머신 (PENDING->...->CONFIRMED) | P0 | O | BE QA | PD-09 확정 상태 머신 |

### 2.2 재고 도메인 (inventory-service)

| US | 기능 | 우선순위 | 자동화 | 담당 | 비고 |
|----|------|---------|--------|------|------|
| US-601 | SKU별 재고 관리 (3단 구조: total/available/reserved) | P0 | O | BE QA | PD-06/PD-18 확정 |
| US-601 | RESERVE -> DEDUCT -> RELEASE 3단계 | P0 | O | BE QA | PD-18 확정 |
| US-601 | All-or-Nothing 주문 거절 | P0 | O | BE QA | PD-19 확정 |
| US-601 | 재고 변경 이력 기록 | P1 | O | BE QA | |
| US-601 | 재고 음수 방지 | P0 | O | BE QA | |
| US-601 | 입고(INBOUND) API | P0 | O | BE QA | PD-42 확정 |
| US-601 | 15분 결제 미완료 시 자동 RELEASE | P0 | O | BE QA | PD-18 확정 |
| US-602 | Redis 분산 락 (Redisson) | P0 | O | BE QA | |
| US-602 | 낙관적 락 (@Version) 2차 방어 | P1 | O | BE QA | |
| US-602 | 동시성 100스레드 테스트 | P0 | O | BE QA | |
| US-602 | 락 순서 정렬 (데드락 방지) | P1 | O | BE QA | |
| US-603 | 안전재고 알림 (카테고리별 기본값) | P1 | O | BE QA | PD-20 확정 |
| US-603 | 중복 알림 방지 (Redis 24h) | P1 | O | BE QA | |
| US-603 | out_of_stock 이벤트 | P1 | O | BE QA | |
| US-604 | 재입고 알림 신청 (최대 50건) | P2 | O | BE QA | |
| US-604 | 재입고 시 WAITING -> NOTIFIED 전이 | P2 | O | BE QA | |
| US-604 | 90일 자동 만료 | P2 | O | BE QA | PD-21 확정 |

### 2.3 검색 도메인 (search-service)

| US | 기능 | 우선순위 | 자동화 | 담당 | 비고 |
|----|------|---------|--------|------|------|
| US-701 | Kafka CDC -> ES 인덱싱 | P1 | O | BE QA | PD-43 product 이벤트 발행 전제 |
| US-701 | 벌크 인덱싱 (10만개 5분 이내) | P1 | O | BE QA | PD-28 확정 |
| US-701 | DLQ 저장 + 재처리 (3회 지수 백오프) | P1 | O | BE QA | PD-31 확정 |
| US-701 | 인덱싱 지연 P95 3초 이내 | P1 | O | BE QA | PD-29 확정 |
| US-702 | nori 한글 형태소 검색 | P1 | O | BE QA | |
| US-702 | 유의어 처리 (DB + ES synonym filter) | P2 | O | BE QA | PD-24 확정 |
| US-702 | 오타 교정 (ES fuzzy, fuzziness=AUTO) | P2 | O | BE QA | PD-26 확정 |
| US-702 | 정렬 (관련도/최신/가격/인기) | P1 | O | BE QA | |
| US-702 | 인기순 복합 점수 (Sprint 7 고도화) | P2 | O | BE QA | PD-23 확정 |
| US-703 | 필터 (카테고리/브랜드/가격/색상/사이즈) | P2 | O | BE QA | |
| US-703 | facet count | P2 | O | BE QA | |
| US-704 | 자동완성 (P99 50ms 이내) | P2 | O | BE QA | PD-27 확정 |
| US-705 | 인기 검색어 (Redis Sorted Set, sliding window) | P2 | O | BE QA | PD-25 확정 |
| US-705 | 금칙어 필터링 (DB + 관리자 CRUD) | P2 | O | BE QA | PD-39 확정 |
| US-705 | Rate Limiting (IP 120/min, 사용자 60/min) | P1 | O | BE QA | PD-30 확정 |
| - | 최근 검색어 (Redis List) | P2 | O | BE QA | PD-48 확정 |

### 2.4 리뷰 도메인 (review-service)

| US | 기능 | 우선순위 | 자동화 | 담당 | 비고 |
|----|------|---------|--------|------|------|
| US-801 | 텍스트 + 포토 리뷰 작성 | P1 | O | BE QA | |
| US-801 | 리뷰 수정 (텍스트/이미지만, 별점 불가) | P1 | O | BE QA | PD-32 확정 |
| US-801 | 수정 이력 보존 (최대 3회) | P1 | O | BE QA | PD-32 확정 |
| US-801 | 이미지 리사이즈 (Thumbnailator, 400x400) | P2 | O | BE QA | PD-33 확정 |
| US-801 | 이미지 크기 제한 (5MB/장, 30MB/요청) | P1 | O | BE QA | PD-34 확정 |
| US-801 | 관리자 리뷰 숨김 (HIDDEN) | P1 | O | BE QA | PD-35 확정 |
| US-802 | 사이즈 후기 (키/몸무게/핏) | P2 | O | BE QA | |
| US-802 | 비슷한 체형 필터 (+-5 범위) | P2 | O | BE QA | |
| US-803 | 리뷰 포인트 적립 (100P/300P/350P) | P2 | O | BE QA | |
| US-803 | 일일 포인트 한도 5,000P (KST 00:00 리셋) | P2 | O | BE QA | PD-37 확정 |
| US-803 | 리뷰 삭제 시 포인트 회수 (마이너스 잔액 허용) | P2 | O | BE QA | PD-36 확정 |
| US-804 | 리뷰 집계 (평균 별점, 별점 분포, 사이즈 분포) | P2 | O | BE QA | |
| US-804 | 집계 Redis 캐싱 + 변경 시 갱신 | P2 | O | BE QA | |
| US-804 | ES 상품 인덱스 리뷰 집계 동기화 | P2 | O | BE QA | |
| - | review 테이블 rating TINYINT UNSIGNED | P1 | O | BE QA | PD-38 확정 |

### 2.5 크로스 도메인

| 시나리오 | 우선순위 | 자동화 | 담당 | 비고 |
|----------|---------|--------|------|------|
| 주문->재고RESERVE->결제->재고DEDUCT->배송->배송완료->구매확정->리뷰 | P0 | O | BE QA | Happy Path E2E |
| 반품신청->환불(결제금액-배송비)->재고복구 | P0 | O | BE QA | |
| 교환신청->재고예약->수거->재발송->완료 | P1 | O | BE QA | |
| 결제실패->재고RELEASE | P0 | O | BE QA | |
| 15분 TTL 만료->자동 RELEASE | P0 | O | BE QA | |
| 상품 CUD->Kafka->ES 인덱싱->검색 결과 반영 | P1 | O | BE QA | |
| 리뷰 작성->집계 갱신->ES 동기화 | P2 | O | BE QA | |
| 이벤트 멱등성 (Transactional Outbox + processed_event) | P0 | O | BE QA | PD-04 확정 |

---

## 3. 성능 테스트 시나리오

> PM 결정 기반: PD-22 (프로덕션 목표 100 TPS), PD-27 (자동완성 P99 50ms), PD-28 (벌크 10만개 5분), PD-29 (인덱싱 P95 3초)

### 3.1 재고 동시성 테스트

| 시나리오 | 목표 TPS | 동시 사용자 | 기대 P95 응답시간 | 도구 |
|----------|---------|------------|-----------------|------|
| 단일 SKU 동시 재고 차감 | 100 TPS | 100 | 200ms 이내 (P99) | k6 |
| 복수 SKU 동시 재고 차감 (3 SKU) | 100 TPS | 100 | 300ms 이내 | k6 |
| RESERVE -> DEDUCT 동시 실행 | 100 TPS | 100 | 200ms 이내 | k6 |
| Redis 분산 락 경합 (동일 SKU) | 100 TPS | 100 | 500ms 이내 | k6 |

**검증 항목:**
- 100스레드 동시 차감 후 재고 수량 정합성 = 초기재고 - (성공건수 * 차감수량)
- 오버셀링 0건
- 분산 락 타임아웃(5초) 초과 비율 < 1%

### 3.2 검색 성능 테스트

| 시나리오 | 목표 TPS | 동시 사용자 | 기대 P95 응답시간 | 도구 |
|----------|---------|------------|-----------------|------|
| 키워드 검색 (nori) | 50 req/s | 50 | 200ms 이내 | k6 |
| 필터 검색 (3개 필터 조합) | 50 req/s | 50 | 300ms 이내 | k6 |
| 자동완성 | 100 req/s | 100 | 50ms 이내 (P99) | k6 |
| 인기 검색어 조회 | 200 req/s | 200 | 10ms 이내 | k6 |
| 벌크 인덱싱 (100,000 상품) | - | - | 5분 이내 완료 | k6 |
| 단건 인덱싱 지연 | - | - | 3초 이내 (P95) | k6 |

### 3.3 배송 도메인 성능 테스트

| 시나리오 | 목표 TPS | 동시 사용자 | 기대 P95 응답시간 | 도구 |
|----------|---------|------------|-----------------|------|
| 송장 등록 | 50 TPS | 50 | 300ms 이내 | k6 |
| 배송 추적 조회 (캐시 hit) | 100 TPS | 100 | 50ms 이내 | k6 |
| 배송 추적 조회 (캐시 miss) | 50 TPS | 50 | 500ms 이내 | k6 |
| 반품 신청 | 30 TPS | 30 | 500ms 이내 | k6 |
| 자동 구매확정 배치 (1,000건) | - | - | 60초 이내 완료 | k6 |

### 3.4 리뷰 도메인 성능 테스트

| 시나리오 | 목표 TPS | 동시 사용자 | 기대 P95 응답시간 | 도구 |
|----------|---------|------------|-----------------|------|
| 리뷰 작성 (텍스트) | 10 TPS | 10 | 300ms 이내 | k6 |
| 리뷰 작성 (포토 5장) | 5 TPS | 5 | 2,000ms 이내 | k6 |
| 리뷰 목록 조회 | 50 TPS | 50 | 200ms 이내 | k6 |
| 리뷰 집계 조회 (캐시 hit) | 100 TPS | 100 | 30ms 이내 | k6 |

---

## 4. 회귀 테스트 범위

### 4.1 Phase 1 기능 중 Phase 2 영향을 받는 기능 목록

| Phase 1 서비스 | 영향 받는 기능 | Phase 2 변경 사유 | 회귀 테스트 항목 |
|---------------|--------------|-----------------|----------------|
| **closet-order** | OrderStatus 상태 전이 | PD-09: PREPARING/SHIPPED 상태 추가, 반품/교환 상태 전이 확장 | 기존 PENDING->STOCK_RESERVED->PAID->CONFIRMED 흐름이 깨지지 않는지 |
| **closet-order** | 주문 생성 | PD-18: RESERVE 단계 추가 (주문 생성 시 재고 예약) | 주문 생성 API의 기존 응답/동작 호환성 |
| **closet-order** | 주문 취소 | PD-18: RELEASE 단계 추가 (취소 시 재고 해제) | 기존 주문 취소 흐름 정상 동작 |
| **closet-order** | Kafka 이벤트 발행 | order.status.changed 이벤트 스키마 확장 | 기존 Consumer 호환성 |
| **closet-product** | 상품 CUD | PD-43: Kafka 이벤트 발행 추가 | 상품 CRUD API 성능/동작 영향 없음 확인 |
| **closet-product** | 품절 표시 | US-603: out_of_stock 이벤트 수신 시 품절 처리 | 기존 상품 조회 API에 품절 상태 반영 |
| **closet-payment** | 결제 API | PD-12: 부분 환불 API 추가 (POST /api/v1/payments/{id}/refund) | 기존 전체 취소 API 정상 동작 |
| **closet-member** | 포인트 | US-803: 리뷰 포인트 적립/회수 | 기존 포인트 적립/조회 API 정상 동작 |
| **closet-gateway** | 라우팅 | PD-05: 신규 포트 배정 (shipping=8088, inventory=8089, search=8086, review=8087) | 기존 서비스 라우팅 영향 없음 |
| **closet-gateway** | 인증 | PD-03: JWT claim에 role 추가, X-Internal-Api-Key 헤더 | 기존 인증 흐름 호환성 |
| **closet-gateway** | Rate Limiting | PD-30: 검색 Rate Limiting 추가 | 기존 API Rate Limiting 정책 영향 없음 |

### 4.2 회귀 테스트 자동화 범위

- Phase 1 전체 E2E 테스트 스위트를 CI/CD 파이프라인에 포함
- 특히 **주문 생성 -> 결제 -> 주문 완료** 핵심 흐름의 비파괴 검증
- Phase 1 API 응답 스키마 호환성 검증 (계약 테스트)

---

## 5. 테스트 환경

### 5.1 TestContainers 설정

```kotlin
// BaseIntegrationTest 패턴 (싱글턴 컨테이너 + Initializer)
abstract class BaseIntegrationTest {
    companion object {
        // MySQL 8.0
        val mysqlContainer = MySQLContainer("mysql:8.0").apply {
            withDatabaseName("closet_test")
            withUsername("test")
            withPassword("test")
            withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_unicode_ci"
            )
        }
        
        // Redis 7.0
        val redisContainer = GenericContainer("redis:7.0-alpine").apply {
            withExposedPorts(6379)
        }
        
        // Kafka (Confluent)
        val kafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
        ).apply {
            withEnv("KAFKA_NUM_PARTITIONS", "3") // PD-50 확정
        }
        
        // Elasticsearch (nori plugin)
        val elasticsearchContainer = ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
        ).apply {
            withEnv("discovery.type", "single-node")
            withEnv("xpack.security.enabled", "false")
            // nori 플러그인은 커스텀 이미지 또는 startup 스크립트로 설치
        }

        init {
            mysqlContainer.start()
            redisContainer.start()
            kafkaContainer.start()
            elasticsearchContainer.start()
        }
    }
}
```

### 5.2 서비스별 컨테이너 의존성

| 서비스 | MySQL | Redis | Kafka | ES |
|--------|-------|-------|-------|----|
| shipping-service | O | O | O | - |
| inventory-service | O | O | O | - |
| search-service | - | O | O | O |
| review-service | O | O | O | - |

### 5.3 테스트 데이터 관리

**원칙:**
- 각 테스트는 독립적이다. 테스트 간 데이터 의존 금지.
- `@BeforeEach`에서 테스트 데이터 세팅, `@AfterEach`에서 정리.
- Flyway 마이그레이션을 테스트 컨테이너에 적용하여 실제 스키마와 동일한 환경 보장.

**테스트 Fixture:**

| 도메인 | Fixture | 설명 |
|--------|---------|------|
| 배송 | `ShippingFixture` | 주문(PAID 상태), 택배사 코드, 송장번호 등 |
| 배송 | `ReturnFixture` | 배송 완료(delivered_at 설정) 주문, 반품 사유 |
| 배송 | `ExchangeFixture` | 교환 대상 주문, 교환 옵션(동일 가격) |
| 재고 | `InventoryFixture` | SKU, 3단 재고(total/available/reserved), 안전재고 |
| 검색 | `SearchFixture` | ES 인덱스 문서, 검색 키워드, 유의어 |
| 리뷰 | `ReviewFixture` | 구매확정 주문, 리뷰 텍스트, 이미지 파일 |

**Kafka 테스트 전략:**
- `@EmbeddedKafka` 대신 TestContainers Kafka 사용 (실제 환경 근사)
- Consumer 테스트: 이벤트 발행 -> Consumer 처리 -> DB 상태 검증
- 멱등성 테스트: 동일 eventId로 2회 발행 -> 1회만 처리됨 검증

**Redis 테스트 전략:**
- 분산 락: 멀티스레드 테스트로 락 경합 시뮬레이션
- 캐시: TTL 만료 후 캐시 미스 검증
- 포인트 한도: Redis 키 `review:daily_point:{memberId}` TTL 24시간 검증

---

## 6. 테스트 일정 (Sprint 5~8)

### Sprint 5 (Week 1~2): 재고 + 배송 기반

| 주차 | 테스트 활동 | 대상 | 산출물 |
|------|-----------|------|--------|
| Week 1 | Unit Test 작성 | US-601 재고 RESERVE/DEDUCT/RELEASE, US-602 분산 락 | 테스트 코드, 커버리지 리포트 |
| Week 1 | Integration Test 작성 | 재고 Kafka Consumer (order.created/cancelled) | 테스트 코드 |
| Week 2 | Unit Test 작성 | US-501 송장 등록, 상태 전이, 송장번호 검증 | 테스트 코드 |
| Week 2 | Integration Test 작성 | 배송 + 택배사 Mock API 연동 | 테스트 코드 |
| Week 2 | 동시성 테스트 | US-602 100스레드 재고 차감 | 성능 리포트 |

### Sprint 6 (Week 3~4): 반품 + 배송 추적 + 알림

| 주차 | 테스트 활동 | 대상 | 산출물 |
|------|-----------|------|--------|
| Week 3 | Unit Test 작성 | US-504 반품 상태 전이, 배송비 계산, 환불 금액 | 테스트 코드 |
| Week 3 | Integration Test 작성 | 반품 -> 결제 취소 -> 재고 복구 E2E | 테스트 코드 |
| Week 3 | Unit Test 작성 | US-502 배송 추적, 상태 매핑, 캐싱 | 테스트 코드 |
| Week 4 | Unit Test 작성 | US-503 자동 구매확정 배치, 168시간 경계 | 테스트 코드 |
| Week 4 | Unit Test 작성 | US-603 안전재고 알림, US-604 재입고 알림 | 테스트 코드 |
| Week 4 | 회귀 테스트 | Phase 1 주문/결제 흐름 | 회귀 리포트 |

### Sprint 7 (Week 5~6): 검색 + 리뷰 + 교환

| 주차 | 테스트 활동 | 대상 | 산출물 |
|------|-----------|------|--------|
| Week 5 | Unit Test 작성 | US-701 인덱싱, US-702 검색, US-703 필터 | 테스트 코드 |
| Week 5 | Integration Test 작성 | Kafka -> ES 인덱싱, nori 검색 | 테스트 코드 |
| Week 5 | Unit Test 작성 | US-505 교환 신청, 재고 선점 | 테스트 코드 |
| Week 6 | Unit Test 작성 | US-801 리뷰 작성/수정/삭제, US-802 사이즈 후기 | 테스트 코드 |
| Week 6 | Integration Test 작성 | 리뷰 -> 포인트 적립 -> 집계 갱신 | 테스트 코드 |
| Week 6 | 성능 테스트 (검색) | 자동완성 P99 50ms, 벌크 인덱싱 10만개 5분 | 성능 리포트 |

### Sprint 8 (Week 7~8): E2E + 성능 + 회귀

| 주차 | 테스트 활동 | 대상 | 산출물 |
|------|-----------|------|--------|
| Week 7 | E2E 테스트 | 주문->재고->결제->배송->구매확정->리뷰 전체 흐름 | 테스트 코드 |
| Week 7 | E2E 테스트 | 반품->환불->재고복구, 교환->재고예약->출고 | 테스트 코드 |
| Week 7 | Unit Test 작성 | US-704 자동완성, US-705 인기검색어, US-803 포인트, US-804 집계 | 테스트 코드 |
| Week 8 | 성능 테스트 (전체) | 재고 100TPS, 검색 50TPS, 배송 추적 100TPS | 최종 성능 리포트 |
| Week 8 | 전체 회귀 테스트 | Phase 1 + Phase 2 전체 | 최종 회귀 리포트 |
| Week 8 | 품질 게이트 검증 | 커버리지, 통과율, 성능 기준 | QA 사인오프 리포트 |

---

## 7. 리스크 및 완화 계획

| 리스크 | 영향 | 완화 계획 |
|--------|------|----------|
| TestContainers ES + nori 플러그인 설정 지연 | 검색 테스트 블로킹 | Sprint 5에 ES 컨테이너 환경 선행 구축 |
| Phase 1 서비스 수정(product Kafka 발행, payment 부분 환불) 지연 | 통합 테스트 블로킹 | Mock으로 먼저 테스트, 수정 완료 후 실 연동 테스트 |
| 반품/교환 상태 전이 복잡도 | 테스트 케이스 폭발 | 상태 전이 다이어그램 기반 체계적 경로 도출 |
| Kafka 이벤트 순서 보장 이슈 | 데이터 정합성 | eventId 기반 멱등성 테스트 강화 |
| 성능 테스트 환경과 프로덕션 차이 | 성능 수치 신뢰도 | k6 설정을 프로덕션 유사 환경(CPU/메모리 제한)으로 조정 |
