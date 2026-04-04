# Phase 2 BE 테스트 케이스

> 작성일: 2026-04-04
> 입력: qa_test_cases.md (149건), phase2-logistics_architecture.md, pm_decisions.md
> 패턴: Kotest BehaviorSpec (Given/When/Then), MockK, Testcontainers
> 총 BE TC: 130건

---

## 1. closet-inventory (재고 서비스) -- 38건

### 1.1 Unit: InventoryTest -- 도메인 엔티티 (14건)

> 파일: `closet-inventory/src/test/kotlin/com/closet/inventory/domain/InventoryTest.kt`
> QA 매핑: INV-001~012

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-INV-001 | RESERVE 정상 -- available 감소, reserved 증가 | Inventory(total=100, available=100, reserved=0) | reserve(quantity=2) | available=98, reserved=2 | Unit | P0 |
| BE-INV-002 | DEDUCT 정상 -- reserved 감소 | Inventory(total=100, available=98, reserved=2) | deduct(quantity=2) | reserved=0, available=98 유지 | Unit | P0 |
| BE-INV-003 | RELEASE 정상 -- reserved 감소, available 복구 | Inventory(total=100, available=98, reserved=2) | release(quantity=2) | reserved=0, available=100 | Unit | P0 |
| BE-INV-004 | RESTORE 정상 -- 반품 시 total, available 증가 | Inventory(total=98, available=98, reserved=0) | restore(quantity=2) | total=100, available=100 | Unit | P0 |
| BE-INV-005 | INBOUND 정상 -- 입고 시 total, available 증가 | Inventory(total=100, available=100, reserved=0) | inbound(quantity=50) | total=150, available=150 | Unit | P0 |
| BE-INV-006 | RESERVE 시 재고 부족 예외 | Inventory(total=100, available=1, reserved=99) | reserve(quantity=2) | IllegalArgumentException("재고 부족") | Unit | P0 |
| BE-INV-007 | available 음수 방지 | Inventory(total=0, available=0, reserved=0) | reserve(quantity=1) | IllegalArgumentException("가용 재고가 부족합니다") | Unit | P0 |
| BE-INV-008 | reserved 음수 방지 | Inventory(total=100, available=100, reserved=0) | deduct(quantity=1) | IllegalArgumentException("예약 수량이 부족합니다") | Unit | P0 |
| BE-INV-009 | 불변 조건: total == available + reserved | Inventory(total=100, available=70, reserved=30) | reserve(5) / deduct(5) / release(5) 각각 후 | 항상 total == available + reserved | Unit | P0 |
| BE-INV-010 | RESERVE 시 available 정확히 0 도달 | Inventory(total=2, available=2, reserved=0) | reserve(quantity=2) | available=0, reserved=2, isOutOfStock()==true | Unit | P0 |
| BE-INV-011 | 안전재고 판단 -- 이하일 때 | Inventory(available=9, safetyStock=10) | isBelowSafetyStock() | true | Unit | P1 |
| BE-INV-012 | 안전재고 판단 -- 초과일 때 | Inventory(available=11, safetyStock=10) | isBelowSafetyStock() | false | Unit | P1 |
| BE-INV-013 | 안전재고 판단 -- 경계값 동일 | Inventory(available=10, safetyStock=10) | isBelowSafetyStock() | true (이하이므로) | Unit | P1 |
| BE-INV-014 | 카테고리별 안전재고 기본값 적용 | Inventory 생성 시 category별 | Inventory.createWithDefaultSafetyStock(category=TOP) | safetyStock=10 (PD-20: 상의/하의=10, 아우터=5, 신발=8, 액세서리=15) | Unit | P1 |

### 1.2 Unit: InventoryServiceTest -- 서비스 레이어 (10건)

> 파일: `closet-inventory/src/test/kotlin/com/closet/inventory/application/InventoryServiceTest.kt`
> Mock: InventoryRepository, InventoryHistoryRepository, InventoryLockService, KafkaTemplate
> QA 매핑: INV-001~012, INV-019~022

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-INV-015 | 재고 RESERVE -- 정상 흐름, 이력 기록 | lockService.reserveStock 성공 Mock | reserveStock(sku, quantity=2, orderId=1L) | inventory_history(type=RESERVE) 저장, 결과 반환 | Unit | P0 |
| BE-INV-016 | 복수 SKU All-or-Nothing 전체 거절 | SKU-A: available=10, SKU-B: available=0 | reserveStockBatch([SKU-A:1, SKU-B:1]) | 전체 거절, SKU-A도 RESERVE 안 됨, inventory.insufficient 이벤트 발행 (PD-19) | Unit | P0 |
| BE-INV-017 | 복수 SKU 락 순서 -- SKU 코드 오름차순 정렬 | SKU-B, SKU-A 순서로 요청 | reserveStockBatch([SKU-B, SKU-A]) | 락 획득 순서: SKU-A -> SKU-B (데드락 방지) | Unit | P1 |
| BE-INV-018 | 재고 차감 후 안전재고 이하 시 이벤트 발행 | available=11, safetyStock=10 | reserve(quantity=2) 후 available=9 | kafkaTemplate.send("inventory.low_stock") 호출 verify | Unit | P1 |
| BE-INV-019 | 재고 차감 후 품절 시 이벤트 발행 | available=1 | reserve(quantity=1) 후 available=0 | kafkaTemplate.send("inventory.out_of_stock") 호출 verify | Unit | P1 |
| BE-INV-020 | 24시간 내 중복 안전재고 알림 방지 | Redis 키 `inventory:low_stock:alert:{sku}` 존재 | reserve 후 안전재고 이하 | 알림 미발행 (Redis 키로 중복 판단) | Unit | P1 |
| BE-INV-021 | 입고 시 재입고 알림 트리거 | 기존 available=0, WAITING 알림 3건 존재 | inbound(quantity=50) | available=50, restock_notification 이벤트 3건 발행, 알림 status=NOTIFIED | Unit | P0 |
| BE-INV-022 | 안전재고 수동 오버라이드 | safetyStock=10인 Inventory | updateSafetyStock(inventoryId, 20) | safetyStock=20으로 변경 | Unit | P1 |
| BE-INV-023 | 재입고 알림 신청 -- 품절 상품만 | available > 0인 Inventory | createRestockNotification(productId, optionId, memberId) | BusinessException("품절 상품만 신청 가능") | Unit | P2 |
| BE-INV-024 | 재입고 알림 50건 초과 제한 | 회원의 WAITING 알림 50건 존재 | createRestockNotification() | BusinessException("최대 50건 초과") | Unit | P2 |

