# 기술 부채 & 개선사항 전체 분석

> 분석일: 2026-03-23
> 대상: Closet E-Commerce 17개 서비스
> 분석자: Tech Lead
> 분석 방법: 전체 소스 코드 직접 리뷰 (엔티티, 서비스, 컨트롤러, 설정, 테스트, Flyway, Docker Compose)

---

## 1. 요약

| 카테고리 | Critical | High | Medium | Low | 합계 |
|---------|----------|------|--------|-----|------|
| 아키텍처 | 2 | 3 | 2 | 1 | 8 |
| 코드 품질 | 0 | 3 | 4 | 3 | 10 |
| 테스트 | 1 | 2 | 2 | 1 | 6 |
| 보안 | 2 | 2 | 1 | 0 | 5 |
| 성능 | 1 | 2 | 2 | 1 | 6 |
| 인프라 | 1 | 2 | 2 | 1 | 6 |
| 문서 | 0 | 1 | 2 | 1 | 4 |
| **합계** | **7** | **15** | **15** | **8** | **45** |

---

## 2. 상세 분석

### 2.1 아키텍처 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| A-01 | Saga 패턴 미구현 (주문-결제-재고) | **Critical** | OrderService가 ApplicationEventPublisher로 Spring 내부 이벤트만 발행. 결제 실패 시 재고 롤백 메커니즘 없음. Order.place() 후 결제 실패하면 STOCK_RESERVED 상태로 잔류 | 주문 생성 후 결제 실패 시 재고가 영구적으로 예약 상태로 남음. 15분 만료(reservationExpiresAt) 로직은 존재하나 스케줄러 미구현 | Choreography Saga: OrderCreatedEvent → Kafka → InventoryService(예약) → PaymentService(결제) → OrderService(확정). 보상 트랜잭션(OrderCancelledEvent → 재고 해제 + 결제 취소) 구현 | 3~4 스프린트 |
| A-02 | 서비스 간 통신이 동기 HTTP만 사용 | **Critical** | BFF가 Feign으로 member/product/order/payment 동기 호출. 주문 이벤트는 ApplicationEventPublisher(프로세스 내부)만 사용. Kafka 의존성은 order/payment/search/review/shipping/settlement에 있으나 실제 Producer/Consumer 코드 없음 | 서비스 장애 전파, 결합도 높음, 비동기 처리 불가 | Kafka Producer/Consumer 구현: (1) 주문 생성 → Kafka → 재고 예약/검색 인덱싱, (2) 결제 완료 → Kafka → 주문 상태 갱신/배송 생성, (3) 배송 완료 → Kafka → 정산 생성/알림 발송 | 4~5 스프린트 |
| A-03 | Gateway에 Phase 2~4 서비스 라우팅 미등록 | **High** | application.yml에 member/product/order/payment/bff 5개만 등록. shipping(8086), inventory(8087), search(8088), review(8089), promotion(8090), display(8091), seller(8092), cs(8093), settlement(8094), notification(8095), content(8096) 미등록 | Phase 2~4 서비스에 Gateway 경유 접근 불가, 인증/레이트리밋 적용 불가 | application.yml에 12개 라우트 추가. 각 서비스별 Path predicate 정의 | 0.5 스프린트 |
| A-04 | BFF에 Phase 2~4 서비스 미연동 | **High** | BFF client는 ProductServiceClient, OrderServiceClient, PaymentServiceClient, MemberServiceClient 4개만 존재. HomeBffFacade에 `banners = null // Phase 3`, `exhibitions = null // Phase 3`, OrderBffFacade에 `shipment = null // Phase 2` 주석 존재 | 프론트엔드에서 복합 조회 불가 (홈: 배너+기획전, 주문상세: 배송정보, 체크아웃: 쿠폰) | ShippingServiceClient, InventoryServiceClient, ReviewServiceClient, PromotionServiceClient, DisplayServiceClient 추가. HomeBffFacade에 배너/기획전/랭킹 연동, OrderBffFacade에 배송 연동, CheckoutBff에 쿠폰 연동 | 2 스프린트 |
| A-05 | CQRS 미구현 (검색 외) | **High** | Search 서비스만 Elasticsearch 기반 CQRS 구현. 나머지 서비스는 Command/Query 분리 없이 동일 DB에서 읽기/쓰기 처리 | 상품 목록, 랭킹 등 읽기 부하가 높은 API에서 쓰기 DB 성능에 영향 | Phase 1: 상품/주문 목록 조회를 Redis 캐시 기반 Read Model로 분리. Phase 2: 이벤트 기반 Read Model 갱신 | 2~3 스프린트 |
| A-06 | 재고 예약 만료 스케줄러 미구현 | **Medium** | Order 엔티티에 reservationExpiresAt 필드가 있고 15분 TTL이 설정되지만, 만료된 예약을 해제하는 스케줄러/배치 없음 | 결제 미완료 주문의 재고가 영구 예약 상태로 남아 품절 오류 유발 | Spring Scheduler(@Scheduled) 또는 배치로 만료 주문 조회 → 재고 해제 + 주문 상태 FAILED 전이 | 1 스프린트 |
| A-07 | 공통 모듈(closet-common) 의존성 범위 과다 | **Medium** | closet-common이 JPA/Hibernate 의존성을 가지고 있어 BFF, Gateway, Search에서도 간접 의존. BFF/Search는 autoconfigure.exclude로 우회 | 빌드 시간 증가, 불필요한 클래스패스 오염 | closet-common을 closet-common-core(exception, response, vo)와 closet-common-jpa(BaseEntity, JpaAuditingConfig)로 분리 | 1 스프린트 |
| A-08 | 단일 DB 사용 (Database per Service 미적용) | **Low** | 모든 서비스가 동일 MySQL 인스턴스(localhost:3306/closet)에 접속. Flyway 테이블만 서비스별 분리(flyway_schema_history_xxx) | 스키마 충돌 가능, DB 장애 시 전체 서비스 다운, 독립 배포/스케일링 불가 | 서비스별 DB 분리 (closet_member, closet_product, closet_order 등). Docker Compose에 MySQL 인스턴스 추가 또는 스키마 분리 | 향후 |

