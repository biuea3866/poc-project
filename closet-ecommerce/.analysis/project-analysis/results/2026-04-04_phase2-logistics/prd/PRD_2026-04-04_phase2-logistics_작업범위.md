# Phase 2 작업 범위 산정서

> 작성일: 2026-04-04
> 프로젝트: Closet E-commerce
> Phase: 2 (성장) + WMS 확장
> 도메인: 배송 + 재고 + 검색 + 리뷰 + **WMS(창고관리시스템)**
> PRD 원본: `.analysis/prd/results/2026-03-22_phase2-prd/phase2_prd.md`

---

## 0. 현재 코드베이스 현황

### Phase 1 구현 완료 모듈 (settings.gradle.kts 등록 + 소스 존재)

| 모듈 | Kotlin 파일 수 | 주요 구현 |
|------|---------------|----------|
| `closet-common` | 11 | BaseEntity, ErrorCode, GlobalExceptionHandler, ApiResponse, Money VO, BaseIntegrationTest, Fixture |
| `closet-gateway` | 5 | API Gateway (WebFlux 기반) |
| `closet-member` | 21 | 회원 인증/인가(JWT), 등급, 포인트, 배송지 관리 |
| `closet-product` | 28 | 상품 카탈로그, 카테고리, 브랜드, 옵션(사이즈/색상), SKU, QueryDSL |
| `closet-order` | 22 | 장바구니, 주문 생성, 주문 상태 관리, Kafka 이벤트 |
| `closet-payment` | 5 | PG 연동, 결제/환불 |
| `closet-bff` | 21 | BFF 오케스트레이션 (Feign Client), 구매자 화면 API |
| `closet-external-api` | 14 | Mock 택배사 API(CJ/Epost/Logen/Lotte), Mock PG(Toss/KakaoPay/NaverPay/Danal) |

### Phase 2 대상 모듈 (디렉토리만 존재, 소스 미구현 = 0 파일)

| 모듈 | 상태 | PRD 유저스토리 |
|------|------|---------------|
| `closet-shipping` | **빈 모듈** | US-501 ~ US-505 |
| `closet-inventory` | **빈 모듈** | US-601 ~ US-604 |
| `closet-search` | **빈 모듈** | US-701 ~ US-705 |
| `closet-review` | **빈 모듈** | US-801 ~ US-804 |
| `closet-seller` | **빈 모듈** | Phase 2에서 BFF 연동 필요 |
| `closet-settlement` | **빈 모듈** | 구매확정 연동 필요 |
| `closet-notification` | **빈 모듈** | 재입고/배송 알림 연동 필요 |

### 신규 추가 필요 모듈 (디렉토리 미존재)

| 모듈 | 용도 |
|------|------|
| `closet-wms` | **WMS 창고관리시스템** (추가 요구사항) |

### 인프라 현황 (docker-compose.yml)

| 인프라 | 상태 | Phase 2 필요 |
|--------|------|-------------|
| MySQL 8.0 | **운영중** | O (신규 테이블) |
| Redis 7.0 | **운영중** | O (분산락, 캐시, 인기검색어) |
| Kafka (Confluent 7.5) | **운영중** | O (신규 토픽) |
| Elasticsearch 8.11 | **운영중** (컨테이너만) | O (인덱스 생성, nori 플러그인) |
| Prometheus/Grafana/Loki | **운영중** | O (신규 서비스 메트릭) |

---

## 1. BE 변경 범위

### 1.1 신규 서비스 (From scratch)

#### A. closet-shipping (배송 서비스) - US-501 ~ US-505

| 레이어 | 예상 파일 | 설명 |
|--------|----------|------|
| **Domain** | | |
| entity | Shipping.kt | 배송 엔티티 |
| entity | ShippingTrackingLog.kt | 배송 추적 로그 엔티티 |
| entity | ReturnRequest.kt | 반품 요청 엔티티 |
| entity | ExchangeRequest.kt | 교환 요청 엔티티 |
| enum | ShippingStatus.kt | READY, IN_TRANSIT, DELIVERED (상태 전이 규칙) |
| enum | ReturnStatus.kt | REQUESTED ~ APPROVED/REJECTED |
| enum | ExchangeStatus.kt | REQUESTED ~ COMPLETED |
| enum | ReturnReason.kt | DEFECTIVE, WRONG_ITEM, CHANGE_OF_MIND, SIZE_MISMATCH |
| enum | Carrier.kt | CJ_LOGISTICS, EPOST, LOGEN, LOTTE |
| event | ShippingEvent.kt | order.status.changed, return.approved 등 |
| repository | ShippingRepository.kt | |
| repository | ShippingTrackingLogRepository.kt | |
| repository | ReturnRequestRepository.kt | |
| repository | ExchangeRequestRepository.kt | |
| **Application** | | |
| service | ShippingService.kt | 송장 등록, 배송 상태 변경 |
| service | ShippingTrackingService.kt | 택배사 API 연동, 배송 추적 |
| service | ReturnService.kt | 반품 신청/승인/거절 |
| service | ExchangeService.kt | 교환 신청/처리 |
| scheduler | AutoConfirmScheduler.kt | 자동 구매확정 배치 (매일 00:00) |
| consumer | OrderEventConsumer.kt | order.created, order.cancelled 수신 |
| **Presentation** | | |
| controller | ShippingController.kt | 송장 등록, 배송 추적 조회 |
| controller | ReturnController.kt | 반품 신청/조회 |
| controller | ExchangeController.kt | 교환 신청/조회 |
| dto | ShippingDto.kt, ReturnDto.kt, ExchangeDto.kt | |
| **Infrastructure** | | |
| client | CarrierApiClient.kt | 스마트택배 API 연동 (closet-external-api 호출) |
| config | KafkaConsumerConfig.kt | |
| config | RedisConfig.kt | 배송 추적 캐시 |
| **Test** | | |
| test | ShippingServiceTest.kt | |
| test | ReturnServiceTest.kt | |
| test | ExchangeServiceTest.kt | |
| test | AutoConfirmSchedulerTest.kt | |
| **DB Migration** | | |
| flyway | V2__create_shipping.sql | shipping, shipping_tracking_log, return_request, exchange_request |

**예상 파일 수: ~30개**

#### B. closet-inventory (재고 서비스) - US-601 ~ US-604

| 레이어 | 예상 파일 | 설명 |
|--------|----------|------|
| **Domain** | | |
| entity | Inventory.kt | 재고 엔티티 (@Version 낙관적 락) |
| entity | InventoryHistory.kt | 재고 변경 이력 |
| entity | RestockNotification.kt | 재입고 알림 신청 |
| enum | ChangeType.kt | DEDUCT, RESTORE, INBOUND, ADJUST |
| enum | ReferenceType.kt | ORDER, RETURN, EXCHANGE, MANUAL |
| enum | NotificationStatus.kt | WAITING, NOTIFIED, CANCELLED |
| event | InventoryEvent.kt | inventory.insufficient, inventory.low_stock, inventory.out_of_stock, inventory.restock_notification |
| repository | InventoryRepository.kt | |
| repository | InventoryHistoryRepository.kt | |
| repository | RestockNotificationRepository.kt | |
| **Application** | | |
| service | InventoryService.kt | 재고 차감/복구, 분산락 |
| service | RestockNotificationService.kt | 재입고 알림 관리 |
| consumer | OrderEventConsumer.kt | order.created(차감), order.cancelled(복구) |
| consumer | ReturnEventConsumer.kt | return.approved(복구) |
| **Presentation** | | |
| controller | InventoryController.kt | 재고 조회, 차감, 안전재고 설정 |
| controller | RestockNotificationController.kt | 재입고 알림 신청/취소 |
| dto | InventoryDto.kt, RestockDto.kt | |
| **Infrastructure** | | |
| config | RedissonConfig.kt | Redisson 분산락 설정 |
| config | KafkaConsumerConfig.kt | |
| lock | DistributedLockManager.kt | Redis 분산락 유틸 |
| **Test** | | |
| test | InventoryServiceTest.kt | |
| test | InventoryConcurrencyTest.kt | 100 스레드 동시성 테스트 |
| test | RestockNotificationServiceTest.kt | |
| **DB Migration** | | |
| flyway | V2__create_inventory.sql | inventory, inventory_history, restock_notification |