### 1.3 Integration: InventoryKafkaConsumerTest -- Kafka 이벤트 처리 (7건)

> 파일: `closet-inventory/src/test/kotlin/com/closet/inventory/infrastructure/kafka/InventoryKafkaConsumerTest.kt`
> 의존: Testcontainers (MySQL, Redis, Kafka)
> QA 매핑: INV-001~004, INV-007, INV-010

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-INV-025 | order.created 이벤트 수신 -> 재고 RESERVE | Inventory(available=100), Kafka order.created 이벤트 (quantity=2) | Consumer 수신 | available=98, reserved=2, inventory_history(RESERVE) 저장 | Integration | P0 |
| BE-INV-026 | order.cancelled 이벤트 수신 -> 재고 RELEASE | reserved=2 상태 Inventory, order.cancelled 이벤트 | Consumer 수신 | reserved=0, available 복구, inventory_history(RELEASE) 저장 | Integration | P0 |
| BE-INV-027 | return.approved 이벤트 수신 -> 재고 RESTORE | total=98 상태 Inventory, return.approved 이벤트 (quantity=2) | Consumer 수신 | total=100, available 증가, inventory_history(RETURN_RESTORE) 저장 | Integration | P0 |
| BE-INV-028 | order.created 이벤트 재고 부족 -> insufficient 발행 | Inventory(available=1), order.created(quantity=2) | Consumer 수신 | inventory.insufficient 이벤트 발행, 부족 SKU 정보 포함 | Integration | P0 |
| BE-INV-029 | 이벤트 멱등성 -- 동일 eventId 중복 수신 스킵 | processed_event 테이블에 eventId 존재, 동일 eventId로 order.created 재전송 | Consumer 수신 | processed_event UNIQUE KEY로 2회차 스킵, 재고 변동 없음 (PD-04) | Integration | P0 |
| BE-INV-030 | Outbox 패턴 -- 재고 차감 + outbox INSERT 동일 트랜잭션 | 재고 차감 요청 | 트랜잭션 실행 | inventory UPDATE + inventory_history INSERT + outbox_event INSERT 모두 COMMIT (PD-51) | Integration | P0 |
| BE-INV-031 | 15분 TTL 만료 자동 RELEASE | RESERVE 후 reservationExpiresAt 경과 | TTL 만료 스케줄러 실행 | reserved 복구, available 증가, 주문 자동 취소 (PD-18) | Integration | P0 |

### 1.4 Integration: RedissonLockTest -- 동시성 (7건)

> 파일: `closet-inventory/src/test/kotlin/com/closet/inventory/infrastructure/lock/RedissonLockTest.kt`
> 의존: Testcontainers (MySQL, Redis)
> QA 매핑: INV-013~018

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-INV-032 | 분산 락 기본 동작 -- 락 획득/해제 | Inventory(available=100), 단일 스레드 | reserveStock(sku, quantity=1) | 락 획득(inventory:lock:{sku}) -> 차감 -> 락 해제, available=99 | Integration | P0 |
| BE-INV-033 | 100스레드 동시 차감 -- 정합성 (재고 충분) | Inventory(available=100), 100스레드 동시 차감 (각 1개) | CountDownLatch로 동시 실행 | 최종 available=0, 성공 100건, 실패 0건, 오버셀링 0건 (PD-22) | Integration | P0 |
| BE-INV-034 | 100스레드 동시 차감 -- 재고 부족 (50개만) | Inventory(available=50), 100스레드 동시 차감 (각 1개) | CountDownLatch로 동시 실행 | 성공 50건, 실패 50건(재고 부족), 최종 available=0 | Integration | P0 |
| BE-INV-035 | 락 획득 실패 -- 3초 타임아웃 | 다른 스레드가 락 5초 이상 점유 | reserveStock() 시도 | BusinessException(LOCK_ACQUISITION_FAILED) 발생 | Integration | P1 |
| BE-INV-036 | 복수 SKU 락 순서 정렬 -- 데드락 방지 | SKU-A, SKU-B 동시 주문 (Thread1: A,B / Thread2: B,A) | 동시 실행 | 둘 다 SKU 코드 오름차순 락 획득, 데드락 미발생, 정상 완료 | Integration | P1 |
| BE-INV-037 | Watch Dog 자동 갱신 -- leaseTime=-1 | 락 보유 중 처리 시간이 기본 lock watchdog timeout 초과 | 장시간 작업 시뮬레이션 | 락 자동 갱신, 작업 완료까지 유지 | Integration | P1 |
| BE-INV-038 | 낙관적 락(@Version) 최종 방어 | 분산 락 없이 직접 DB 동시 업데이트 | 2스레드 동시 save() | OptimisticLockingFailureException 발생 (1건은 실패) | Integration | P1 |