### 2.2 코드 품질 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| C-01 | BaseEntity 상속 일관성 부재 | **High** | BaseEntity를 상속하는 엔티티: Member, Product, Review, Banner, Exhibition, Seller, Inquiry, Notification, Magazine, Coordination, OotdSnap, Faq. 직접 필드 정의: Order, Payment, Shipment, InventoryItem, Coupon, TimeSale, Settlement, SettlementItem, OrderItem, CartItem, PointHistory, ShipmentStatusHistory, MemberCoupon, PointPolicy, RankingSnapshot, CommissionRate, NotificationTemplate, RestockSubscription | 코드 중복 (id, createdAt, updatedAt, deletedAt 반복 정의), soft delete 누락 위험 | 모든 엔티티를 BaseEntity 상속으로 통일. 특수한 경우(복합키 등)만 예외 허용 | 2 스프린트 |
| C-02 | 패키지 구조 불일관 | **High** | **Type A** (domain/entity, domain/enums, domain/repository 분리): product, review, display, content. **Type B** (domain/ 플랫): member, shipping, inventory, seller, cs, notification. **Type C** (domain/xxx 하위 도메인): order(domain/order, domain/cart, domain/event), promotion(domain/coupon, domain/point, domain/timesale), settlement(domain/commission, domain/settlement). **repository 위치**: member, shipping, inventory, promotion은 최상위 repository/, product, review, display, content, cs, notification, seller는 domain/repository/ | 코드 탐색 혼란, 새 서비스 추가 시 기준 없음 | 표준 패키지 구조 확정: `{service}.domain.entity/`, `{service}.domain.enums/`, `{service}.domain.repository/`, `{service}.application.service/`, `{service}.application.dto/`, `{service}.presentation/`, `{service}.infrastructure/` | 1.5 스프린트 |
| C-03 | DTO 위치 불일관 | **High** | member: presentation/dto/MemberDtos.kt. product: application/dto/ProductDto.kt. order: presentation/dto/OrderDto.kt, CartDto.kt. payment: application/PaymentService.kt 내부에 DTO 정의(!). shipping: presentation/dto/. review: application/dto/. display: application/dto/. content: application/dto/. promotion: presentation/dto/. settlement: presentation/dto/. notification: presentation/dto/. cs: presentation/dto/. seller: presentation/dto/ | 계층 간 DTO 변환 규칙 불명확, 특히 payment에서 서비스 파일에 Request/Response DTO가 함께 정의됨 | 규칙 확정: Request DTO → presentation/dto/, Response DTO → application/dto/ 또는 모두 presentation/dto/로 통일. payment 서비스의 인라인 DTO를 별도 파일로 분리 | 1 스프린트 |
| C-04 | @EntityListeners 누락 (BaseEntity 미사용 시) | **Medium** | BaseEntity는 @EntityListeners(AuditingEntityListener::class) 포함. 그러나 직접 필드 정의한 엔티티 중 @EntityListeners 명시: Order(O), Payment(O), Shipment(O), InventoryItem(O), Coupon(O), TimeSale(O), Settlement(O). 누락 가능성: OrderItem, CartItem, MemberCoupon, PointPolicy, ShipmentStatusHistory 등 하위 엔티티 | createdAt/updatedAt 자동 설정 실패 가능 | BaseEntity 상속으로 통일하거나, 직접 정의 시 @EntityListeners 필수 추가 | 0.5 스프린트 |
| C-05 | enum canTransitionTo 패턴 일관성 | **Medium** | **완전 구현**: MemberGrade, MemberStatus, ProductStatus, OrderStatus, ShipmentStatus, ReviewStatus, CouponStatus, ExhibitionStatus, InquiryStatus, SellerStatus, SettlementStatus, MagazineStatus, TimeSaleStatus. **미구현**: PaymentStatus(confirm/cancel에서 직접 상태 변경, validateTransitionTo 없음), ReturnStatus(canTransitionTo/validateTransitionTo 없음), OrderItemStatus | PaymentStatus, ReturnStatus 등에서 잘못된 상태 전이 가능 | PaymentStatus, ReturnStatus, OrderItemStatus에 canTransitionTo/validateTransitionTo 추가 | 0.5 스프린트 |
| C-06 | columnDefinition = "VARCHAR(30)" 일관성 | **Medium** | 대부분의 enum 필드에 `columnDefinition = "VARCHAR(30)"` 적용. 예외: Review.sizeFeeling은 `VARCHAR(20)`, Review.status는 `VARCHAR(20)`. Banner.isVisible은 올바르게 `TINYINT(1)` 사용 | 일관성 부족으로 DB 마이그레이션 시 혼란 | enum 길이에 따라 통일: 일반 상태 enum → VARCHAR(30), 짧은 enum → VARCHAR(20) 명시적 규칙 정의 | 0.5 스프린트 |
| C-07 | Money VO 사용 불일관 | **Medium** | member: pointBalance가 Int (Money 미사용). product: Money VO 사용. order: Money VO 사용. payment: Money VO 사용. promotion: BigDecimal 직접 사용(Coupon.discountValue, TimeSale.salePrice). settlement: BigDecimal 직접 사용 | 금액 관련 비즈니스 로직 캡슐화 누락, 음수 금액 검증 누락 가능 | 금액 필드는 모두 Money VO로 통일. 포인트(Int)는 별도 Point VO 도입 고려 | 1 스프린트 |
| C-08 | Flyway 테이블명 일관성 | **Low** | member, inventory, seller, cs, notification: 기본값(flyway_schema_history) 사용. product, order, payment, shipping, review, promotion, display, settlement, content: 서비스별 테이블명(flyway_schema_history_xxx) 사용 | 단일 DB 환경에서 스키마 충돌 가능 | 모든 서비스에 `table: flyway_schema_history_{service}` 명시 | 0.3 스프린트 |
| C-09 | Kafka 의존성만 있고 실제 구현 없음 | **Low** | order, payment, search, review, shipping, settlement에 spring-kafka 의존성 존재. 그러나 Producer/Consumer 클래스 없음. OrderService만 ApplicationEventPublisher 사용 (Kafka 아님) | 불필요한 의존성, Kafka 연결 실패 시 기동 오류 가능 | Kafka 구현 전까지 의존성 제거하거나, 실제 Producer/Consumer 구현 | 0.5 스프린트 |
| C-10 | Order 엔티티의 item 관계 미정의 | **Low** | Order 엔티티에 @OneToMany items 관계가 없음. OrderService에서 orderItemRepository.findByOrderId()로 별도 조회. Product는 @OneToMany options/images/sizeGuides 정의 | 도메인 모델 불완전, 지연 로딩 전략 활용 불가 | Order에 @OneToMany items 관계 추가 검토 (성능 트레이드오프 고려) | 0.5 스프린트 |