**예상 파일 수: ~25개**

#### C. closet-search (검색 서비스) - US-701 ~ US-705

| 레이어 | 예상 파일 | 설명 |
|--------|----------|------|
| **Domain** | | |
| document | ProductDocument.kt | ES 상품 문서 모델 |
| enum | SearchSort.kt | RELEVANCE, LATEST, PRICE_ASC, PRICE_DESC, POPULAR |
| **Application** | | |
| service | ProductIndexService.kt | 상품 인덱싱 (단건 + 벌크) |
| service | ProductSearchService.kt | 키워드 검색 + 필터 |
| service | AutocompleteService.kt | 자동완성 |
| service | PopularKeywordService.kt | 인기 검색어 관리 (Redis Sorted Set) |
| consumer | ProductEventConsumer.kt | product.created/updated/deleted 수신 |
| consumer | ReviewSummaryConsumer.kt | review.summary.updated 수신 (별점 동기화) |
| **Presentation** | | |
| controller | SearchController.kt | 검색, 필터, 자동완성, 인기 검색어 |
| controller | IndexAdminController.kt | 재인덱싱 (admin) |
| dto | SearchDto.kt, FacetDto.kt | |
| **Infrastructure** | | |
| config | ElasticsearchConfig.kt | ES 클라이언트 설정 |
| config | KafkaConsumerConfig.kt | |
| config | RedisConfig.kt | 인기 검색어 캐시 |
| index | ProductIndexTemplate.kt | 인덱스 매핑/설정 (nori, edge_ngram) |
| repository | ProductSearchRepository.kt | ES 쿼리 구현 |
| **Test** | | |
| test | ProductSearchServiceTest.kt | |
| test | AutocompleteServiceTest.kt | |
| test | PopularKeywordServiceTest.kt | |
| **DB Migration** | | |
| - | ES 인덱스 템플릿만 (Flyway 불필요) | |

**예상 파일 수: ~22개**

#### D. closet-review (리뷰 서비스) - US-801 ~ US-804

| 레이어 | 예상 파일 | 설명 |
|--------|----------|------|
| **Domain** | | |
| entity | Review.kt | 리뷰 엔티티 |
| entity | ReviewImage.kt | 리뷰 이미지 |
| entity | ReviewSizeInfo.kt | 사이즈 후기 |
| entity | ReviewSummary.kt | 리뷰 집계 |
| enum | SizeFit.kt | SMALL, PERFECT, LARGE |
| enum | ReviewStatus.kt | ACTIVE, HIDDEN, DELETED |
| event | ReviewEvent.kt | review.created, review.summary.updated |
| repository | ReviewRepository.kt | |
| repository | ReviewImageRepository.kt | |
| repository | ReviewSizeInfoRepository.kt | |
| repository | ReviewSummaryRepository.kt | |
| **Application** | | |
| service | ReviewService.kt | 리뷰 CRUD |
| service | ReviewImageService.kt | 이미지 업로드/리사이즈 |
| service | ReviewSummaryService.kt | 리뷰 집계 (실시간 + 배치) |
| scheduler | ReviewSummaryRecalculateScheduler.kt | 집계 보정 배치 |
| **Presentation** | | |
| controller | ReviewController.kt | 리뷰 작성/조회/수정/삭제 |
| controller | ReviewSummaryController.kt | 리뷰 집계 조회, 재계산 |
| dto | ReviewDto.kt, ReviewSummaryDto.kt | |
| **Infrastructure** | | |
| config | S3Config.kt | S3 이미지 업로드 |
| config | KafkaProducerConfig.kt | |
| config | RedisConfig.kt | 리뷰 집계 캐시 |
| client | S3ImageUploader.kt | |
| **Test** | | |
| test | ReviewServiceTest.kt | |
| test | ReviewSummaryServiceTest.kt | |
| **DB Migration** | | |
| flyway | V2__create_review.sql | review, review_image, review_size_info, review_summary |

**예상 파일 수: ~25개**

#### E. closet-wms (WMS 창고관리시스템) - 신규 추가 요구사항

> 올리브영 GMS 벤치마킹. 입고 ~ 출고 ~ 재고실사 전체 창고 운영 프로세스.

