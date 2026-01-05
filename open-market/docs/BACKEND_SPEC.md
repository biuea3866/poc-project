# 🔧 백엔드 요구사항 (BACKEND_SPEC.md)

> **담당**: Human (조봉준)
> **기술 스택**: Kotlin + Spring Boot 3.2+

---

## 프로젝트 구조

### 멀티모듈 구조

```
backend/
├── build.gradle.kts              # 루트 빌드 스크립트
├── settings.gradle.kts
│
├── api/                          # API 모듈
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/openmarket/api/
│           ├── member/           # 회원 API
│           ├── product/          # 상품 API
│           ├── order/            # 주문 API
│           ├── payment/          # 결제 API
│           ├── seller/           # 판매자 API
│           └── common/           # 공통 (응답, 예외처리)
│
├── domain/                       # 도메인 모듈
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/openmarket/domain/
│           ├── member/
│           ├── product/
│           ├── order/
│           ├── payment/
│           ├── delivery/
│           └── settlement/
│
├── infra/                        # 인프라 모듈
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/openmarket/infra/
│           ├── config/           # 설정
│           ├── redis/            # Redis 연동
│           ├── kafka/            # Kafka 연동
│           ├── elasticsearch/    # ES 연동
│           ├── pg/               # PG사 연동
│           └── channel/          # 외부채널 연동
│
└── batch/                        # 배치 모듈
    ├── build.gradle.kts
    └── src/main/kotlin/
        └── com/openmarket/batch/
            ├── settlement/       # 정산 배치
            └── sync/             # 동기화 배치
```

---

## 도메인별 상세 스펙

### 1. Member (회원) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "members")
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val email: String,
    
    @Column(nullable = false)
    var password: String,
    
    @Column(nullable = false)
    var name: String,
    
    @Column
    var phone: String? = null,
    
    @Enumerated(EnumType.STRING)
    var role: MemberRole = MemberRole.BUYER,
    
    @Enumerated(EnumType.STRING)
    var status: MemberStatus = MemberStatus.ACTIVE,
    
    @CreatedDate
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class MemberRole { BUYER, SELLER, ADMIN }
enum class MemberStatus { ACTIVE, INACTIVE, SUSPENDED }
```

#### API Endpoints
| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/members/signup | 회원가입 |
| POST | /api/v1/members/login | 로그인 |
| POST | /api/v1/members/refresh | 토큰 갱신 |
| GET | /api/v1/members/me | 내 정보 조회 |
| PUT | /api/v1/members/me | 내 정보 수정 |
| POST | /api/v1/members/seller/apply | 판매자 전환 신청 |

---

### 2. Product (상품) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "products")
class Product(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: Member,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category,
    
    @Column(nullable = false)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String,
    
    @Column(nullable = false)
    var price: BigDecimal,
    
    @Enumerated(EnumType.STRING)
    var status: ProductStatus = ProductStatus.DRAFT,
    
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL])
    val options: MutableList<ProductOption> = mutableListOf(),
    
    @OneToMany(mappedBy = "product", cascade = [CascadeType.ALL])
    val images: MutableList<ProductImage> = mutableListOf()
)

@Entity
@Table(name = "product_options")
class ProductOption(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,
    
    @Column(nullable = false)
    var name: String,  // ex: "색상: 빨강 / 사이즈: L"
    
    @Column(nullable = false)
    var additionalPrice: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false)
    var stock: Int = 0
)

enum class ProductStatus { DRAFT, ON_SALE, SOLD_OUT, HIDDEN, DELETED }
```

#### API Endpoints
| Method | Path | 설명 |
|--------|------|------|
| GET | /api/v1/products | 상품 목록 (검색, 필터, 페이징) |
| GET | /api/v1/products/{id} | 상품 상세 |
| POST | /api/v1/seller/products | 상품 등록 |
| PUT | /api/v1/seller/products/{id} | 상품 수정 |
| DELETE | /api/v1/seller/products/{id} | 상품 삭제 |
| POST | /api/v1/seller/products/{id}/images | 이미지 업로드 |