### 2.3 테스트 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| T-01 | 통합 테스트 전무 | **Critical** | BaseIntegrationTest가 정의되어 있으나, 이를 상속하는 통합 테스트가 하나도 없음. 모든 테스트가 MockK 기반 단위 테스트 | DB 쿼리, 트랜잭션, Flyway 마이그레이션 검증 불가. 실제 환경에서만 발견되는 버그 위험 | 서비스별 최소 1개 통합 테스트 작성: Repository 테스트 + Service 트랜잭션 테스트. BaseIntegrationTest 상속 활용 | 3 스프린트 |
| T-02 | Phase 1 서비스 테스트 커버리지 부족 | **High** | member: 1파일(BehaviorSpec 4 TC), product: 1파일(BehaviorSpec 6 TC), payment: 0파일(!!), bff: 0파일, gateway: 0파일 | 핵심 결제 로직에 테스트 없음, Gateway 인증 필터 검증 없음 | payment: PaymentService 단위 테스트 + PaymentStatus 상태 전이 테스트. gateway: JwtAuthenticationFilter 단위 테스트. bff: Facade 단위 테스트 | 2 스프린트 |
| T-03 | Phase 2~4 테스트 커버리지 편차 | **High** | shipping: 4파일(~98 TC), inventory: 2파일(~73 TC), promotion: 4파일(~96 TC), settlement: 3파일(~86 TC), content: 3파일(~56 TC). 반면 search: 1파일, review: 1파일, seller: 1파일, cs: 1파일 | 검색, 리뷰, 셀러, CS 서비스의 안정성 검증 부족 | 각 서비스 최소 도메인 테스트 + 서비스 테스트 2파일 이상 확보 | 2 스프린트 |
| T-04 | E2E 테스트 미구현 | **Medium** | 서비스 간 연동 테스트 없음. BFF → member/product/order/payment 호출 체인 검증 불가 | 전체 흐름 (회원가입 → 상품 조회 → 주문 → 결제) 검증 불가 | TestContainers + WireMock 기반 E2E 테스트 또는 k6 시나리오 활용 | 2 스프린트 |
| T-05 | 도메인 모델 테스트 부족 (Phase 1) | **Medium** | member: MemberGrade/MemberStatus canTransitionTo 테스트 없음. product: ProductStatus 테스트 없음. order: OrderStatusTest만 있고 Order 엔티티 비즈니스 로직 테스트 없음 | 상태 전이 규칙 변경 시 회귀 버그 위험 | 모든 enum의 canTransitionTo에 대한 파라미터화 테스트 추가 | 1 스프린트 |
| T-06 | 테스트 픽스처 미활용 | **Low** | closet-common에 MemberFixture, OrderFixture, ProductFixture 정의. 실제 테스트에서 사용하지 않고 각 테스트에서 직접 객체 생성 | 테스트 코드 중복, 픽스처 변경 시 반영 안 됨 | 공통 픽스처 활용으로 테스트 코드 정리 | 0.5 스프린트 |