| 레이어 | 예상 파일 | 설명 |
|--------|----------|------|
| **Domain - 공통** | | |
| entity | Warehouse.kt | 창고 마스터 |
| entity | Location.kt | 로케이션 (Zone > Aisle > Rack > Shelf > Bin) |
| enum | LocationType.kt | RECEIVING, STORAGE, PICKING, PACKING, SHIPPING |
| enum | ZoneType.kt | GENERAL, COLD, HAZARDOUS, VALUABLE |
| **Domain - 입고** | | |
| entity | InboundOrder.kt | 입고 예정 (ASN: Advanced Shipping Notice) |
| entity | InboundOrderItem.kt | 입고 예정 상품 |
| entity | InboundReceipt.kt | 입고 실적 (검수 완료 후) |
| entity | InboundReceiptItem.kt | 입고 실적 상품 |
| enum | InboundStatus.kt | EXPECTED, ARRIVED, INSPECTING, INSPECTION_COMPLETED, CONFIRMED, CANCELLED |
| enum | InspectionResult.kt | PASSED, PARTIAL_PASSED, REJECTED |
| **Domain - 보관** | | |
| entity | StockLot.kt | 재고 로트 (입고 단위별 구분) |
| entity | LocationInventory.kt | 로케이션별 재고 (로케이션 + SKU + 로트) |
| entity | PutawayTask.kt | 적치 작업 (검수 완료 → 보관 로케이션 이동) |
| enum | PutawayStatus.kt | PENDING, IN_PROGRESS, COMPLETED |
| enum | StockStatus.kt | AVAILABLE, RESERVED, DAMAGED, QUARANTINE |
| **Domain - 피킹** | | |
| entity | PickingWave.kt | 피킹 웨이브 (다건 주문 묶음 피킹) |
| entity | PickingTask.kt | 피킹 작업 지시 |
| entity | PickingTaskItem.kt | 피킹 작업 상세 (SKU + 로케이션 + 수량) |
| enum | PickingWaveStatus.kt | CREATED, ASSIGNED, IN_PROGRESS, COMPLETED |
| enum | PickingTaskStatus.kt | PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, SHORT |
| enum | PickingStrategy.kt | FIFO, FEFO, CLOSEST_LOCATION |
| **Domain - 출고** | | |
| entity | OutboundOrder.kt | 출고 지시 |
| entity | OutboundOrderItem.kt | 출고 지시 상품 |
| entity | PackingTask.kt | 포장 작업 |
| entity | ShipmentRequest.kt | 택배 접수 |
| enum | OutboundStatus.kt | CREATED, PICKING, PICKED, PACKING, PACKED, SHIPPED, COMPLETED |
| enum | PackingStatus.kt | PENDING, IN_PROGRESS, COMPLETED, FAILED |
| **Domain - 재고 실사** | | |
| entity | StocktakeOrder.kt | 실사 요청 |
| entity | StocktakeOrderItem.kt | 실사 항목 |
| entity | StocktakeResult.kt | 실사 결과 (차이 조정) |
| enum | StocktakeStatus.kt | REQUESTED, IN_PROGRESS, COMPLETED, ADJUSTED |
| enum | StocktakeType.kt | FULL, CYCLE, SPOT |
| **Domain - 이벤트** | | |
| event | WmsEvent.kt | wms.inbound.confirmed, wms.outbound.completed, wms.stocktake.adjusted |
| **Domain - 리포지토리** | | |
| repository | WarehouseRepository.kt | |
| repository | LocationRepository.kt | |
| repository | InboundOrderRepository.kt | |
| repository | InboundReceiptRepository.kt | |
| repository | StockLotRepository.kt | |
| repository | LocationInventoryRepository.kt | |
| repository | PutawayTaskRepository.kt | |
| repository | PickingWaveRepository.kt | |
| repository | PickingTaskRepository.kt | |
| repository | OutboundOrderRepository.kt | |
| repository | PackingTaskRepository.kt | |
| repository | ShipmentRequestRepository.kt | |
| repository | StocktakeOrderRepository.kt | |
| repository | StocktakeResultRepository.kt | |
| **Application** | | |
| service | InboundService.kt | 입고 예정 등록 → 검수 → 입고 확정 → 재고 반영 |
| service | InspectionService.kt | 검수 처리 (양품/불량/부분합격) |
| service | PutawayService.kt | 적치 (로케이션 지정, 자동 추천) |
| service | LocationService.kt | 로케이션 관리, 최적 위치 추천 |
| service | PickingWaveService.kt | 피킹 웨이브 생성 (다건 주문 묶음) |
| service | PickingTaskService.kt | 피킹 지시 생성 → 피킹 진행 → 완료 |
| service | OutboundService.kt | 출고 지시 → 포장 → 택배 접수 → 출고 완료 |
| service | PackingService.kt | 포장 작업 관리 |
| service | ShipmentService.kt | 택배 접수, 송장 발급 |
| service | StocktakeService.kt | 실사 요청 → 실사 진행 → 차이 조정 |
| service | StockLotService.kt | 로트 관리, FIFO/FEFO 조회 |
| service | WmsInventorySyncService.kt | WMS 재고 ↔ closet-inventory 동기화 |
| consumer | OrderEventConsumer.kt | order.paid → 출고 지시 자동 생성 |
| consumer | InventoryEventConsumer.kt | 재고 동기화 이벤트 수신 |
| **Presentation** | | |
| controller | InboundController.kt | 입고 관리 API |
| controller | PutawayController.kt | 적치 관리 API |
| controller | PickingController.kt | 피킹 관리 API |
| controller | OutboundController.kt | 출고 관리 API |
| controller | StocktakeController.kt | 재고 실사 API |
| controller | WarehouseController.kt | 창고/로케이션 관리 API |
| controller | WmsDashboardController.kt | WMS 대시보드 (재고현황, 작업현황) |
| dto | InboundDto.kt | |
| dto | PutawayDto.kt | |
| dto | PickingDto.kt | |
| dto | OutboundDto.kt | |
| dto | StocktakeDto.kt | |
| dto | WarehouseDto.kt | |
| dto | WmsDashboardDto.kt | |
| **Infrastructure** | | |
| config | KafkaConsumerConfig.kt | |
| config | RedisConfig.kt | 로케이션 재고 캐시 |
| client | CarrierApiClient.kt | 택배 접수 연동 |
| **Test** | | |
| test | InboundServiceTest.kt | |
| test | PickingTaskServiceTest.kt | |
| test | OutboundServiceTest.kt | |
| test | StocktakeServiceTest.kt | |
| test | PutawayServiceTest.kt | |
| test | WmsIntegrationTest.kt | 입고→적치→피킹→출고 E2E |
| **DB Migration** | | |
| flyway | V1__create_wms_warehouse.sql | warehouse, location |
| flyway | V2__create_wms_inbound.sql | inbound_order, inbound_order_item, inbound_receipt, inbound_receipt_item |
| flyway | V3__create_wms_storage.sql | stock_lot, location_inventory, putaway_task |
| flyway | V4__create_wms_picking.sql | picking_wave, picking_task, picking_task_item |
| flyway | V5__create_wms_outbound.sql | outbound_order, outbound_order_item, packing_task, shipment_request |
| flyway | V6__create_wms_stocktake.sql | stocktake_order, stocktake_order_item, stocktake_result |

**예상 파일 수: ~85개**

### 1.2 수정 서비스 (기존 모듈 변경)

| 모듈 | 변경 파일 | 변경 내용 |
|------|----------|----------|
| **closet-order** | `OrderStatus.kt` | RETURN_REQUESTED, EXCHANGE_REQUESTED 상태 추가, 전이 규칙 확장 |
| | `OrderEvent.kt` | OrderConfirmedEvent, OrderStatusChangedEvent 추가 |
| | `OrderService.kt` | 구매확정 API, 상태 변경 이벤트 발행 로직 |
| | `OrderController.kt` | POST /orders/{orderId}/confirm 엔드포인트 |
| | `OrderDto.kt` | 구매확정 응답 DTO |
| **closet-member** | `PointHistory.kt` | REVIEW_TEXT, REVIEW_PHOTO, REVIEW_SIZE 포인트 타입 추가 |
| | `MemberService.kt` | 포인트 적립 이벤트 컨슈머 추가 |
| | (신규) `PointEventConsumer.kt` | point.earn 이벤트 소비 |
| **closet-product** | `ProductService.kt` | 상품 생성/수정/삭제 시 Kafka 이벤트 발행 추가 |
| | (신규) `ProductEvent.kt` | product.created, product.updated, product.deleted |
| | (신규) `KafkaProducerConfig.kt` | Kafka Producer 설정 |
| **closet-payment** | `PaymentService.kt` | 반품 환불 처리 API 추가 |
| | `PaymentController.kt` | POST /payments/{paymentId}/refund 엔드포인트 |
| **closet-bff** | (신규) `ShippingServiceClient.kt` | shipping-service Feign 클라이언트 |
| | (신규) `InventoryServiceClient.kt` | inventory-service Feign 클라이언트 |
| | (신규) `SearchServiceClient.kt` | search-service Feign 클라이언트 |
| | (신규) `ReviewServiceClient.kt` | review-service Feign 클라이언트 |
| | (신규) `WmsServiceClient.kt` | wms-service Feign 클라이언트 |
| | (신규) `ShippingBffFacade.kt` | 배송 추적/반품/교환 BFF |
| | (신규) `ReviewBffFacade.kt` | 리뷰 작성/조회 BFF |
| | (신규) `SearchBffFacade.kt` | 검색/필터/자동완성 BFF |
| | (신규) `SellerBffFacade.kt` | 판매자 화면 BFF (송장등록, 재고, WMS) |
| | (신규) `AdminBffFacade.kt` | 관리자 화면 BFF (WMS 대시보드) |
| | (신규) `BffShippingController.kt` | |
| | (신규) `BffReviewController.kt` | |
| | (신규) `BffSearchController.kt` | |
| | (신규) `BffSellerController.kt` | |
| | (신규) `BffAdminController.kt` | |
| **closet-common** | (신규) `KafkaConfig.kt` | 공통 Kafka 설정 추출 |
| | (신규) `RedisConfig.kt` | 공통 Redis 설정 추출 |
| | `ErrorCode.kt` | 배송/재고/검색/리뷰/WMS 에러코드 추가 |
| | (신규) `ShippingFixture.kt` | 테스트 픽스처 |
| | (신규) `InventoryFixture.kt` | 테스트 픽스처 |
| | (신규) `ReviewFixture.kt` | 테스트 픽스처 |
| **closet-external-api** | `CarrierService.kt` | 배송 추적 조회 API 추가 |
| | (신규) `SmartDeliveryController.kt` | 스마트택배 Mock API |
| **docker/docker-compose.yml** | 서비스 정의 | closet-shipping, closet-inventory, closet-search, closet-review, closet-wms 추가 |
| **settings.gradle.kts** | include | 신규 5개 모듈 등록 |