#### 재고 차감 (동시성 제어)
```kotlin
@Service
class StockService(
    private val redissonClient: RedissonClient,
    private val productOptionRepository: ProductOptionRepository
) {
    fun decreaseStock(optionId: Long, quantity: Int) {
        val lock = redissonClient.getLock("stock:$optionId")
        try {
            if (lock.tryLock(5, 3, TimeUnit.SECONDS)) {
                val option = productOptionRepository.findById(optionId)
                    .orElseThrow { ProductNotFoundException() }
                
                if (option.stock < quantity) {
                    throw InsufficientStockException()
                }
                
                option.stock -= quantity
                productOptionRepository.save(option)
            }
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
```

---

### 3. Order (주문) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Id
    val id: String = UUID.randomUUID().toString(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    val buyer: Member,
    
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: MutableList<OrderItem> = mutableListOf(),
    
    @Embedded
    var shippingAddress: ShippingAddress,
    
    @Column(nullable = false)
    var totalAmount: BigDecimal,
    
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING,
    
    @CreatedDate
    val orderedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    val option: ProductOption,
    
    @Column(nullable = false)
    val quantity: Int,
    
    @Column(nullable = false)
    val price: BigDecimal,
    
    @Enumerated(EnumType.STRING)
    var itemStatus: OrderItemStatus = OrderItemStatus.ORDERED
)

enum class OrderStatus {
    PENDING,        // 주문 생성
    PAID,           // 결제 완료
    PREPARING,      // 상품 준비중
    SHIPPED,        // 배송중
    DELIVERED,      // 배송 완료
    CANCELLED,      // 취소
    REFUNDED        // 환불
}
```

#### 주문 상태 머신
```
PENDING ──(결제)──> PAID ──(발송)──> PREPARING ──(배송시작)──> SHIPPED ──(배송완료)──> DELIVERED
    │                │                                                                    │
    │                │                                                                    │
    └──(취소)──> CANCELLED <───────────────────(환불)─────────────────────────────> REFUNDED
```

#### API Endpoints
| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/orders | 주문 생성 |
| GET | /api/v1/orders | 내 주문 목록 |
| GET | /api/v1/orders/{id} | 주문 상세 |
| POST | /api/v1/orders/{id}/cancel | 주문 취소 |
| GET | /api/v1/seller/orders | 판매자 주문 목록 |
| PUT | /api/v1/seller/orders/{id}/ship | 발송 처리 |

---

### 4. Payment (결제) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "payments")
class Payment(
    @Id
    val id: String = UUID.randomUUID().toString(),
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order,
    
    @Enumerated(EnumType.STRING)
    val pgProvider: PgProvider,
    
    @Column
    var pgPaymentKey: String? = null,  // PG사 결제키
    
    @Column(nullable = false)
    val amount: BigDecimal,
    
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,
    
    @Column
    var paidAt: LocalDateTime? = null,
    
    @Column
    var cancelledAt: LocalDateTime? = null,
    
    @Column
    var failReason: String? = null
)

enum class PgProvider {
    TOSS_PAYMENTS,
    KAKAO_PAY,
    NAVER_PAY,
    DANAL
}

enum class PaymentStatus {
    PENDING,
    PAID,
    CANCELLED,
    FAILED,
    PARTIAL_CANCELLED
}
```

#### API Endpoints
| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/payments/ready | 결제 준비 (PG 연동) |
| POST | /api/v1/payments/confirm | 결제 승인 |
| POST | /api/v1/payments/{id}/cancel | 결제 취소 |
| GET | /api/v1/payments/{id} | 결제 상세 |

---

### 5. Channel (외부 채널 연동) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "channel_products")
class ChannelProduct(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,
    
    @Enumerated(EnumType.STRING)
    val channel: SalesChannel,
    
    @Column
    var channelProductId: String? = null,  // 채널 상품 ID
    
    @Enumerated(EnumType.STRING)
    var syncStatus: SyncStatus = SyncStatus.PENDING,
    
    @Column
    var lastSyncedAt: LocalDateTime? = null
)