---

## 2. closet-shipping (배송 서비스) -- 36건

### 2.1 Unit: ShipmentTest -- 도메인 엔티티 상태 전이 (8건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/domain/ShipmentTest.kt`
> QA 매핑: SH-001~013, SH-017~019

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-001 | ShippingStatus READY -> IN_TRANSIT 전이 가능 | ShippingStatus.READY | canTransitionTo(IN_TRANSIT) | true | Unit | P0 |
| BE-SH-002 | ShippingStatus IN_TRANSIT -> DELIVERED 전이 가능 | ShippingStatus.IN_TRANSIT | canTransitionTo(DELIVERED) | true | Unit | P0 |
| BE-SH-003 | ShippingStatus DELIVERED -> 어디로든 전이 불가 | ShippingStatus.DELIVERED | canTransitionTo(READY), canTransitionTo(IN_TRANSIT) | 모두 false | Unit | P0 |
| BE-SH-004 | ShippingStatus READY -> DELIVERED 직접 전이 불가 | ShippingStatus.READY | canTransitionTo(DELIVERED) | false | Unit | P0 |
| BE-SH-005 | Mock 택배사 상태 매핑 -- ACCEPTED -> READY | carrierStatus = "ACCEPTED" | ShippingStatus.fromCarrierStatus("ACCEPTED") | READY (PD-10) | Unit | P0 |
| BE-SH-006 | Mock 택배사 상태 매핑 -- OUT_FOR_DELIVERY -> IN_TRANSIT | carrierStatus = "OUT_FOR_DELIVERY" | ShippingStatus.fromCarrierStatus("OUT_FOR_DELIVERY") | IN_TRANSIT (PD-10) | Unit | P0 |
| BE-SH-007 | Mock 택배사 상태 매핑 -- 알 수 없는 상태 | carrierStatus = "UNKNOWN" | ShippingStatus.fromCarrierStatus("UNKNOWN") | IllegalArgumentException | Unit | P1 |
| BE-SH-008 | 송장번호 형식 검증 -- 유효 형식들 | "1234567890"(10자리), "123456789012345"(15자리), "CJ1234567890"(prefix) | validateTrackingNumber() | 모두 true (PD-08: `[A-Z]{0,4}[0-9]{10,15}`) | Unit | P0 |

### 2.2 Unit: ReturnRequestTest -- 반품 도메인 (6건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/domain/ReturnRequestTest.kt`
> QA 매핑: SH-033~045

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-009 | ReturnStatus 정상 전이 전체 흐름 | REQUESTED | REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> INSPECTING -> APPROVED -> COMPLETED | 모든 전이 성공 | Unit | P0 |
| BE-SH-010 | ReturnStatus INSPECTING -> REJECTED 전이 가능 | ReturnStatus.INSPECTING | canTransitionTo(REJECTED) | true | Unit | P0 |
| BE-SH-011 | ReturnStatus 터미널 상태 검증 | COMPLETED, REJECTED | isTerminal() | 둘 다 true | Unit | P0 |
| BE-SH-012 | 반품 7일(168시간) 기한 초과 검증 | deliveredAt = now - 169시간 | ReturnRequest.validateDeadline(deliveredAt) | IllegalArgumentException("반품 기한 만료") | Unit | P0 |
| BE-SH-013 | 반품 168시간 직전 (167시간 59분) 유효 | deliveredAt = now - 167시간 59분 | ReturnRequest.validateDeadline(deliveredAt) | 정상 통과 | Unit | P0 |
| BE-SH-014 | 반품 사유별 배송비 부담 산정 | DEFECTIVE, WRONG_ITEM, SIZE_MISMATCH, CHANGE_OF_MIND | ReturnRequest.calculateShippingFee(reason) | DEFECTIVE=0/SELLER, WRONG_ITEM=0/SELLER, SIZE_MISMATCH=3000/BUYER, CHANGE_OF_MIND=3000/BUYER (PD-11) | Unit | P0 |

### 2.3 Unit: ExchangeRequestTest -- 교환 도메인 (4건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/domain/ExchangeRequestTest.kt`
> QA 매핑: SH-046~053

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-015 | ExchangeStatus 정상 전이 전체 흐름 | REQUESTED | REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> RESHIPPING -> COMPLETED | 모든 전이 성공 | Unit | P1 |
| BE-SH-016 | ExchangeStatus 터미널 상태 검증 | COMPLETED, REJECTED | isTerminal() | 둘 다 true | Unit | P1 |
| BE-SH-017 | 교환 시 동일 가격만 허용 | originalOption.price=30000, exchangeOption.price=35000 | ExchangeRequest.validatePriceMatch() | IllegalArgumentException("동일 가격만 교환 가능", PD-14) | Unit | P1 |
| BE-SH-018 | 교환 왕복 배송비 -- 불량 사유 판매자 부담 | reason=DEFECTIVE | ExchangeRequest.calculateShippingFee() | shippingFee=0, payer=SELLER | Unit | P1 |