**수정 파일 수: ~35개 (기존 파일 수정 + 기존 모듈 내 신규 파일)**

### 1.3 모듈별 변경 요약

| 모듈 | 신규 파일 | 수정 파일 | 합계 |
|------|----------|----------|------|
| closet-shipping | ~30 | 0 | **30** |
| closet-inventory | ~25 | 0 | **25** |
| closet-search | ~22 | 0 | **22** |
| closet-review | ~25 | 0 | **25** |
| closet-wms | ~85 | 0 | **85** |
| closet-order (수정) | 0 | ~5 | **5** |
| closet-member (수정) | ~1 | ~2 | **3** |
| closet-product (수정) | ~2 | ~1 | **3** |
| closet-payment (수정) | 0 | ~2 | **2** |
| closet-bff (수정) | ~15 | 0 | **15** |
| closet-common (수정) | ~5 | ~1 | **6** |
| closet-external-api (수정) | ~1 | ~1 | **2** |
| 인프라/설정 | 0 | ~2 | **2** |
| **합계** | **~211** | **~14** | **~225** |

---

## 2. FE 변경 범위

### 2.1 BFF API 추가 목록

#### 구매자 화면 (closet-web / closet-mobile)

| API | Method | Path | 연동 서비스 |
|-----|--------|------|------------|
| 배송 추적 조회 | GET | `/bff/v1/orders/{orderId}/shipping` | shipping |
| 반품 신청 | POST | `/bff/v1/returns` | shipping, payment |
| 반품 조회 | GET | `/bff/v1/returns?orderId={orderId}` | shipping |
| 교환 신청 | POST | `/bff/v1/exchanges` | shipping, inventory |
| 교환 조회 | GET | `/bff/v1/exchanges?orderId={orderId}` | shipping |
| 구매확정 | POST | `/bff/v1/orders/{orderId}/confirm` | order |
| 상품 검색 | GET | `/bff/v1/search/products` | search |
| 자동완성 | GET | `/bff/v1/search/autocomplete` | search |
| 인기 검색어 | GET | `/bff/v1/search/popular-keywords` | search |
| 리뷰 작성 | POST | `/bff/v1/reviews` | review |
| 리뷰 목록 조회 | GET | `/bff/v1/reviews?productId={id}` | review |
| 리뷰 수정 | PUT | `/bff/v1/reviews/{reviewId}` | review |
| 리뷰 삭제 | DELETE | `/bff/v1/reviews/{reviewId}` | review |
| 사이즈 후기 요약 | GET | `/bff/v1/reviews/size-summary?productId={id}` | review |
| 리뷰 집계 | GET | `/bff/v1/reviews/summary?productId={id}` | review |
| 재입고 알림 신청 | POST | `/bff/v1/restock-notifications` | inventory |
| 재입고 알림 취소 | DELETE | `/bff/v1/restock-notifications/{id}` | inventory |
| 재고 조회 (옵션별) | GET | `/bff/v1/inventories?productId={id}` | inventory |

**구매자 API: 18개**

#### 판매자 화면 (closet-web 셀러 어드민)

| API | Method | Path | 연동 서비스 |
|-----|--------|------|------------|
| 송장 등록 | POST | `/bff/v1/seller/shippings` | shipping |
| 반품 목록 | GET | `/bff/v1/seller/returns` | shipping |
| 반품 승인/거절 | PATCH | `/bff/v1/seller/returns/{id}/process` | shipping, payment |
| 교환 목록 | GET | `/bff/v1/seller/exchanges` | shipping |
| 교환 처리 | PATCH | `/bff/v1/seller/exchanges/{id}/process` | shipping, inventory |
| 재고 조회 | GET | `/bff/v1/seller/inventories` | inventory |
| 안전재고 설정 | PATCH | `/bff/v1/seller/inventories/{id}/safety-stock` | inventory |
| 재고 입고 | POST | `/bff/v1/seller/inventories/{id}/inbound` | inventory |
| WMS 입고 예정 등록 | POST | `/bff/v1/seller/wms/inbound-orders` | wms |
| WMS 입고 예정 목록 | GET | `/bff/v1/seller/wms/inbound-orders` | wms |
| WMS 입고 검수 | PATCH | `/bff/v1/seller/wms/inbound-orders/{id}/inspect` | wms |
| WMS 입고 확정 | POST | `/bff/v1/seller/wms/inbound-orders/{id}/confirm` | wms |
| WMS 출고 지시 조회 | GET | `/bff/v1/seller/wms/outbound-orders` | wms |
| WMS 재고 현황 | GET | `/bff/v1/seller/wms/stock-summary` | wms |

**판매자 API: 14개**

#### 관리자 화면 (closet-web 어드민)

| API | Method | Path | 연동 서비스 |
|-----|--------|------|------------|
| 재인덱싱 | POST | `/bff/v1/admin/search/reindex` | search |
| 리뷰 집계 재계산 | POST | `/bff/v1/admin/reviews/summary/recalculate` | review |
| WMS 창고 관리 | CRUD | `/bff/v1/admin/wms/warehouses` | wms |
| WMS 로케이션 관리 | CRUD | `/bff/v1/admin/wms/locations` | wms |
| WMS 적치 지시 | POST | `/bff/v1/admin/wms/putaway-tasks` | wms |
| WMS 적치 완료 | PATCH | `/bff/v1/admin/wms/putaway-tasks/{id}/complete` | wms |
| WMS 피킹 웨이브 생성 | POST | `/bff/v1/admin/wms/picking-waves` | wms |
| WMS 피킹 작업 목록 | GET | `/bff/v1/admin/wms/picking-tasks` | wms |
| WMS 피킹 진행/완료 | PATCH | `/bff/v1/admin/wms/picking-tasks/{id}/process` | wms |
| WMS 포장 처리 | PATCH | `/bff/v1/admin/wms/packing-tasks/{id}/process` | wms |
| WMS 택배 접수 | POST | `/bff/v1/admin/wms/shipments` | wms |
| WMS 출고 완료 | POST | `/bff/v1/admin/wms/outbound-orders/{id}/complete` | wms |
| WMS 재고 실사 요청 | POST | `/bff/v1/admin/wms/stocktakes` | wms |
| WMS 재고 실사 진행 | PATCH | `/bff/v1/admin/wms/stocktakes/{id}/process` | wms |
| WMS 재고 실사 차이 조정 | POST | `/bff/v1/admin/wms/stocktakes/{id}/adjust` | wms |
| WMS 대시보드 | GET | `/bff/v1/admin/wms/dashboard` | wms |