### 2.4 보안 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| S-01 | JWT Secret 하드코딩 | **Critical** | Gateway application.yml: `jwt.secret: closet-member-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256`. Member application.yml: 동일 값 하드코딩. Git 히스토리에 노출 | 시크릿 키 유출 시 모든 JWT 토큰 위조 가능 | 환경변수 또는 Vault/AWS Secrets Manager로 주입. `${JWT_SECRET}` 형태로 변경 | 0.5 스프린트 |
| S-02 | Spring Security 미적용 | **Critical** | member 서비스에 JwtAuthenticationFilter가 OncePerRequestFilter 수동 구현. PERMIT_ALL_PATHS 하드코딩. CSRF/CORS/세션 관리 없음. Phase 2~4 서비스에는 인증 필터 자체가 없음(Gateway에서만 처리 가정) | Gateway 우회 시 모든 서비스에 무인증 접근 가능. CORS 설정은 Gateway CorsConfig에만 존재 | Spring Security 도입 (최소 JWT 필터 체인). 서비스별 최소한의 SecurityFilterChain 구성. X-Member-Id 헤더 기반 내부 인증 표준화 | 2 스프린트 |
| S-03 | DB 비밀번호 평문 노출 | **High** | 모든 서비스 application.yml: `username: root`, `password: closet` 평문. docker-compose: `MYSQL_ROOT_PASSWORD: closet` | 소스 코드에 DB 접근 정보 노출 | 환경변수/프로필 분리. application-local.yml (gitignore) + application-docker.yml (환경변수 참조) | 0.5 스프린트 |
| S-04 | API 인증/인가 일관성 부재 | **High** | Gateway publicPaths: register, login, refresh, GET products/categories/brands/bff. 그 외 모두 JWT 필요. 그러나 Phase 2~4 서비스(shipping, inventory, search 등)는 Gateway 라우트 미등록이므로 직접 접근 시 인증 없음 | 서비스 직접 접근 시 보안 우회 | (1) 모든 서비스를 Gateway 라우트에 등록, (2) 서비스 간 통신은 내부 네트워크로 제한(Docker network), (3) 관리자 API 분리 | 1 스프린트 |
| S-05 | 입력 검증 부분 적용 | **Medium** | member: @NotBlank, @Email, @Size 적용(O). product: @NotBlank, @NotNull, @Min 적용(O). order: @NotNull, @NotEmpty, @NotBlank, @Min 적용(O). payment: @Valid 미사용, ConfirmPaymentRequest에 검증 어노테이션 없음(!). shipping/inventory/promotion/display/seller/cs/settlement/notification/content: DTO에 검증 어노테이션 적용 수준 확인 필요 | 결제 API에 잘못된 데이터 전달 가능 | payment DTO에 @NotBlank, @NotNull, @Min 추가. 모든 서비스 DTO 검증 어노테이션 감사 | 0.5 스프린트 |