### 2.4 Unit: ShippingServiceTest -- 서비스 레이어 (8건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/application/ShippingServiceTest.kt`
> Mock: ShipmentRepository, CarrierAdapterFactory, KafkaTemplate, RedisTemplate
> QA 매핑: SH-001~010, SH-014~023

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-019 | 송장 등록 성공 -- 시스템 자동 채번 | PAID 상태 주문, CJ CarrierAdapter Mock | createShipment(orderId, carrier=CJ) | Shipment 생성(status=READY), CarrierAdapter.registerShipment() 호출, 주문 상태 PREPARING 전이 이벤트 | Unit | P0 |
| BE-SH-020 | 송장 등록 성공 -- 판매자 수동 입력 | PAID 상태 주문, trackingNumber="1234567890123" | createShipment(orderId, carrier, trackingNumber) | 입력된 trackingNumber 그대로 저장, CarrierAdapter 호출 생략 | Unit | P0 |
| BE-SH-021 | 중복 송장 등록 거절 | 이미 orderId에 Shipment 존재 | createShipment(동일 orderId) | BusinessException(DUPLICATE_SHIPMENT) | Unit | P0 |
| BE-SH-022 | 잘못된 택배사 코드 거절 | carrier="INVALID" | createShipment(orderId, carrier="INVALID") | BusinessException(UNSUPPORTED_CARRIER) | Unit | P1 |
| BE-SH-023 | 배송 추적 조회 -- 캐시 miss, Mock API 호출 | Redis 캐시 비어있음, CarrierAdapter Mock 응답 | getTrackingInfo(shippingId) | Mock API 호출 1회, TrackingLog 저장, Redis 캐시 저장(TTL 5분) | Unit | P0 |
| BE-SH-024 | 배송 추적 조회 -- 캐시 hit, API 미호출 | Redis 캐시에 추적 정보 존재 (5분 이내) | getTrackingInfo(shippingId) | Redis에서 반환, CarrierAdapter 호출 0회 (PD-41) | Unit | P1 |
| BE-SH-025 | 택배사 API 장애 시 캐시 폴백 | Redis 캐시에 이전 결과 존재, CarrierAdapter 500 에러 | getTrackingInfo(shippingId) | 캐시된 정보 반환, ERROR 로깅 (PD-40) | Unit | P0 |
| BE-SH-026 | 다른 판매자의 주문에 송장 등록 거절 | sellerId=1 인증, 주문의 sellerId=2 | createShipment(orderId) | BusinessException(FORBIDDEN) | Unit | P0 |

### 2.5 Integration: ShippingTrackingTest -- 배송 추적 Redis 캐싱 (3건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/infrastructure/tracking/ShippingTrackingTest.kt`
> 의존: Testcontainers (MySQL, Redis), MockServer (Mock 택배사)
> QA 매핑: SH-014~021

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-027 | Redis 캐시 TTL 5분 만료 후 API 재호출 | 캐시 저장 후 5분 경과 (Redis TTL 만료) | getTrackingInfo(shippingId) | CarrierAdapter 재호출, 새 결과 캐시 저장 | Integration | P1 |
| BE-SH-028 | 배송 상태 DELIVERED 감지 -> 주문 상태 갱신 이벤트 | Shipment(status=IN_TRANSIT), Mock API 응답 DELIVERED | 배송 추적 폴링 실행 | Shipment.status=DELIVERED, deliveredAt 설정, shipping.status.changed Kafka 이벤트 발행 | Integration | P0 |
| BE-SH-029 | ShippingTrackingLog에 원본 상태 저장 | Mock API 응답 status=OUT_FOR_DELIVERY | 배송 추적 조회 | ShippingTrackingLog(carrierStatus=OUT_FOR_DELIVERY, mappedStatus=IN_TRANSIT) 저장 (PD-10) | Integration | P0 |

### 2.6 Integration: AutoConfirmBatchTest -- 자동 구매확정 배치 (7건)

> 파일: `closet-shipping/src/test/kotlin/com/closet/shipping/infrastructure/batch/AutoConfirmBatchTest.kt`
> 의존: Testcontainers (MySQL, Kafka)
> QA 매핑: SH-024~032

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SH-030 | 자동 구매확정 -- 168시간 경과 건 CONFIRMED | delivered_at <= now - 168시간인 주문 3건 | 자동 구매확정 배치 실행 | 3건 모두 CONFIRMED, 각 건 order.confirmed Kafka 이벤트 발행 (PD-17) | Integration | P0 |
| BE-SH-031 | 자동 구매확정 -- 반품 진행 중 건 스킵 | delivered_at 168시간 초과, 반품 status=REQUESTED | 자동 구매확정 배치 | 해당 건 CONFIRMED 미전이 (PD-16) | Integration | P0 |
| BE-SH-032 | 자동 구매확정 -- 교환 진행 중 건 스킵 | delivered_at 168시간 초과, 교환 status=PICKUP_SCHEDULED | 자동 구매확정 배치 | 해당 건 스킵 | Integration | P0 |
| BE-SH-033 | 168시간 정확히 경과 -- 경계값 포함 | delivered_at = now - INTERVAL 168 HOUR (정확히) | 자동 구매확정 배치 | CONFIRMED (경계값 포함) | Integration | P0 |
| BE-SH-034 | 167시간 59분 경과 -- 미달 스킵 | delivered_at = now - 167시간 59분 | 자동 구매확정 배치 | 스킵 (미도달) | Integration | P0 |
| BE-SH-035 | D-1 사전 알림 이벤트 발행 | delivered_at = now - 144시간 (D-1 시점) | D-1 알림 배치 실행 | Kafka confirm.reminder 이벤트 발행 (PD-45) | Integration | P1 |
| BE-SH-036 | 반품 검수 3영업일 초과 자동 승인 | ReturnRequest(status=INSPECTING), inspectionStartedAt + 3영업일 경과 | 검수 자동 승인 배치 | status=APPROVED, 환불 + 재고 복구 이벤트 발행 (PD-13) | Integration | P1 |