**관리자 API: 16개**

**BFF API 총합: 48개**

---

## 3. 인프라 변경

### 3.1 Kafka 토픽

| 토픽명 | Producer | Consumer | 용도 |
|--------|----------|----------|------|
| `order.status.changed` | order, shipping | shipping, notification | 주문 상태 변경 알림 |
| `order.confirmed` | order, shipping | settlement, inventory | 구매확정 |
| `product.created` | product | search | 상품 생성 인덱싱 |
| `product.updated` | product | search | 상품 수정 인덱싱 |
| `product.deleted` | product | search | 상품 삭제 인덱싱 |
| `inventory.insufficient` | inventory | order | 재고 부족 |
| `inventory.low_stock` | inventory | notification | 안전재고 알림 |
| `inventory.out_of_stock` | inventory | notification, product | 품절 |
| `inventory.restock_notification` | inventory | notification | 재입고 알림 발송 |
| `return.approved` | shipping | inventory, payment | 반품 승인 → 재고복구 + 환불 |
| `review.created` | review | member, search | 리뷰 생성 → 포인트 적립 |
| `review.summary.updated` | review | search | 리뷰 집계 → ES 동기화 |
| `point.earn` | review | member | 포인트 적립 |
| `wms.inbound.confirmed` | wms | inventory | WMS 입고 확정 → 재고 반영 |
| `wms.outbound.completed` | wms | shipping | WMS 출고 완료 → 배송 시작 |
| `wms.stocktake.adjusted` | wms | inventory | WMS 실사 차이 → 재고 조정 |
| `wms.outbound.created` | wms | - | 출고 지시 생성 (로그) |

**신규 Kafka 토픽: 17개**

### 3.2 Redis 키 설계

| 키 패턴 | 데이터 타입 | TTL | 서비스 | 용도 |
|---------|-----------|-----|--------|------|
| `shipping:tracking:{shippingId}` | String (JSON) | 5분 | shipping | 배송 추적 캐시 |
| `inventory:lock:{sku}` | String | 3초 (락) | inventory | 분산락 (Redisson) |
| `inventory:low_stock_alert:{sku}` | String | 24시간 | inventory | 안전재고 중복 알림 방지 |
| `search:popular_keywords` | Sorted Set | - (rolling) | search | 인기 검색어 실시간 집계 |
| `search:popular_keywords:prev` | Sorted Set | 1시간 | search | 이전 시간 인기 검색어 (순위 변동 계산) |
| `search:banned_keywords` | Set | - | search | 금칙어 목록 |
| `review:summary:{productId}` | String (JSON) | 10분 | review | 리뷰 집계 캐시 |
| `review:daily_point:{memberId}` | String (Int) | 24시간 | review | 일일 리뷰 포인트 한도 체크 |
| `wms:location_inventory:{warehouseId}` | Hash | 5분 | wms | 로케이션별 재고 캐시 |
| `wms:picking_wave:{waveId}` | String (JSON) | 1시간 | wms | 피킹 웨이브 진행 상태 |

**신규 Redis 키: 10개 패턴**

### 3.3 Elasticsearch 인덱스

| 인덱스명 | 용도 | 분석기 |
|---------|------|--------|
| `closet-products` | 상품 검색 | nori_analyzer (한글 형태소), autocomplete_analyzer (edge_ngram) |

**인덱스 설정 요소:**
- nori 한글 형태소 분석기 (nori_tokenizer + nori_readingform + lowercase)
- autocomplete_analyzer (edge_ngram 1~20, letter + digit)
- 유의어 사전 (synonyms.txt): 바지=팬츠, 상의=탑, 원피스=드레스 등
- 인덱싱 필드: productId, name, brand, category, subCategory, price, salePrice, colors, sizes, tags, description, imageUrl, salesCount, reviewCount, avgRating, createdAt, updatedAt
- DLQ 토픽: `search.indexing.dlq` (인덱싱 실패 재처리)

**신규 ES 인덱스: 1개 (+ DLQ 토픽 1개)**

### 3.4 DB 스키마 변경 (MySQL)

| 도메인 | 신규 테이블 | 수정 테이블 |
|--------|-----------|-----------|
| shipping | shipping, shipping_tracking_log, return_request, exchange_request | - |
| inventory | inventory, inventory_history, restock_notification | - |
| review | review, review_image, review_size_info, review_summary | - |
| wms | warehouse, location, inbound_order, inbound_order_item, inbound_receipt, inbound_receipt_item, stock_lot, location_inventory, putaway_task, picking_wave, picking_task, picking_task_item, outbound_order, outbound_order_item, packing_task, shipment_request, stocktake_order, stocktake_order_item, stocktake_result | - |

**신규 테이블: 30개**

### 3.5 docker-compose.yml 변경

신규 서비스 컨테이너 추가:

| 서비스 | 포트 | 의존성 |
|--------|------|--------|
| closet-shipping | 8085 | mysql, kafka, redis |
| closet-inventory | 8086 | mysql, kafka, redis |
| closet-search | 8087 | elasticsearch, kafka, redis |
| closet-review | 8088 | mysql, kafka, redis, s3 |
| closet-wms | 8089 | mysql, kafka, redis |

> 참고: PRD 원본의 포트 배정(8084~8087)은 closet-payment가 이미 8084를 사용하므로 8085부터 재배정.

### 3.6 settings.gradle.kts 변경

```kotlin
include(
    "closet-common",
    "closet-gateway",
    "closet-member",
    "closet-product",
    "closet-order",
    "closet-payment",
    "closet-bff",
    "closet-external-api",
    // Phase 2
    "closet-shipping",
    "closet-inventory",
    "closet-search",
    "closet-review",
    // Phase 2 - WMS
    "closet-wms"
)
```

---

## 4. QA 시나리오

### 4.1 배송 도메인 (closet-shipping)

| # | 시나리오 | US | 우선순위 |
|---|---------|-----|---------|
| S-01 | 판매자가 송장번호를 등록하면 주문 상태가 SHIPPING으로 변경된다 | US-501 | P0 |
| S-02 | 이미 송장 등록된 주문에 중복 등록 시 에러 반환 | US-501 | P0 |
| S-03 | 송장번호 형식 검증 (10-15자리 숫자) | US-501 | P1 |
| S-04 | 배송 추적 정보 조회 시 택배사 API 응답을 정상 반환 | US-502 | P0 |
| S-05 | 택배사 API 장애 시 캐싱된 정보 반환 | US-502 | P1 |
| S-06 | 배송 추적 Redis 캐시 5분 TTL 동작 확인 | US-502 | P1 |
| S-07 | 배송 완료 후 7일 경과 시 자동 구매확정 | US-503 | P0 |
| S-08 | 구매자 수동 구매확정 (7일 이전) | US-503 | P0 |
| S-09 | 반품/교환 진행 중인 건 자동 구매확정 제외 | US-503 | P0 |
| S-10 | 구매확정 시 정산 이벤트(order.confirmed) 발행 | US-503 | P0 |
| S-11 | 반품 신청 (배송 완료 후 7일 이내) | US-504 | P0 |
| S-12 | 반품 사유별 배송비 부담 (단순변심=구매자, 불량=판매자) | US-504 | P0 |
| S-13 | 반품 상태 흐름 (REQUESTED → ... → APPROVED/REJECTED) | US-504 | P0 |
| S-14 | 반품 승인 시 결제 취소(환불) 처리 | US-504 | P0 |
| S-15 | 반품 승인 시 재고 복구 이벤트 발행 | US-504 | P0 |
| S-16 | 반품 거절 시 거절 사유 기재 + 알림 발송 | US-504 | P1 |
| S-17 | 교환 신청 (동일 상품 다른 옵션만 가능) | US-505 | P0 |
| S-18 | 교환 희망 옵션 재고 확인 | US-505 | P0 |
| S-19 | 교환 시 재고 선점/복구 정합성 | US-505 | P0 |
| S-20 | 교환 상태 흐름 (REQUESTED → ... → COMPLETED) | US-505 | P1 |