### 2.5 성능 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| P-01 | N+1 쿼리 가능성 (OrderService) | **Critical** | OrderService.findByMemberId(): `orderRepository.findByMemberIdAndDeletedAtIsNull(memberId, pageable).map { order -> orderItemRepository.findByOrderId(order.id) }` — 주문 N개마다 OrderItem SELECT 추가 쿼리 | 주문 목록 조회 시 쿼리 수 = 1(주문) + N(아이템) | (1) @EntityGraph 또는 fetch join 사용, (2) Order에 @OneToMany items 관계 정의 후 batch fetch 활용, (3) QueryDSL로 한 번에 조회 | 1 스프린트 |
| P-02 | 캐싱 미적용 (상품/카테고리/브랜드) | **High** | product, promotion, display에 Redis 의존성이 있으나 실제 @Cacheable이나 RedisTemplate 사용 코드 없음. 상품 목록, 카테고리 트리, 브랜드 목록 등 읽기 빈도 높은 API에 캐시 없음 | 모든 조회가 DB 직접 접근, 트래픽 증가 시 DB 부하 | Spring Cache + Redis: (1) 카테고리 트리(TTL 1h), (2) 브랜드 목록(TTL 30m), (3) 상품 상세(TTL 5m), (4) 홈 BFF 응답(TTL 1m) | 1.5 스프린트 |
| P-03 | 검색 인덱스 실시간 동기화 없음 | **High** | ProductSearchService.syncFromProductService()가 REST API 폴링 방식. 상품 변경 시 실시간 인덱싱 없음 (Kafka Consumer 미구현) | 상품 수정/등록 후 검색 결과에 반영되기까지 지연 | Product 변경 이벤트 → Kafka → SearchService Consumer로 실시간 인덱싱 | 1 스프린트 |
| P-04 | HikariCP 설정 불일관 | **Medium** | member, product, order, payment, shipping, settlement: maximum-pool-size=30, minimum-idle=5, connection-timeout=5000. inventory: maximum-pool-size=30, minimum-idle=5, connection-timeout=5000. promotion, display, cs, seller: HikariCP 설정 없음(기본값 10). notification, content: maximum-pool-size=30만 설정 | 서비스별 DB 커넥션 풀 크기 편차, 기본값 서비스에서 커넥션 부족 가능 | 모든 서비스 HikariCP 설정 통일: maximum-pool-size=30, minimum-idle=5, connection-timeout=5000, max-lifetime=600000 | 0.3 스프린트 |
| P-05 | Pagination 구현 일관성 | **Medium** | product: QueryDSL 기반 Page 반환, @PageableDefault(size=20). order: Spring Data Page 반환, @PageableDefault(size=20). 기타 서비스: Pageable 미적용(리스트 전체 반환) | 데이터 증가 시 전체 조회 API 성능 급감 | 목록 조회 API에 Pageable 파라미터 적용 표준화 | 1 스프린트 |
| P-06 | BFF 병렬 호출 에러 핸들링 부족 | **Low** | HomeBffFacade, OrderBffFacade에서 CompletableFuture.allOf().join() 사용. 하나의 서비스 실패 시 전체 실패. runCatching은 paymentFuture에만 적용 | 부분 서비스 장애 시 전체 BFF 응답 실패 | Circuit Breaker(Resilience4j) 도입, 부분 실패 허용(fallback 값 반환) | 1 스프린트 |