---

## 3. closet-search (검색 서비스) -- 28건

### 3.1 Unit: SearchServiceTest -- 쿼리 빌드/필터/정렬 (14건)

> 파일: `closet-search/src/test/kotlin/com/closet/search/application/SearchServiceTest.kt`
> Mock: ElasticsearchClient, RedisTemplate
> QA 매핑: SE-008~025

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SE-001 | 키워드 검색 쿼리 빌드 -- multi_match | keyword = "반팔 티셔츠" | buildSearchQuery(keyword) | multi_match 쿼리 생성 (fields: name, brand, description, tags) | Unit | P1 |
| BE-SE-002 | 정렬 LATEST -- createdAt desc | sort = LATEST | buildSortQuery(sort) | SortOptions(createdAt, DESC) 생성 | Unit | P1 |
| BE-SE-003 | 정렬 PRICE_ASC -- price asc | sort = PRICE_ASC | buildSortQuery(sort) | SortOptions(price, ASC) 생성 | Unit | P1 |
| BE-SE-004 | 정렬 PRICE_DESC -- price desc | sort = PRICE_DESC | buildSortQuery(sort) | SortOptions(price, DESC) 생성 | Unit | P1 |
| BE-SE-005 | 정렬 POPULAR -- popularityScore desc | sort = POPULAR | buildSortQuery(sort) | SortOptions(popularityScore, DESC) 생성 | Unit | P1 |
| BE-SE-006 | 카테고리 필터 적용 | category = "TOP" | buildFilterQuery(category=TOP) | term 필터(category=TOP) 추가 | Unit | P2 |
| BE-SE-007 | 브랜드 복수 필터 -- terms 쿼리 | brands = ["Nike", "Adidas"] | buildFilterQuery(brands=["Nike","Adidas"]) | terms 필터(brand.keyword: [Nike, Adidas]) 추가 | Unit | P2 |
| BE-SE-008 | 가격 범위 필터 | minPrice=10000, maxPrice=30000 | buildFilterQuery(minPrice, maxPrice) | range 필터(price: gte=10000, lte=30000) 추가 | Unit | P2 |
| BE-SE-009 | 복수 필터 AND 조합 | category=TOP, color=BLACK, size=M | buildFilterQuery(category, color, size) | bool(must: [term(category), term(color), term(size)]) 조합 | Unit | P2 |
| BE-SE-010 | 검색 결과 없음 응답 | ES 반환 hits=0 | searchProducts(keyword="없는상품") | SearchResponse(totalCount=0, items=[]) 반환 | Unit | P1 |
| BE-SE-011 | 빈 검색어 예외 | keyword = "" | searchProducts(keyword="") | BusinessException("검색어를 입력해주세요") | Unit | P1 |
| BE-SE-012 | 페이지 크기 최대값 100 제한 | size = 101 | searchProducts(size=101) | size=100으로 자동 조정 또는 BusinessException | Unit | P2 |
| BE-SE-013 | 오타 교정 -- fuzzy 쿼리 빌드 | keyword = "반팔 티셔추" | buildSearchQuery(keyword) | fuzziness=AUTO 옵션 포함 (PD-26) | Unit | P2 |
| BE-SE-014 | 인기순 복합 점수 계산 | salesCount=100, reviewCount=50, avgRating=4.5, viewCount=1000 | calculatePopularityScore() | score = 100*0.4 + 50*0.3 + 4.5*0.2 + 1000*0.1 = 40+15+0.9+100 = 155.9 (PD-23) | Unit | P1 |

### 3.2 Unit: PopularKeywordServiceTest -- 인기/최근 검색어 (5건)

> 파일: `closet-search/src/test/kotlin/com/closet/search/application/PopularKeywordServiceTest.kt`
> Mock: RedisTemplate (StringRedisTemplate)
> QA 매핑: SE-030~037

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SE-015 | 검색 시 Redis Sorted Set 기록 | keyword = "반팔" | recordSearchKeyword("반팔") | Redis ZADD search:popular_keywords score=timestamp member="반팔" (PD-25) | Unit | P2 |
| BE-SE-016 | 인기 검색어 조회 시 1시간 이전 데이터 정리 | Redis에 2시간 전 검색 기록 존재 | getPopularKeywords() | ZREMRANGEBYSCORE(0, now-3600) 호출 후 ZREVRANGE top 10 반환 | Unit | P2 |
| BE-SE-017 | 금칙어 포함 키워드 제외 | "비속어A"가 금칙어 DB 등록, Redis에 "비속어A" 존재 | getPopularKeywords() | "비속어A" 결과에서 제외 (PD-39) | Unit | P2 |
| BE-SE-018 | 최근 검색어 저장 -- Redis List LPUSH + LTRIM | keyword = "청바지", memberId=1L | recordRecentKeyword(memberId, "청바지") | LPUSH search:recent:1 "청바지" + LTRIM 0 19 (최대 20개, PD-48) | Unit | P2 |
| BE-SE-019 | 최근 검색어 중복 시 기존 제거 후 재삽입 | "청바지"가 이미 리스트에 존재 | recordRecentKeyword(memberId, "청바지") | LREM "청바지" -> LPUSH "청바지" (최신으로 이동) | Unit | P2 |