**배송 시나리오: 20개**

### 4.2 재고 도메인 (closet-inventory)

| # | 시나리오 | US | 우선순위 |
|---|---------|-----|---------|
| I-01 | 주문 생성 시 SKU별 재고 차감 | US-601 | P0 |
| I-02 | 재고 부족 시 주문 거절 + 이벤트 발행 | US-601 | P0 |
| I-03 | 주문 취소 시 재고 복구 | US-601 | P0 |
| I-04 | 재고 변경 이력 기록 (차감/복구/입고) | US-601 | P0 |
| I-05 | 재고 수량 음수 방지 | US-601 | P0 |
| I-06 | Redis 분산락으로 동시 차감 정합성 보장 | US-602 | P0 |
| I-07 | 100 스레드 동시 차감 테스트 | US-602 | P0 |
| I-08 | 락 획득 실패 시 3회 재시도 후 실패 반환 | US-602 | P1 |
| I-09 | @Version 낙관적 락 2차 안전장치 동작 | US-602 | P1 |
| I-10 | 안전재고 이하 시 low_stock 알림 이벤트 | US-603 | P0 |
| I-11 | 24시간 내 중복 알림 방지 (Redis) | US-603 | P1 |
| I-12 | 재고 0 시 out_of_stock 이벤트 | US-603 | P0 |
| I-13 | 재입고 알림 신청/취소 | US-604 | P0 |
| I-14 | 재입고 시 알림 신청자에게 이벤트 발행 | US-604 | P0 |
| I-15 | 회원당 재입고 알림 50건 제한 | US-604 | P1 |

**재고 시나리오: 15개**

### 4.3 검색 도메인 (closet-search)

| # | 시나리오 | US | 우선순위 |
|---|---------|-----|---------|
| SR-01 | 상품 생성 이벤트 수신 시 ES 인덱싱 | US-701 | P0 |
| SR-02 | 상품 수정/삭제 이벤트 수신 시 ES 업데이트/삭제 | US-701 | P0 |
| SR-03 | 벌크 재인덱싱 API 동작 | US-701 | P1 |
| SR-04 | 인덱싱 실패 시 DLQ 저장 | US-701 | P1 |
| SR-05 | 한글 키워드 검색 (nori 형태소 분석) | US-702 | P0 |
| SR-06 | 복합어 분리 검색 ("반팔티셔츠" → "반팔" + "티셔츠") | US-702 | P0 |
| SR-07 | 검색 결과 정렬 (관련도/최신/가격/인기) | US-702 | P0 |
| SR-08 | 검색 결과 하이라이팅 | US-702 | P1 |
| SR-09 | 카테고리/브랜드/가격/색상/사이즈 필터 | US-703 | P0 |
| SR-10 | 필터 facet count 표시 | US-703 | P0 |
| SR-11 | 복수 필터 AND 조합 | US-703 | P1 |
| SR-12 | 자동완성 (2글자 이상, 최대 10개) | US-704 | P0 |
| SR-13 | 자동완성 응답 시간 50ms 이내 | US-704 | P1 |
| SR-14 | 인기 검색어 Top 10 조회 | US-705 | P0 |
| SR-15 | 인기 검색어 순위 변동 표시 | US-705 | P1 |
| SR-16 | 금칙어 필터링 | US-705 | P1 |

**검색 시나리오: 16개**

### 4.4 리뷰 도메인 (closet-review)

| # | 시나리오 | US | 우선순위 |
|---|---------|-----|---------|
| RV-01 | 구매확정 상태에서만 리뷰 작성 가능 | US-801 | P0 |
| RV-02 | 주문 상품당 리뷰 1회만 작성 가능 | US-801 | P0 |
| RV-03 | 텍스트 리뷰 (별점 + 내용 20~1000자) | US-801 | P0 |
| RV-04 | 포토 리뷰 (최대 5장, JPEG/PNG, 5MB 제한) | US-801 | P0 |
| RV-05 | 이미지 S3 업로드 + 썸네일 리사이즈(400x400) | US-801 | P1 |
| RV-06 | 리뷰 수정 (작성 후 7일 이내만) | US-801 | P1 |
| RV-07 | 리뷰 삭제 (본인만) | US-801 | P0 |
| RV-08 | 사이즈 정보 입력 (키/몸무게/핏) | US-802 | P0 |
| RV-09 | 사이즈 핏 분포 시각화 | US-802 | P1 |
| RV-10 | "나와 비슷한 체형" 필터 | US-802 | P1 |
| RV-11 | 텍스트 리뷰 100P / 포토 리뷰 300P / 사이즈 +50P 적립 | US-803 | P0 |
| RV-12 | 리뷰 삭제 시 포인트 회수 | US-803 | P0 |
| RV-13 | 일일 리뷰 포인트 5,000P 한도 | US-803 | P1 |
| RV-14 | 리뷰 집계 (평균 별점, 분포) 실시간 갱신 | US-804 | P0 |
| RV-15 | 리뷰 집계 Redis 캐시 동작 | US-804 | P1 |
| RV-16 | ES 상품 인덱스에 리뷰 집계 동기화 | US-804 | P1 |
| RV-17 | 벌크 집계 재계산 배치 | US-804 | P2 |

**리뷰 시나리오: 17개**

### 4.5 WMS 도메인 (closet-wms)