### 2.6 인프라/운영 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| I-01 | Docker Compose에 Phase 2~4 서비스 미포함 | **Critical** | docker-compose.yml에 closet-gateway, closet-member, closet-product, closet-order, closet-payment만 정의. BFF, Shipping, Inventory, Search, Review, Promotion, Display, Seller, CS, Settlement, Notification, Content 미포함 | 도커 환경에서 12개 서비스 실행 불가 | 나머지 12개 서비스를 docker-compose.yml에 추가. Dockerfile이 없는 서비스는 Dockerfile 작성 | 1 스프린트 |
| I-02 | Zipkin 트레이싱 설정 불일관 | **High** | member~settlement: `zipkin.tracing.endpoint` 설정 있음. notification, content: Zipkin 미설정(OTLP만). 일부 서비스 metrics 아래 빈 키 존재(`metrics:` 다음에 내용 없음) | 분산 트레이싱에 일부 서비스 누락 | 모든 서비스에 동일한 tracing 설정 블록 적용 (OTLP + Zipkin) | 0.3 스프린트 |
| I-03 | Actuator endpoint 일관성 | **High** | 모든 서비스: `include: health,info,prometheus,metrics`. Gateway만 추가: `gateway`. 일부 서비스에서 `metrics:` 키 아래 빈 값(구문 오류 가능성) | 모니터링 메트릭 수집 누락 가능 | 공통 application-monitoring.yml 프로필 생성하여 모든 서비스에서 include | 0.3 스프린트 |
| I-04 | Dockerfile 미확인 | **Medium** | docker-compose.yml에서 각 서비스 build.context로 Dockerfile 참조하나, Phase 2~4 서비스에 Dockerfile 존재 여부 미확인 | 도커 빌드 실패 가능 | 모든 서비스에 표준 Dockerfile 추가 (multi-stage build: Gradle 빌드 → JRE 이미지) | 0.5 스프린트 |
| I-05 | Logback 설정 없음 | **Medium** | 모든 서비스에 logback-spring.xml 미설정. inventory만 application.yml에 `logging.level` 정의 | 로그 형식 불일관, 구조화 로깅 불가, Loki 수집 시 파싱 어려움 | 공통 logback-spring.xml 작성: JSON 형식, traceId/spanId 포함, 서비스명 태그 | 0.5 스프린트 |
| I-06 | default_batch_fetch_size 불일관 | **Low** | member~review, promotion~content: `default_batch_fetch_size: 100` 설정. inventory: 미설정 | N+1 방지 효과 불일관 | 모든 JPA 서비스에 `default_batch_fetch_size: 100` 통일 | 0.1 스프린트 |

### 2.7 문서 부채

| # | 항목 | 심각도 | 현재 상태 | 영향 | 개선 방안 | 예상 공수 |
|---|------|--------|----------|------|----------|----------|
| D-01 | API 문서 (Swagger/OpenAPI) 미적용 | **High** | 모든 서비스에 springdoc-openapi 의존성 없음. Swagger UI 접근 불가 | 프론트엔드/QA 팀이 API 스펙 파악 어려움, 수동 문서 관리 필요 | springdoc-openapi-starter-webmvc-ui 의존성 추가 + Controller에 @Operation/@Schema 어노테이션 | 2 스프린트 |
| D-02 | Phase 2~4 서비스 API 스펙 미문서화 | **Medium** | docs/ 디렉토리 내 ERD/설계 문서 존재 여부 미확인. 12개 서비스의 API endpoint, request/response 형식이 코드에서만 파악 가능 | 신규 개발자 온보딩 비용 증가 | Swagger 자동 문서 + 주요 시나리오별 API 가이드 작성 | 1 스프린트 |
| D-03 | ERD 업데이트 미확인 | **Medium** | Phase 2~4 서비스 추가 후 ERD 갱신 여부 불명 | 테이블 관계 파악 어려움 | Flyway SQL 기반으로 ERD 자동 생성 도구 활용 (SchemaSpy, dbdocs.io) | 0.5 스프린트 |
| D-04 | Kafka 토픽/이벤트 설계 문서 없음 | **Low** | OrderEvent 클래스(OrderCreatedEvent, OrderPaidEvent, OrderCancelledEvent) 정의만 존재. 토픽 명세, 파티션 전략, 컨슈머 그룹 문서 없음 | Kafka 구현 시 설계 기준 없음 | 이벤트 카탈로그 작성: 토픽명, 키 전략, 스키마, 컨슈머 목록 | 0.5 스프린트 |