### 3.3 Integration: ElasticsearchIndexingTest -- ES 인덱싱 (6건)

> 파일: `closet-search/src/test/kotlin/com/closet/search/infrastructure/elasticsearch/ElasticsearchIndexingTest.kt`
> 의존: Testcontainers (Elasticsearch with nori, Kafka)
> QA 매핑: SE-001~007

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SE-020 | product.created Kafka -> ES 문서 생성 | Kafka product.created 이벤트 발행 | Consumer 수신 + 인덱싱 | closet-products 인덱스에 문서 생성, 3초 이내 검색 가능 (PD-29) | Integration | P1 |
| BE-SE-021 | product.updated Kafka -> ES 문서 갱신 | 기존 문서 인덱싱됨, product.updated 이벤트 | Consumer 수신 | 기존 문서 업데이트 확인 (변경된 필드 반영) | Integration | P1 |
| BE-SE-022 | product.deleted Kafka -> ES 문서 삭제 | 기존 문서 인덱싱됨, product.deleted 이벤트 | Consumer 수신 | 문서 삭제, 검색 결과에서 제외 | Integration | P1 |
| BE-SE-023 | nori 형태소 분석 -- 복합어 분리 검색 | "반팔티셔츠" 상품명 인덱싱 | search(query="반팔") 또는 search(query="티셔츠") | 두 검색어 모두 해당 상품 반환 (nori 분리) | Integration | P1 |
| BE-SE-024 | 유의어 검색 | "청바지" 상품 인덱싱, synonym: "바지,팬츠,pants" | search(query="팬츠") | "청바지" 포함 상품 반환 (PD-24) | Integration | P2 |
| BE-SE-025 | 이벤트 멱등성 -- 중복 인덱싱 방지 | 동일 eventId로 product.created 2회 수신 | Consumer 처리 | processed_event UNIQUE KEY로 2회차 스킵, 1회만 인덱싱 | Integration | P1 |

### 3.4 Integration: AutocompleteTest -- 자동완성 (3건)

> 파일: `closet-search/src/test/kotlin/com/closet/search/infrastructure/elasticsearch/AutocompleteTest.kt`
> 의존: Testcontainers (Elasticsearch with nori)
> QA 매핑: SE-026~029

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-SE-026 | 자동완성 기본 동작 -- edge_ngram | "반팔 티셔츠" 인덱싱, autocomplete_index_analyzer 적용 | autocomplete(query="반팔") | suggestions에 "반팔 티셔츠" 포함 | Integration | P2 |
| BE-SE-027 | 자동완성 최대 10건 제한 | 다수 상품 인덱싱 (20건 이상) | autocomplete(query="티") | 최대 10개 suggestions 반환 | Integration | P2 |
| BE-SE-028 | 자동완성 1글자 입력 -- 빈 결과 | - | autocomplete(query="반") (1글자) | suggestions=[] (edge_ngram min_gram=2) | Integration | P2 |

---

## 4. closet-review (리뷰 서비스) -- 28건

### 4.1 Unit: ReviewTest -- 도메인 엔티티 (10건)

> 파일: `closet-review/src/test/kotlin/com/closet/review/domain/ReviewTest.kt`
> QA 매핑: RV-001~021

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-RV-001 | 별점 유효 범위 -- 1~5 정상 | rating = 1, 3, 5 | Review.create(rating=1), Review.create(rating=3), Review.create(rating=5) | 모두 정상 생성 | Unit | P1 |
| BE-RV-002 | 별점 범위 초과 -- 0 거절 | rating = 0 | Review.create(rating=0) | IllegalArgumentException("별점은 1~5 사이여야 합니다") | Unit | P1 |
| BE-RV-003 | 별점 범위 초과 -- 6 거절 | rating = 6 | Review.create(rating=6) | IllegalArgumentException("별점은 1~5 사이여야 합니다") | Unit | P1 |
| BE-RV-004 | 내용 최소 20자 검증 | content = "짧은리뷰" (10자) | Review.create(content="짧은리뷰") | IllegalArgumentException("최소 20자 이상 작성해주세요") | Unit | P1 |
| BE-RV-005 | 내용 정확히 20자 -- 경계값 | content = 정확히 20자 문자열 | Review.create(content=20자) | 정상 생성 | Unit | P1 |
| BE-RV-006 | 리뷰 수정 -- 7일 이내 가능 | Review(createdAt = now - 6일) | review.update(newContent) | 정상 수정, editCount 증가 | Unit | P1 |
| BE-RV-007 | 리뷰 수정 -- 7일 초과 불가 | Review(createdAt = now - 8일) | review.update(newContent) | IllegalArgumentException("수정 기한이 만료되었습니다") | Unit | P1 |
| BE-RV-008 | 리뷰 수정 -- 최대 3회 초과 불가 | Review(editCount = 3) | review.update(newContent) | IllegalArgumentException("최대 3회까지 수정 가능합니다", PD-32) | Unit | P1 |
| BE-RV-009 | 별점 수정 불가 | Review(rating = 5) | review.updateRating(3) | IllegalArgumentException("별점은 수정할 수 없습니다", PD-32) | Unit | P1 |
| BE-RV-010 | 리뷰 삭제 -- status DELETED 전이 | Review(status = ACTIVE) | review.delete() | status = DELETED | Unit | P1 |

