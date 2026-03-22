# Phase 1 도메인 구현 현황

> 작성일: 2026-03-22
> 상태: Phase 1 MVP 구현 완료

## 1. 서비스 구성

| 서비스 | 포트 | 모듈 | 역할 | 상태 |
|--------|------|------|------|------|
| Gateway | 8080 | closet-gateway | 라우팅, JWT 인증, CORS, Rate Limiting, Request Logging | 완료 |
| Member | 8081 | closet-member | 회원, 인증(JWT), 배송지, 포인트 | 완료 |
| Product | 8082 | closet-product | 상품, 카테고리, 브랜드, 옵션, 이미지, 사이즈 가이드 | 완료 |
| Order | 8083 | closet-order | 주문, 장바구니, 주문 상태 이력, 도메인 이벤트 | 완료 |
| Payment | 8084 | closet-payment | 결제 (스켈레톤 — Application 클래스만 존재) | 스켈레톤 |
| BFF | 8085 | closet-bff | 서비스 오케스트레이션 (Feign + Virtual Threads) | 완료 |

## 2. 서비스별 도메인 모델 상세

### 2.1 Member Service

#### 엔티티

**Member** (Aggregate Root, `BaseEntity` 상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| email | String | 이메일 (로그인 ID) | NOT NULL, UNIQUE, max 200 |
| passwordHash | String? | 비밀번호 (BCrypt) | max 200 |
| name | String | 이름 | NOT NULL, max 50 |
| phone | String? | 전화번호 | max 20 |
| grade | MemberGrade | 회원 등급 | NOT NULL, default NORMAL |
| pointBalance | Int | 포인트 잔액 | NOT NULL, default 0 |
| status | MemberStatus | 회원 상태 | NOT NULL, default ACTIVE |

비즈니스 메서드:
- `register()` — 팩토리 메서드 (NORMAL 등급, 0 포인트, ACTIVE 상태)
- `withdraw()` — 탈퇴 (상태 전이 검증 + softDelete)
- `upgradeGrade(newGrade)` — 등급 변경 (전이 규칙 검증)
- `earnPoints(amount)` — 포인트 적립 (0 초과 검증)
- `usePoints(amount)` — 포인트 사용 (잔액 부족 시 BusinessException)

**ShippingAddress** (`BaseEntity` 상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| memberId | Long | 회원 ID | NOT NULL |
| name | String | 수령인 이름 | NOT NULL, max 50 |
| phone | String | 수령인 전화번호 | NOT NULL, max 20 |
| zipCode | String | 우편번호 | NOT NULL, max 10 |
| address | String | 주소 | NOT NULL, max 200 |
| detailAddress | String? | 상세주소 | max 200 |
| isDefault | Boolean | 기본 배송지 여부 | NOT NULL, TINYINT(1) |

비즈니스 메서드:
- `update(name, phone, zipCode, address, detailAddress)` — 배송지 정보 수정
- `markAsDefault()` / `unmarkDefault()` — 기본 배송지 토글

**PointHistory** (독립 엔티티, BaseEntity 미상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| id | Long | PK | AUTO_INCREMENT |
| memberId | Long | 회원 ID | NOT NULL |
| type | PointType | 변동 유형 | NOT NULL |
| amount | Int | 변동 금액 | NOT NULL |
| balanceAfter | Int | 변동 후 잔액 | NOT NULL |
| reason | String | 사유 | NOT NULL, max 200 |
| referenceId | String? | 참조 ID (주문번호 등) | max 100 |
| createdAt | LocalDateTime | 생성일시 | NOT NULL, DATETIME(6) |

팩토리 메서드: `earn()`, `use()`

#### Enum

**MemberStatus**: `ACTIVE`, `INACTIVE`, `WITHDRAWN`
- 상태 전이 규칙:
  - ACTIVE -> INACTIVE, WITHDRAWN
  - INACTIVE -> ACTIVE
  - WITHDRAWN -> (없음, 터미널 상태)

**MemberGrade**: `NORMAL`, `SILVER`, `GOLD`, `PLATINUM`
- 상태 전이 규칙 (1단계씩):
  - NORMAL -> SILVER
  - SILVER -> NORMAL, GOLD
  - GOLD -> SILVER, PLATINUM
  - PLATINUM -> GOLD
- 포인트 적립률: NORMAL(1%), SILVER(2%), GOLD(3%), PLATINUM(5%)

**PointType**: `EARN`, `USE`, `EXPIRE`, `CANCEL`

### 2.2 Product Service

#### 엔티티

**Product** (Aggregate Root, `BaseEntity` 상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| name | String | 상품명 | NOT NULL, max 100 |
| description | String | 상품 상세 설명 | NOT NULL, TEXT |
| brandId | Long | 브랜드 ID | NOT NULL |
| categoryId | Long | 카테고리 ID | NOT NULL |
| basePrice | Money | 정가 | NOT NULL, BIGINT |
| salePrice | Money | 판매가 | NOT NULL, BIGINT |
| discountRate | Int | 할인율 (%) | NOT NULL, default 0 |
| status | ProductStatus | 상품 상태 | NOT NULL, default DRAFT |
| season | Season? | 시즌 | nullable |
| fitType | FitType? | 핏 타입 | nullable |
| gender | Gender? | 성별 | nullable |
| options | MutableList<ProductOption> | 옵션 목록 | OneToMany, CascadeType.ALL |
| images | MutableList<ProductImage> | 이미지 목록 | OneToMany, CascadeType.ALL |
| sizeGuides | MutableList<SizeGuide> | 사이즈 가이드 | OneToMany, CascadeType.ALL |

비즈니스 메서드:
- `activate()` / `deactivate()` / `markSoldOut()` — 상태 전이 (검증 포함)
- `changeStatus(target)` — 범용 상태 변경
- `updatePrice(basePrice, salePrice, discountRate)` — 가격 변경 (판매가 <= 정가 검증)
- `addOption(option)` / `removeOption(optionId)` — 옵션 관리
- `addImage(image)` — 이미지 추가
- `update(...)` — 전체 정보 수정

**ProductOption** (독립 엔티티, AuditingEntityListener)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| size | Size | 사이즈 | NOT NULL |
| colorName | String | 색상명 | NOT NULL, max 50 |
| colorHex | String | 색상 HEX 코드 | NOT NULL, max 7 |
| skuCode | String | SKU 코드 | NOT NULL, UNIQUE, max 50 |
| additionalPrice | Money | 추가 금액 | NOT NULL, default 0 |
| product | Product? | 상품 (ManyToOne) | LAZY |

**ProductImage** (독립 엔티티)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| imageUrl | String | 이미지 URL | NOT NULL, max 500 |
| type | ImageType | 이미지 유형 | NOT NULL |
| sortOrder | Int | 노출 순서 | NOT NULL, default 0 |
| product | Product? | 상품 (ManyToOne) | LAZY |

**SizeGuide** (독립 엔티티)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| size | String | 사이즈명 | NOT NULL, max 30 |
| shoulderWidth | BigDecimal? | 어깨너비 (cm) | DECIMAL(6,1) |
| chestWidth | BigDecimal? | 가슴단면 (cm) | DECIMAL(6,1) |
| totalLength | BigDecimal? | 총장 (cm) | DECIMAL(6,1) |
| sleeveLength | BigDecimal? | 소매길이 (cm) | DECIMAL(6,1) |
| product | Product? | 상품 (ManyToOne) | LAZY |

**Brand** (`BaseEntity` 상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| name | String | 브랜드명 | NOT NULL, max 100 |
| logoUrl | String? | 로고 이미지 URL | max 500 |
| description | String? | 브랜드 소개 | TEXT |
| sellerId | Long | 셀러 ID | NOT NULL |
| status | Boolean | 활성 여부 | NOT NULL, TINYINT(1) |

**Category** (`BaseEntity` 상속)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| parentId | Long? | 부모 카테고리 ID | nullable |
| name | String | 카테고리명 | NOT NULL, max 50 |
| depth | Int | 깊이 (1=대, 2=중, 3=소) | NOT NULL |
| sortOrder | Int | 노출 순서 | NOT NULL, default 0 |
| status | Boolean | 활성 여부 | NOT NULL, TINYINT(1) |

#### Enum

**ProductStatus**: `DRAFT`, `ACTIVE`, `SOLD_OUT`, `INACTIVE`
- 상태 전이 규칙:
  - DRAFT -> ACTIVE
  - ACTIVE -> SOLD_OUT, INACTIVE
  - SOLD_OUT -> ACTIVE
  - INACTIVE -> ACTIVE

**Season**: `SS`, `FW`, `ALL`

**FitType**: `OVERSIZED`, `REGULAR`, `SLIM`

**Gender**: `MALE`, `FEMALE`, `UNISEX`

**Size**: `XS`, `S`, `M`, `L`, `XL`, `XXL`, `XXXL`, `FREE`

**ImageType**: `MAIN`, `DETAIL`

#### 커스텀 리포지토리

**ProductRepositoryCustom** — QueryDSL 기반 동적 필터링 인터페이스
- `findByFilter(categoryId, brandId, minPrice, maxPrice, status, pageable): Page<Product>`

### 2.3 Order Service

#### 엔티티

**Order** (Aggregate Root, AuditingEntityListener)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| orderNumber | String | 주문번호 | NOT NULL, UNIQUE, max 30 |
| memberId | Long | 회원 ID | NOT NULL |
| sellerId | Long | 셀러 ID | NOT NULL |
| totalAmount | Money | 총 상품금액 | NOT NULL, DECIMAL(15,2) |
| discountAmount | Money | 할인금액 | NOT NULL, DECIMAL(15,2) |
| shippingFee | Money | 배송비 | NOT NULL, DECIMAL(15,2) |
| paymentAmount | Money | 결제금액 | NOT NULL, DECIMAL(15,2) |
| status | OrderStatus | 주문 상태 | NOT NULL, default PENDING |
| receiverName | String | 수령인명 | NOT NULL, max 50 |
| receiverPhone | String | 수령인 전화번호 | NOT NULL, max 20 |
| zipCode | String | 우편번호 | NOT NULL, max 10 |
| address | String | 주소 | NOT NULL, max 200 |
| detailAddress | String | 상세주소 | NOT NULL, max 200 |
| reservationExpiresAt | LocalDateTime? | 재고 예약 만료 시각 | DATETIME(6) |
| orderedAt | LocalDateTime? | 주문일시 | DATETIME(6) |

비즈니스 메서드:
- `create(...)` — 팩토리 메서드 (주문번호 자동 생성, 금액 계산)
- `generateOrderNumber()` — `yyyyMMddHHmmss` + 6자리 랜덤
- `place()` — 주문 확정 (STOCK_RESERVED + 15분 예약 만료)
- `pay()` / `prepare()` / `ship()` / `deliver()` / `confirm()` — 상태 전이
- `cancel(reason)` — 취소 (PENDING/STOCK_RESERVED/PAID에서만 가능)
- `requestReturn(itemId, reason)` — 반품 요청 (DELIVERED에서만 가능)
- `calculatePaymentAmount()` — 결제금액 = 총액 - 할인 + 배송비

**OrderItem** (AuditingEntityListener)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| orderId | Long | 주문 ID | NOT NULL |
| productId | Long | 상품 ID | NOT NULL |
| productOptionId | Long | 상품 옵션 ID | NOT NULL |
| productName | String | 상품명 (스냅샷) | NOT NULL, max 200 |
| optionName | String | 옵션명 (스냅샷) | NOT NULL, max 200 |
| categoryId | Long | 카테고리 ID | NOT NULL |
| quantity | Int | 수량 | NOT NULL |
| unitPrice | Money | 단가 | NOT NULL, DECIMAL(15,2) |
| totalPrice | Money | 합계금액 | NOT NULL, DECIMAL(15,2) |
| status | OrderItemStatus | 주문항목 상태 | NOT NULL, default ORDERED |

비즈니스 메서드:
- `create(...)` — 팩토리 메서드 (totalPrice = unitPrice * quantity)
- `cancel()` — 항목 취소
- `requestReturn()` — 반품 요청

**OrderStatusHistory** (이력 엔티티)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| orderId | Long | 주문 ID | NOT NULL |
| fromStatus | OrderStatus? | 이전 상태 | nullable |
| toStatus | OrderStatus | 변경 상태 | NOT NULL |
| reason | String? | 사유 | max 500 |
| changedBy | String? | 변경자 | max 100 |

**Cart** (AuditingEntityListener)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| memberId | Long | 회원 ID | NOT NULL |

**CartItem** (AuditingEntityListener)
| 필드 | 타입 | 설명 | 제약 |
|------|------|------|------|
| cartId | Long | 장바구니 ID | NOT NULL |
| productId | Long | 상품 ID | NOT NULL |
| productOptionId | Long | 상품 옵션 ID | NOT NULL |
| quantity | Int | 수량 | NOT NULL |
| unitPrice | Money | 단가 | NOT NULL, DECIMAL(15,2) |

비즈니스 메서드:
- `create(...)` — 팩토리 메서드 (수량 1 이상 검증)
- `updateQuantity(quantity)` — 수량 변경 (1 이상 검증)

#### Enum

**OrderStatus**: `PENDING`, `STOCK_RESERVED`, `PAID`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CONFIRMED`, `CANCELLED`, `FAILED`

상태 전이 규칙:
```
PENDING -------> STOCK_RESERVED -------> PAID -------> PREPARING -------> SHIPPED -------> DELIVERED -------> CONFIRMED
  |                    |                   |                |
  +---> CANCELLED <----+----> CANCELLED <--+--> CANCELLED <-+
  |
  +---> FAILED
```
- 터미널 상태: CONFIRMED, CANCELLED, FAILED

**OrderItemStatus**: `ORDERED`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`

#### 도메인 이벤트

- `OrderCreatedEvent(orderId, memberId, items[productOptionId, quantity])`
- `OrderPaidEvent(orderId, paymentAmount)`
- `OrderCancelledEvent(orderId, reason, items[productOptionId, quantity])`

### 2.4 Payment Service

**현재 상태**: 스켈레톤 (Application 클래스만 존재)
- `ClosetPaymentApplication` — `@SpringBootApplication(scanBasePackages = ["com.closet.payment", "com.closet.common"])`
- Virtual Threads 활성화 (`spring.threads.virtual.enabled=true`)
- Flyway 설정 완료 (테이블명: `flyway_schema_history_payment`)
- 포트: 8084
- DB 마이그레이션 SQL 파일 없음 (DDL-AUTO: update 사용 중)

BFF PaymentServiceClient를 통해 예상되는 API:
- `GET /payments/orders/{orderId}` — 주문별 결제 조회
- `POST /payments/confirm` — 결제 승인
- `POST /payments/{id}/cancel` — 결제 취소

### 2.5 Gateway

**Spring Cloud Gateway (WebFlux 기반)**

#### 라우팅 (application.yml)
| Route ID | URI | Predicates |
|----------|-----|------------|
| member-service | http://localhost:8081 | `/api/v1/members/**` |
| product-service | http://localhost:8082 | `/api/v1/products/**`, `/api/v1/categories/**`, `/api/v1/brands/**` |
| order-service | http://localhost:8083 | `/api/v1/orders/**`, `/api/v1/carts/**` |
| payment-service | http://localhost:8084 | `/api/v1/payments/**` |
| bff-service | http://localhost:8085 | `/api/v1/bff/**` |

#### 필터

**JwtAuthenticationFilter** (GlobalFilter, Order: -1)
- JWT 검증 후 `X-Member-Id` 헤더를 하위 서비스로 전파
- jjwt 라이브러리 사용 (HMAC-SHA)
- Public Paths (인증 불필요):
  - `POST /api/v1/members/register`
  - `POST /api/v1/members/login`
  - `POST /api/v1/members/auth/refresh`
  - `GET /api/v1/products/**`
  - `GET /api/v1/categories/**`
  - `GET /api/v1/brands/**`
  - `GET /api/v1/bff/products/**`
  - `POST /api/v1/bff/auth/**`

**RequestLoggingFilter** (GlobalFilter, Order: -2)
- 요청/응답 로깅 (method, path, route, 소요시간)

#### 설정

**CorsConfig**
- 허용 Origin: `http://localhost:3000` (Web), `http://localhost:19006` (Mobile/Expo)
- 허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Credentials: true
- maxAge: 3600초

**RateLimiterConfig**
- IP 기반 KeyResolver

### 2.6 BFF (Backend for Frontend)

**OpenFeign + Virtual Threads 기반 서비스 오케스트레이션**

#### Feign Clients
| Client | Base URL | 주요 엔드포인트 |
|--------|----------|----------------|
| ProductServiceClient | `${service.product.url}` | GET /products, /categories, /brands |
| MemberServiceClient | `${service.member.url}` | GET/POST /members, /members/{id}/addresses |
| OrderServiceClient | `${service.order.url}` | GET/POST /orders, /carts |
| PaymentServiceClient | `${service.payment.url}` | GET/POST /payments |

#### Facade 패턴 (오케스트레이션)

**ProductBffFacade**
- `getProductDetail(productId)` — 상품 상세 + 관련 상품 (Virtual Threads 병렬 호출)

**OrderBffFacade**
- `getOrderDetail(orderId)` — 주문 + 결제 정보 (병렬)
- `getCheckout(memberId)` — 장바구니 + 배송지 목록 (병렬)
- `placeOrder(memberId, request)` — 주문 생성
- `confirmPayment(request)` — 결제 승인 후 주문 조회
- `cancelOrder(orderId, reason)` — 주문 취소 + 결제 정보 조회

**MyPageBffFacade**
- `getMyPage(memberId)` — 회원 + 최근 주문(5건) + 배송지 (3개 병렬 호출)

**HomeBffFacade**
- `getHome()` — 랭킹 상품(10개) + 신상품(10개) (병렬)

#### FeignConfig
- ErrorDecoder: 404 -> ENTITY_NOT_FOUND, 409 -> DUPLICATE_ENTITY

#### BFF Response DTOs (집계 응답)
- `ProductDetailBffResponse` — 상품 + 리뷰 요약(Phase 2) + 관련 상품
- `OrderDetailBffResponse` — 주문 + 결제 + 배송(Phase 2)
- `MyPageBffResponse` — 회원 + 최근 주문 + 배송지
- `CheckoutBffResponse` — 장바구니 + 배송지 + 쿠폰(Phase 3)
- `HomeBffResponse` — 배너(Phase 3) + 랭킹 + 신상품 + 기획전(Phase 3)

## 3. 공통 라이브러리 (closet-common)

### BaseEntity (`@MappedSuperclass`)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, AUTO_INCREMENT |
| createdAt | LocalDateTime | 생성일시, DATETIME(6) |
| updatedAt | LocalDateTime | 수정일시, DATETIME(6) |
| deletedAt | LocalDateTime? | 삭제일시, DATETIME(6), Soft Delete |

메서드: `softDelete()`, `isDeleted()`

### Money (Value Object, `@Embeddable`)
- `amount: BigDecimal` (0 이상 검증)
- 연산: `+`, `-`, `*`, 비교(`Comparable<Money>`)
- 팩토리: `Money.ZERO`, `Money.of(Long)`, `Money.of(String)`

### ApiResponse
```kotlin
data class ApiResponse<T>(success: Boolean, data: T?, error: ErrorResponse?)
```
- `ApiResponse.ok(data)` / `ApiResponse.created(data)` / `ApiResponse.fail(error)`

### ErrorCode
| Code | 코드 | 메시지 | HTTP Status |
|------|------|--------|-------------|
| INVALID_INPUT | C001 | 잘못된 입력값입니다 | 400 |
| ENTITY_NOT_FOUND | C002 | 엔티티를 찾을 수 없습니다 | 404 |
| INTERNAL_SERVER_ERROR | C003 | 서버 오류가 발생했습니다 | 500 |
| UNAUTHORIZED | C004 | 인증이 필요합니다 | 401 |
| FORBIDDEN | C005 | 접근 권한이 없습니다 | 403 |
| DUPLICATE_ENTITY | C006 | 이미 존재하는 엔티티입니다 | 409 |
| INVALID_STATE_TRANSITION | C007 | 잘못된 상태 전이입니다 | 400 |

### BusinessException
- `open class BusinessException(errorCode: ErrorCode, message: String)`
- RuntimeException 상속

### GlobalExceptionHandler (`@RestControllerAdvice`)
- `BusinessException` -> errorCode.status + ApiResponse.fail
- `MethodArgumentNotValidException` -> 400 + 필드별 상세 메시지
- `IllegalArgumentException` -> 400
- `Exception` -> 500

### JpaAuditingConfig
- `@EnableJpaAuditing` 활성화

## 4. 기술 스택 적용 현황

| 기술 | 적용 현황 | 비고 |
|------|----------|------|
| Java 21 + Virtual Threads | Payment, BFF에서 `spring.threads.virtual.enabled=true` | BFF Facade에서 `Executors.newVirtualThreadPerTaskExecutor()` 직접 사용 |
| Spring Boot 3.x | 전 서비스 | WebFlux(Gateway), WebMVC(나머지) |
| Spring Cloud Gateway | closet-gateway | WebFlux 기반 |
| JPA/Hibernate | Member, Product, Order | BaseEntity + AuditingEntityListener |
| QueryDSL | closet-product | `ProductRepositoryCustom` 인터페이스 정의 (동적 필터링) |
| Flyway | Member, Product, Order | 서비스별 독립 마이그레이션 (`flyway_schema_history_{service}`) |
| OpenFeign | closet-bff | 4개 서비스 클라이언트 |
| JWT (jjwt) | Gateway, Member | HMAC-SHA, `X-Member-Id` 전파 |
| BCrypt | Member | 비밀번호 해싱 |
| Kotest (BehaviorSpec) | Order | `OrderStatusTest` (상태 전이 테스트) |
| Kafka | 미적용 | 도메인 이벤트 정의만 완료 (OrderEvent) |
| Redis | 미적용 | Phase 2 |
| Elasticsearch | 미적용 | Phase 2 |

## 5. 설계 원칙 준수 현황

| 원칙 | 준수 | 세부 |
|------|------|------|
| 엔티티에 비즈니스 로직 캡슐화 | O | Member, Product, Order, CartItem 등 |
| enum 상태 전이 규칙 캡슐화 | O | `canTransitionTo()`, `validateTransitionTo()` 패턴 |
| Controller -> Service (SRP) | O | BFF는 Controller -> Facade -> Client |
| FK 미사용 | O | ID 참조만 사용 (memberId, productId 등) |
| ENUM 컬럼 미사용 | O | `VARCHAR(30)` + `@Enumerated(EnumType.STRING)` |
| TINYINT(1) for boolean | O | ShippingAddress.isDefault, Brand.status, Category.status |
| DATETIME(6) | O | 전 엔티티 createdAt/updatedAt/deletedAt |
| COMMENT 필수 | O | Flyway DDL에 전 컬럼 COMMENT 기재 |
| Soft Delete | O | BaseEntity.deletedAt |
| Value Object (Money) | O | @Embeddable, 0 이상 검증, 연산자 오버로딩 |