---

## 3. 서비스별 일관성 매트릭스

| 서비스 | 패키지구조 | BaseEntity | @EntityListeners | columnDef | Enum패턴 | 테스트수(파일) | HikariCP | Actuator | Zipkin | Flyway테이블 | @Valid |
|--------|-----------|-----------|-----------------|-----------|---------|-------------|----------|----------|--------|-------------|--------|
| member | TypeB(domain/) | O | O(상속) | O(VARCHAR30) | O(canTransitionTo) | 1 | 30/5/5000 | O | O | 기본값 | O |
| product | TypeA(domain/entity) | O | O(상속) | O(VARCHAR30) | O | 1 | 30/5/5000 | O | O | _product | O |
| order | TypeC(domain/order) | X(직접) | O(명시) | O(VARCHAR30) | O | 2 | 30/5/5000 | O | O | _order | O |
| payment | 플랫(domain/) | X(직접) | O(명시) | O(VARCHAR30) | X(validateTransitionTo 없음) | 0 | 30/5/5000 | O | O | _payment | X |
| gateway | N/A | N/A | N/A | N/A | N/A | 0 | N/A | O | O | N/A | N/A |
| bff | N/A(client/facade) | N/A | N/A | N/A | N/A | 0 | N/A | O | O | N/A | N/A |
| shipping | TypeB(domain/) | X(직접) | O(명시) | O(VARCHAR30) | O | 4 | 30/5/5000 | O | O | _shipping | 확인필요 |
| inventory | TypeB(domain/) | X(직접) | O(명시) | O(DATETIME6) | N/A(enum없음) | 2 | 30/5/5000 | O | O | 기본값 | 확인필요 |
| search | infra기반 | N/A(ES) | N/A | N/A | N/A | 1 | N/A | O | O | N/A | O |
| review | TypeA(domain/entity) | O | O(상속) | O(VARCHAR20) | O | 1 | 30/5/5000 | O | O | _review | 확인필요 |
| promotion | TypeC(domain/coupon) | X(직접) | O(명시) | O(VARCHAR30) | O | 4 | 미설정 | O | O | _promotion | 확인필요 |
| display | TypeA(domain/entity) | O | O(상속) | O(VARCHAR30) | O | 3 | 미설정 | O | O | _display | 확인필요 |
| seller | TypeB(domain/) | O | O(상속) | O(VARCHAR30) | O | 1 | 미설정 | O | O | 기본값 | 확인필요 |
| cs | TypeB(domain/) | O | O(상속) | O(VARCHAR30) | O | 1 | 미설정 | O | O | 기본값 | 확인필요 |
| settlement | TypeC(domain/settlement) | X(직접) | O(명시) | O(VARCHAR30) | O | 3 | 30/5/5000 | O | O | _settlement | 확인필요 |
| notification | TypeB(domain/) | O | O(상속) | O(VARCHAR30) | N/A | 3 | 30 only | O | X | 기본값 | 확인필요 |
| content | TypeA(domain/entity) | O | O(상속) | O(VARCHAR30) | O | 3 | 30 only | O | X | _content | 확인필요 |

### 범례
- **패키지구조**: TypeA=domain/entity+enums+repository, TypeB=domain/플랫, TypeC=domain/하위도메인
- **BaseEntity**: O=상속, X=직접 필드 정의
- **Enum패턴**: O=canTransitionTo+validateTransitionTo 구현, X=미구현
- **HikariCP**: max/min/timeout 값 또는 "미설정"(기본값 10)
- **Flyway테이블**: "기본값"=flyway_schema_history, "_xxx"=flyway_schema_history_xxx

---

## 4. 우선순위 로드맵

### 즉시 해결 (P0) — 운영 안정성 + 보안