### 4.2 Unit: ReviewServiceTest -- 서비스 레이어 (10건)

> 파일: `closet-review/src/test/kotlin/com/closet/review/application/ReviewServiceTest.kt`
> Mock: ReviewRepository, ReviewImageService, ReviewSummaryService, KafkaTemplate, RedisTemplate
> QA 매핑: RV-001~023, RV-027~033

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-RV-011 | 텍스트 리뷰 작성 -- 100P 포인트 이벤트 | CONFIRMED 상태 주문, content 20자 이상 | createReview(request) | Review 저장, kafkaTemplate.send("review.created", pointAmount=100) verify | Unit | P1 |
| BE-RV-012 | 포토 리뷰 작성 -- 300P 포인트 이벤트 | CONFIRMED 상태 주문, images 3장 | createReview(request with images) | reviewImageService.uploadImages() 호출, pointAmount=300 이벤트 | Unit | P1 |
| BE-RV-013 | 포토 + 사이즈 리뷰 -- 350P 포인트 이벤트 | images + sizeInfo 포함 | createReview(request with images + sizeInfo) | pointAmount=350 이벤트, ReviewSizeInfo 저장 | Unit | P2 |
| BE-RV-014 | CONFIRMED 아닌 주문에 리뷰 작성 거절 | DELIVERED 상태 주문 | createReview(request) | BusinessException("구매확정 후 리뷰 작성 가능") | Unit | P1 |
| BE-RV-015 | 동일 OrderItem 중복 리뷰 거절 | orderItemId에 이미 Review 존재 | createReview(request) | BusinessException(DUPLICATE_REVIEW) | Unit | P1 |
| BE-RV-016 | 이미지 5장 초과 거절 | images 6장 | createReview(request with 6 images) | BusinessException("이미지는 최대 5장") | Unit | P1 |
| BE-RV-017 | 리뷰 수정 -- 수정 이력 보존 | Review(editCount=1), 새로운 content | updateReview(reviewId, newContent) | ReviewEditHistory 저장 (이전 content 보존), editCount=2 (PD-32) | Unit | P1 |
| BE-RV-018 | 리뷰 삭제 -- 포인트 회수 이벤트 | Review(pointAwarded=300) | deleteReview(reviewId) | status=DELETED, kafkaTemplate.send("review.deleted", deductAmount=300) verify (PD-36) | Unit | P1 |
| BE-RV-019 | 일일 포인트 한도 5000P 체크 | Redis review:daily_point:{memberId} = 4800 | createReview (포토 300P) | 5000P 한도 내이므로 정상 적립, Redis INCRBY 300 | Unit | P2 |
| BE-RV-020 | 일일 포인트 한도 초과 시 차단 | Redis review:daily_point:{memberId} = 5000 | createReview (텍스트 100P) | 포인트 미적립 (한도 초과), 리뷰 자체는 작성 성공 (PD-37) | Unit | P2 |

### 4.3 Unit: ReviewImageServiceTest -- 이미지 리사이즈 (4건)

> 파일: `closet-review/src/test/kotlin/com/closet/review/application/ReviewImageServiceTest.kt`
> QA 매핑: RV-002, RV-012~013

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-RV-021 | 이미지 업로드 -- 원본 + 400x400 썸네일 생성 | JPEG 이미지 1장 (2MB) | uploadImages(reviewId, files) | 원본 저장 + 400x400 썸네일 생성 (Thumbnailator, PD-33) | Unit | P1 |
| BE-RV-022 | 이미지 5MB 초과 거절 | 6MB 이미지 파일 | uploadImages(reviewId, [6MB file]) | IllegalArgumentException("파일 크기는 5MB 이하", PD-34) | Unit | P1 |
| BE-RV-023 | 요청 전체 30MB 초과 거절 | 5MB x 7장 = 35MB | uploadImages(reviewId, 7 files) | IllegalArgumentException("전체 파일 크기는 30MB 이하", PD-34) | Unit | P1 |
| BE-RV-024 | 이미지 10장 초과 거절 | 11장 이미지 | uploadImages(reviewId, 11 files) | IllegalArgumentException("이미지는 최대 10개") | Unit | P1 |

### 4.4 Integration: ReviewSummaryTest -- 리뷰 집계 정합성 (4건)

> 파일: `closet-review/src/test/kotlin/com/closet/review/infrastructure/summary/ReviewSummaryTest.kt`
> 의존: Testcontainers (MySQL, Redis, Kafka)
> QA 매핑: RV-034~038

| ID | 테스트명 | Given | When | Then | 유형 | 우선순위 |
|----|---------|-------|------|------|------|---------|
| BE-RV-025 | 리뷰 생성 시 집계 갱신 | ReviewSummary(totalCount=10, avgRating=4.0), 새 리뷰(rating=5) | 리뷰 저장 후 ReviewSummary 갱신 | totalCount=11, avgRating 재계산, Redis 캐시 갱신, review.summary.updated Kafka 이벤트 | Integration | P2 |
| BE-RV-026 | 리뷰 삭제 시 집계 갱신 | ReviewSummary(totalCount=10), 리뷰 1건 삭제 | 삭제 후 ReviewSummary 갱신 | totalCount=9, avgRating 재계산, 별점 분포 갱신 | Integration | P2 |
| BE-RV-027 | 집계 조회 Redis 캐시 hit | Redis에 review:summary:{productId} 캐시 존재 | getReviewSummary(productId) | DB 조회 없이 Redis 캐시 반환 | Integration | P2 |
| BE-RV-028 | 관리자 전체 상품 집계 재계산 | 집계 불일치 상태 | recalculateAll() | 전체 상품 ReviewSummary 재계산, 정합성 확인 | Integration | P2 |