| # | 시나리오 | 프로세스 | 우선순위 |
|---|---------|---------|---------|
| W-01 | 입고 예정(ASN) 등록 | 입고 | P0 |
| W-02 | 입고 도착 처리 (EXPECTED → ARRIVED) | 입고 | P0 |
| W-03 | 검수 진행 (양품/불량/부분합격 판정) | 입고 | P0 |
| W-04 | 입고 확정 → 재고 반영 이벤트 발행 | 입고 | P0 |
| W-05 | 부분합격 시 양품만 입고, 불량품 격리 | 입고 | P1 |
| W-06 | 입고 취소 처리 | 입고 | P1 |
| W-07 | 적치 로케이션 자동 추천 (빈 공간 기반) | 보관 | P0 |
| W-08 | 적치 작업 생성 → 진행 → 완료 | 보관 | P0 |
| W-09 | Zone별 상품 분류 (일반/냉장/위험물/귀중품) | 보관 | P1 |
| W-10 | 로케이션별 재고 조회 | 보관 | P0 |
| W-11 | 피킹 웨이브 생성 (다건 주문 묶음) | 피킹 | P0 |
| W-12 | 피킹 지시 생성 (SKU + 로케이션 + 수량) | 피킹 | P0 |
| W-13 | 피킹 전략별 동작 (FIFO / FEFO / CLOSEST) | 피킹 | P1 |
| W-14 | 피킹 진행 → 완료 상태 전이 | 피킹 | P0 |
| W-15 | 피킹 수량 부족(SHORT) 처리 | 피킹 | P1 |
| W-16 | 출고 지시 생성 (주문 결제 완료 시 자동) | 출고 | P0 |
| W-17 | 포장 작업 처리 | 출고 | P0 |
| W-18 | 택배 접수 + 송장 발급 | 출고 | P0 |
| W-19 | 출고 완료 → 배송 시작 이벤트 발행 | 출고 | P0 |
| W-20 | 재고 실사 요청 (전수/순환/스팟) | 재고실사 | P0 |
| W-21 | 재고 실사 진행 (실사 수량 입력) | 재고실사 | P0 |
| W-22 | 재고 차이 조정 (시스템 재고 vs 실사 수량) | 재고실사 | P0 |
| W-23 | 차이 조정 이벤트 → closet-inventory 재고 동기화 | 재고실사 | P0 |
| W-24 | WMS 대시보드 (재고 현황, 작업 현황, 입출고 통계) | 대시보드 | P1 |
| W-25 | 입고→적치→피킹→출고 E2E 통합 시나리오 | E2E | P0 |

**WMS 시나리오: 25개**

### 4.6 크로스 도메인 통합 시나리오

| # | 시나리오 | 관련 도메인 | 우선순위 |
|---|---------|-----------|---------|
| X-01 | 주문→재고차감→결제→WMS출고지시→피킹→포장→택배접수→배송완료 E2E | 전체 | P0 |
| X-02 | 배송완료→7일 자동구매확정→정산이벤트 | shipping→order→settlement | P0 |
| X-03 | 반품승인→환불→재고복구 | shipping→payment→inventory | P0 |
| X-04 | 교환→기존재고복구→신규재고선점→재배송 | shipping→inventory | P0 |
| X-05 | 상품등록→ES인덱싱→검색결과 노출 | product→search | P0 |
| X-06 | 리뷰작성→포인트적립→ES집계동기화 | review→member→search | P0 |
| X-07 | 품절→재입고알림신청→WMS입고→재고반영→알림발송 | inventory→wms→notification | P1 |
| X-08 | WMS입고확정→closet-inventory 재고 동기화 | wms→inventory | P0 |

**크로스 도메인 시나리오: 8개**

### QA 시나리오 총계

| 도메인 | 시나리오 수 |
|--------|-----------|
| 배송 (shipping) | 20 |
| 재고 (inventory) | 15 |
| 검색 (search) | 16 |
| 리뷰 (review) | 17 |
| WMS | 25 |
| 크로스 도메인 | 8 |
| **합계** | **101** |

---

## 5. 공수 산정

### 5.1 서비스별 공수

> 크기 기준: S=1~2주, M=2~3주, L=3~4주, XL=4~6주

| 서비스 | 크기 | 예상 공수 | 근거 |
|--------|------|----------|------|
| **closet-inventory** | **M** | 2.5주 | 재고 CRUD + 분산락(Redisson) + Kafka 컨슈머 + 동시성 테스트 |
| **closet-shipping** | **L** | 3주 | 송장등록 + 택배API연동 + 자동구매확정 + 반품 + 교환 (5개 US, 상태머신 복잡) |
| **closet-search** | **M** | 2.5주 | ES 인덱싱 + nori 분석 + 필터/facet + 자동완성 + 인기검색어 |
| **closet-review** | **M** | 2주 | 리뷰 CRUD + S3 이미지 + 사이즈후기 + 집계 + 포인트연동 |
| **closet-wms** | **XL** | 5주 | 5개 서브도메인(입고/보관/피킹/출고/실사), 30개 엔티티 상태머신, 재고동기화 |
| **closet-bff (Phase 2 확장)** | **M** | 2주 | 48개 BFF API, Feign 클라이언트 5개 추가 |
| **기존 모듈 수정** | **S** | 1.5주 | order/member/product/payment 이벤트 추가, common 확장 |
| **인프라 (Kafka/Redis/ES 설정)** | **S** | 1주 | 토픽 17개, Redis 키 10개, ES 인덱스 1개, docker-compose |
| **통합 테스트** | **M** | 2주 | 크로스 도메인 E2E 8개 + 도메인별 통합 |

### 5.2 총 예상 공수

| 구분 | 공수 |
|------|------|
| 개발 (순수 구현) | **19.5주** |
| 코드 리뷰 + 버그 수정 | +20% = **3.9주** |
| QA + 회귀 테스트 | **2주** |
| **총 예상** | **~25.4주 (약 6.4개월, 1인 기준)** |

### 5.3 팀 규모별 예상 일정

| 팀 규모 | 예상 기간 | 비고 |
|---------|----------|------|
| 1명 (풀스택) | ~26주 | 비현실적 |
| 2명 (BE 2) | ~14주 | 병렬도 중간 |
| 3명 (BE 2 + FE 1) | ~10주 | PRD 8주 목표에 근접 (WMS 미포함 시) |
| 4명 (BE 3 + FE 1) | ~8주 | WMS 포함 가능, PRD 목표 달성 가능 |

> **참고**: 원본 PRD의 8주(Sprint 5~8) 계획은 WMS를 포함하지 않은 범위. WMS를 포함하면 최소 +4주(1인 기준) 추가 필요.

---

## 6. 의존 관계 (DAG: 구현 순서)

### 6.1 서비스 간 의존 그래프

```
Layer 0 (의존 없음 - 선행 구현)
├── closet-common 확장 (ErrorCode, Kafka/Redis 공통 설정)
├── closet-product 수정 (Kafka 이벤트 발행 추가)
└── 인프라 설정 (Kafka 토픽, ES 인덱스, docker-compose)

Layer 1 (Layer 0 완료 후)
├── closet-inventory (order.created 컨슈머 필요 → order는 이미 이벤트 발행)
└── closet-search (product.created 컨슈머 필요 → product 이벤트 발행 후)

Layer 2 (Layer 1 완료 후)
├── closet-shipping (inventory 재고 확인, order 상태 변경 연동)
├── closet-review (order CONFIRMED 상태 필요 → shipping 자동 구매확정)
└── closet-wms 기초 (warehouse, location, inbound 관리 → inventory 연동)

Layer 3 (Layer 2 완료 후)
├── closet-wms 심화 (피킹/출고/실사 → shipping 연동)
├── closet-order 수정 (구매확정, 반품/교환 상태)
├── closet-member 수정 (포인트 적립 컨슈머)
└── closet-payment 수정 (반품 환불)

Layer 4 (Layer 3 완료 후)
├── closet-bff 확장 (모든 서비스 클라이언트)
└── 크로스 도메인 통합 테스트
```

### 6.2 DAG 시각화