| 항목 | 관련 이슈 | 예상 공수 |
|------|----------|----------|
| JWT Secret 환경변수화 | S-01 | 0.5 스프린트 |
| DB 비밀번호 환경변수화 | S-03 | 0.5 스프린트 |
| Gateway에 Phase 2~4 라우트 추가 | A-03, S-04 | 0.5 스프린트 |
| Payment 테스트 추가 | T-02 | 0.5 스프린트 |
| Payment DTO @Valid 추가 | S-05 | 0.3 스프린트 |
| PaymentStatus enum 패턴 통일 | C-05 | 0.3 스프린트 |
| HikariCP/Flyway 설정 통일 | P-04, C-08 | 0.3 스프린트 |

### 다음 스프린트 (P1) — 핵심 아키텍처 + 성능

| 항목 | 관련 이슈 | 예상 공수 |
|------|----------|----------|
| 재고 예약 만료 스케줄러 | A-06 | 1 스프린트 |
| N+1 쿼리 해결 (OrderService) | P-01 | 1 스프린트 |
| Docker Compose Phase 2~4 추가 | I-01 | 1 스프린트 |
| BFF Phase 2~4 연동 | A-04 | 2 스프린트 |
| 캐싱 적용 (상품/카테고리) | P-02 | 1.5 스프린트 |
| 트레이싱 설정 통일 | I-02, I-03 | 0.3 스프린트 |
| Swagger/OpenAPI 적용 | D-01 | 2 스프린트 |
| 통합 테스트 작성 (Phase 1) | T-01 | 1.5 스프린트 |

### Phase 5에서 (P2) — 구조 개선

| 항목 | 관련 이슈 | 예상 공수 |
|------|----------|----------|
| Saga 패턴 구현 (주문-결제-재고) | A-01 | 3~4 스프린트 |
| Kafka Producer/Consumer 구현 | A-02 | 4~5 스프린트 |
| BaseEntity 상속 통일 | C-01 | 2 스프린트 |
| 패키지 구조 표준화 | C-02 | 1.5 스프린트 |
| DTO 위치 표준화 | C-03 | 1 스프린트 |
| Money VO 사용 통일 | C-07 | 1 스프린트 |
| Logback JSON 로깅 | I-05 | 0.5 스프린트 |
| Spring Security 도입 | S-02 | 2 스프린트 |
| Circuit Breaker (Resilience4j) | P-06 | 1 스프린트 |

### 향후 (P3) — 장기 개선

| 항목 | 관련 이슈 | 예상 공수 |
|------|----------|----------|
| Database per Service 분리 | A-08 | 장기 |
| CQRS 확대 적용 | A-05 | 2~3 스프린트 |
| closet-common 모듈 분리 | A-07 | 1 스프린트 |
| E2E 테스트 구현 | T-04 | 2 스프린트 |
| 이벤트 카탈로그 문서화 | D-04 | 0.5 스프린트 |

---

## 5. 핵심 발견사항 요약

### 잘 된 부분
1. **enum 상태 전이 패턴**: 13/15개 상태 enum에 canTransitionTo/validateTransitionTo 일관 구현
2. **BaseEntity + JPA Auditing**: 공통 모듈에 잘 정의되어 있고, 다수 서비스에서 활용
3. **Money VO**: 금액 연산 캡슐화 (연산자 오버로딩, 불변 보장, 0 이상 제약)
4. **ApiResponse 표준화**: 모든 서비스에서 `ApiResponse.ok()`, `ApiResponse.created()`, `ApiResponse.fail()` 통일
5. **GlobalExceptionHandler**: 비즈니스 예외, 검증 예외, 미처리 예외 계층적 처리
6. **Virtual Thread 활용**: 대부분 서비스에서 `spring.threads.virtual.enabled: true` 설정
7. **분산 락 (InventoryLockService)**: Redis SETNX + Lua 스크립트 기반 안전한 분산 락 구현
8. **Optimistic Lock (InventoryItem)**: @Version 필드로 동시성 제어
9. **모니터링 인프라**: Prometheus + Grafana + Loki + Tempo + Zipkin 풀스택 구성

### 가장 긴급한 3가지
1. **JWT Secret 하드코딩** (S-01): 즉시 환경변수로 전환 필요
2. **Saga/보상 트랜잭션 없음** (A-01): 주문-결제-재고 정합성 보장 불가
3. **Payment 서비스 테스트 0건** (T-02): 핵심 결제 로직에 검증 전혀 없음