---

## 테스트 케이스 요약

| 서비스 | Unit | Integration | 소계 |
|--------|------|-------------|------|
| closet-inventory | 24 | 14 | 38 |
| closet-shipping | 26 | 10 | 36 |
| closet-search | 19 | 9 | 28 |
| closet-review | 24 | 4 | 28 |
| **합계** | **93** | **37** | **130** |

### 우선순위별 분포

| 우선순위 | 건수 | 비율 |
|---------|------|------|
| P0 | 41 | 31.5% |
| P1 | 58 | 44.6% |
| P2 | 31 | 23.9% |
| **합계** | **130** | 100% |

---

## QA TC -> BE TC 매핑표

| QA 범위 | QA TC수 | BE TC | 커버리지 |
|---------|---------|-------|---------|
| 배송 SH-001~053 | 53 | BE-SH-001~036 (36건) | 모든 SH TC의 도메인/서비스/통합 레벨 커버 |
| 재고 INV-001~032 | 32 | BE-INV-001~038 (38건) | 동시성 테스트 확장 (100스레드, 데드락) |
| 검색 SE-001~037 | 37 | BE-SE-001~028 (28건) | ES 인덱싱, nori 분석, 자동완성 |
| 리뷰 RV-001~038 | 38 | BE-RV-001~028 (28건) | 이미지 리사이즈, 포인트, 집계 |
| 크로스 XD-001~012 | 12 | Integration TC로 흡수 | Kafka 이벤트 기반 흐름 검증 |

> 크로스 도메인 TC(XD-001~012)는 각 서비스의 Integration 테스트에서 Kafka Consumer/Producer 레벨로 분해하여 커버합니다. E2E 테스트는 별도 `closet-e2e-test` 모듈에서 수행합니다.

---

## 테스트 인프라 구성

### BaseIntegrationTest 패턴

```kotlin
@SpringBootTest
@ActiveProfiles("test")
abstract class BaseIntegrationTest : BehaviorSpec() {
    companion object {
        @Container
        val mysql = MySQLContainer("mysql:8.0").apply {
            withDatabaseName("closet_test")
            withUsername("test")
            withPassword("test")
        }

        @Container
        val redis = GenericContainer("redis:7.0-alpine").apply {
            withExposedPorts(6379)
        }

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))

        init {
            mysql.start()
            redis.start()
            kafka.start()
        }
    }
}
```

### closet-search 전용: Elasticsearch Testcontainer

```kotlin
companion object {
    @Container
    val elasticsearch = ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
    ).apply {
        withEnv("xpack.security.enabled", "false")
        withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
        // nori 플러그인은 custom Docker image 또는 plugin install 필요
    }
}
```

### PM 의사결정 참조 매핑

| PM 의사결정 | 관련 BE TC |
|------------|-----------|
| PD-04 (Outbox + 멱등성) | BE-INV-029, BE-INV-030, BE-SE-025 |
| PD-08 (송장번호 형식) | BE-SH-008 |
| PD-10 (배송 상태 매핑) | BE-SH-005~007, BE-SH-029 |
| PD-11 (반품 배송비) | BE-SH-014 |
| PD-13 (반품 검수 3영업일) | BE-SH-036 |
| PD-14 (교환 동일 가격) | BE-SH-017 |
| PD-16 (7일 168시간) | BE-SH-012~013, BE-SH-033~034 |
| PD-17 (자동 구매확정 배치) | BE-SH-030~032 |
| PD-18 (RESERVE/DEDUCT/RELEASE) | BE-INV-001~005, BE-INV-031 |
| PD-19 (All-or-Nothing) | BE-INV-016 |
| PD-20 (카테고리별 안전재고) | BE-INV-014 |
| PD-22 (100 TPS) | BE-INV-033~034 |
| PD-23 (인기순 가중치) | BE-SE-014 |
| PD-24 (유의어) | BE-SE-024 |
| PD-25 (인기검색어 sliding window) | BE-SE-015~016 |
| PD-26 (오타 교정 fuzzy) | BE-SE-013 |
| PD-27 (자동완성 P99 50ms) | BE-SE-026~028 |
| PD-32 (별점 수정 불가, 이력 보존) | BE-RV-008~009, BE-RV-017 |
| PD-33 (Thumbnailator 400x400) | BE-RV-021 |
| PD-34 (이미지 5MB/30MB) | BE-RV-022~023 |
| PD-35 (관리자 HIDDEN) | 관리자 API는 Controller 레벨 테스트로 별도 커버 |
| PD-36 (포인트 회수 마이너스) | BE-RV-018 |
| PD-37 (일일 5000P 한도) | BE-RV-019~020 |
| PD-39 (금칙어) | BE-SE-017 |
| PD-41 (배송 추적 캐싱 5분) | BE-SH-023~025, BE-SH-027 |
| PD-45 (D-1 알림) | BE-SH-035 |
| PD-48 (최근 검색어) | BE-SE-018~019 |
| PD-51 (Outbox 패턴) | BE-INV-030 |