enum class SalesChannel {
    ST11,           // 11번가
    NAVER_STORE,    // 네이버 스마트스토어
    KAKAO_STORE,    // 카카오 스토어
    TOSS_STORE,     // 토스 스토어
    COUPANG         // 쿠팡
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
```

#### 어댑터 패턴 (Port & Adapter)
```kotlin
// Port (Interface)
interface ChannelAdapter {
    fun registerProduct(product: Product): ChannelProductResult
    fun updateProduct(product: Product, channelProductId: String): ChannelProductResult
    fun syncOrder(channelOrderId: String): Order
    fun updateOrderStatus(order: Order): Boolean
}

// Adapter 구현
@Component
class NaverStoreAdapter(
    private val naverStoreClient: NaverStoreClient
) : ChannelAdapter {
    override fun registerProduct(product: Product): ChannelProductResult {
        // 네이버 스마트스토어 API 호출
    }
    // ...
}
```

---

### 6. Settlement (정산) 도메인

#### Entity
```kotlin
@Entity
@Table(name = "settlements")
class Settlement(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    val seller: Member,
    
    @Column(nullable = false)
    val settlementDate: LocalDate,  // 정산 기준일
    
    @Column(nullable = false)
    val salesAmount: BigDecimal,    // 매출액
    
    @Column(nullable = false)
    val feeAmount: BigDecimal,      // 수수료
    
    @Column(nullable = false)
    val settlementAmount: BigDecimal, // 정산 금액
    
    @Enumerated(EnumType.STRING)
    var status: SettlementStatus = SettlementStatus.PENDING
)

enum class SettlementStatus {
    PENDING,    // 정산 대기
    CONFIRMED,  // 정산 확정
    PAID        // 지급 완료
}
```

#### Spring Batch 정산 Job
```kotlin
@Configuration
class SettlementJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager
) {
    @Bean
    fun settlementJob(): Job {
        return JobBuilder("settlementJob", jobRepository)
            .start(calculateSettlementStep())
            .next(confirmSettlementStep())
            .build()
    }
    
    @Bean
    fun calculateSettlementStep(): Step {
        return StepBuilder("calculateSettlementStep", jobRepository)
            .chunk<OrderItem, Settlement>(100, transactionManager)
            .reader(orderItemReader())
            .processor(settlementProcessor())
            .writer(settlementWriter())
            .build()
    }
}
```

---

## 이벤트 기반 아키텍처

### Kafka 토픽 구조
```
open-market.order.created     # 주문 생성
open-market.order.paid        # 결제 완료
open-market.order.cancelled   # 주문 취소
open-market.product.created   # 상품 생성
open-market.product.updated   # 상품 수정
open-market.stock.decreased   # 재고 차감
```

### 이벤트 흐름 예시
```
[주문 생성]
    │
    ├──> order.created ──> 재고 차감
    │
    └──> payment.ready

[결제 완료]
    │
    ├──> order.paid ──> 판매자 알림
    │
    └──> settlement 대기 등록
```

---

## API 공통 스펙

### 응답 형식
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse?,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Any? = null
)
```

### 에러 코드 체계
| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| MEMBER_NOT_FOUND | 404 | 회원 없음 |
| PRODUCT_NOT_FOUND | 404 | 상품 없음 |
| ORDER_NOT_FOUND | 404 | 주문 없음 |
| INSUFFICIENT_STOCK | 400 | 재고 부족 |
| INVALID_ORDER_STATUS | 400 | 잘못된 주문 상태 |
| PAYMENT_FAILED | 400 | 결제 실패 |
| UNAUTHORIZED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 권한 없음 |

### 페이징 응답
```kotlin
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
```

---

## 보안

### JWT 구조
```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}
Payload: {
  "sub": "member_id",
  "role": "BUYER|SELLER|ADMIN",
  "iat": 1234567890,
  "exp": 1234567890
}
```

### 토큰 정책
- Access Token: 1시간
- Refresh Token: 14일 (Redis 저장)

---

## 테스트 전략

### 테스트 종류
- Unit Test: JUnit 5, Mockk
- Integration Test: @SpringBootTest, Testcontainers
- API Test: MockMvc, REST Assured

### 테스트 커버리지 목표
- Service Layer: 80%+
- Repository Layer: 70%+
- Controller Layer: API 문서화 겸용