```
                    ┌─────────────────────────────┐
                    │    Layer 0: 기반 준비         │
                    │ common / product수정 / 인프라  │
                    └──────────┬──────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
        ┌─────▼─────┐   ┌─────▼─────┐          │
        │ inventory  │   │  search   │          │
        │  (Layer 1) │   │ (Layer 1) │          │
        └─────┬──┬──┘   └─────┬─────┘          │
              │  │             │                │
    ┌─────────┘  │     ┌───────┘                │
    │            │     │                        │
┌───▼───┐  ┌────▼─┐  ┌▼──────┐          ┌──────▼──────┐
│shipping│  │review│  │search │          │  wms 기초   │
│(Lay 2) │  │(Ly 2)│  │(done) │          │  (Layer 2)  │
└───┬────┘  └──┬───┘  └───────┘          └──────┬──────┘
    │          │                                │
    │    ┌─────┼──────────┐              ┌──────▼──────┐
    │    │     │          │              │  wms 심화   │
    │    │     │          │              │  (Layer 3)  │
    │    │     │          │              └──────┬──────┘
    │    │     │          │                     │
┌───▼────▼─────▼──────────▼─────────────────────▼──┐
│              Layer 4: BFF + 통합 테스트            │
└──────────────────────────────────────────────────┘
```

### 6.3 권장 구현 순서 (Sprint 배정)

| Sprint | 기간 | 구현 내용 | 선행 조건 |
|--------|------|----------|----------|
| **Sprint 5** (Week 1~2) | 2주 | Layer 0: common 확장, product 이벤트 추가, 인프라 설정 + Layer 1: closet-inventory (US-601~602) | Phase 1 완료 |
| **Sprint 6** (Week 3~4) | 2주 | Layer 1: closet-inventory 완료 (US-603~604) + closet-search (US-701~703) | Sprint 5 |
| **Sprint 7** (Week 5~6) | 2주 | Layer 2: closet-shipping (US-501~505) + closet-search 완료 (US-704~705) | Sprint 6 |
| **Sprint 8** (Week 7~8) | 2주 | Layer 2: closet-review (US-801~804) + 기존 모듈 수정 (order/member/payment) | Sprint 7 |
| **Sprint 9** (Week 9~10) | 2주 | Layer 2~3: closet-wms 기초 (입고/보관) + BFF 확장 (구매자/판매자 API) | Sprint 8 |
| **Sprint 10** (Week 11~12) | 2주 | Layer 3: closet-wms 심화 (피킹/출고/실사) + BFF 확장 (관리자 API) | Sprint 9 |
| **Sprint 11** (Week 13~14) | 2주 | Layer 4: 크로스 도메인 통합 테스트 + QA + 버그 수정 | Sprint 10 |

**총 예상: 14주 (7 Sprint)**

> 원본 PRD 8주(4 Sprint)는 WMS 미포함 범위. WMS 추가로 +6주 필요.
> PRD 원본 범위만 수행 시 Sprint 5~8 (8주) 달성 가능.

---

## 부록 A. WMS 데이터 모델 상세

### A.1 테이블 목록 및 컬럼 수 예상

| 테이블 | 예상 컬럼 수 | 설명 |
|--------|------------|------|
| warehouse | 8 | id, name, code, address, type, is_active, created_at, updated_at |
| location | 12 | id, warehouse_id, zone, aisle, rack, shelf, bin, type, max_capacity, current_usage, created_at, updated_at |
| inbound_order | 10 | id, warehouse_id, supplier_name, expected_date, arrived_at, status, note, created_at, updated_at, created_by |
| inbound_order_item | 8 | id, inbound_order_id, product_id, option_id, sku, expected_quantity, created_at, updated_at |
| inbound_receipt | 8 | id, inbound_order_id, inspection_result, inspected_by, inspected_at, note, created_at, updated_at |
| inbound_receipt_item | 10 | id, inbound_receipt_id, inbound_order_item_id, accepted_quantity, rejected_quantity, rejection_reason, lot_number, created_at, updated_at |
| stock_lot | 10 | id, sku, lot_number, manufactured_date, expiry_date, quantity, status, inbound_receipt_id, created_at, updated_at |
| location_inventory | 8 | id, location_id, sku, lot_id, quantity, status, created_at, updated_at |
| putaway_task | 10 | id, inbound_receipt_item_id, sku, quantity, from_location_id, to_location_id, status, assigned_to, created_at, updated_at |
| picking_wave | 8 | id, warehouse_id, order_count, status, strategy, created_at, updated_at, completed_at |
| picking_task | 10 | id, picking_wave_id, outbound_order_id, status, assigned_to, started_at, completed_at, created_at, updated_at |
| picking_task_item | 10 | id, picking_task_id, sku, location_id, lot_id, requested_quantity, picked_quantity, status, created_at, updated_at |
| outbound_order | 10 | id, order_id, warehouse_id, status, priority, shipping_address, created_at, updated_at, completed_at |
| outbound_order_item | 8 | id, outbound_order_id, product_id, option_id, sku, quantity, created_at, updated_at |
| packing_task | 10 | id, outbound_order_id, status, package_count, weight, packer_id, started_at, completed_at, created_at, updated_at |
| shipment_request | 10 | id, outbound_order_id, carrier, tracking_number, label_url, status, requested_at, shipped_at, created_at, updated_at |
| stocktake_order | 10 | id, warehouse_id, type, status, target_zone, requested_by, started_at, completed_at, created_at, updated_at |
| stocktake_order_item | 8 | id, stocktake_order_id, location_id, sku, system_quantity, actual_quantity, created_at, updated_at |
| stocktake_result | 10 | id, stocktake_order_id, sku, location_id, system_quantity, actual_quantity, difference, adjustment_type, adjusted_by, created_at |

**WMS 테이블: 19개, 총 컬럼: ~178개**

---

## 부록 B. 위험 요소 및 완화 방안

| 위험 | 영향도 | 발생확률 | 완화 방안 |
|------|--------|---------|----------|
| ES nori 플러그인 호환성 | 높음 | 중간 | Docker 이미지에 nori 플러그인 사전 설치, Testcontainers 검증 |
| Redisson 분산락 데드락 | 높음 | 낮음 | 락 타임아웃 3초, 재시도 3회, @Version 2차 안전장치 |
| WMS ↔ inventory 재고 불일치 | 높음 | 중간 | 이벤트 기반 최종 일관성 + 재고 실사로 보정, Saga 패턴 검토 |
| Kafka 메시지 유실 | 높음 | 낮음 | acks=all, 멱등성 보장, DLQ + 재처리 배치 |
| WMS 범위 확장으로 일정 지연 | 중간 | 높음 | WMS를 Phase 2.5로 분리, 입고/출고 기본만 우선 구현 |
| S3 이미지 업로드 성능 | 중간 | 중간 | Presigned URL 직접 업로드, 서버 미경유 |
| 포트 충돌 | 낮음 | 낮음 | 기존 payment(8084) 이후 8085~8089 순차 배정 |

---

## 부록 C. 요약 대시보드

| 항목 | 수량 |
|------|------|
| 신규 서비스 (모듈) | 5개 (shipping, inventory, search, review, **wms**) |
| 수정 서비스 | 7개 (order, member, product, payment, bff, common, external-api) |
| 총 예상 파일 | ~225개 (신규 211 + 수정 14) |
| 신규 DB 테이블 | 30개 |
| BFF API | 48개 (구매자 18 + 판매자 14 + 관리자 16) |
| Kafka 토픽 | 17개 |
| Redis 키 패턴 | 10개 |
| ES 인덱스 | 1개 (closet-products) |
| QA 시나리오 | 101개 |
| 유저스토리 (PRD) | 18개 (US-501~505, US-601~604, US-701~705, US-801~804) + WMS 5개 서브도메인 |
| 예상 공수 (1인) | ~25.4주 |
| 권장 일정 (4인) | 14주 (7 Sprint) |
