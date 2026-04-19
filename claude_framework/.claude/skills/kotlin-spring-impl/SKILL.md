---
name: kotlin-spring-impl
description: Kotlin/Spring Boot 백엔드 구현 스킬. Kotlin 문법 적극 활용, 디자인 패턴 기반, OOP 책임 분리, Rich Domain Model + 얇은 Service, 풀네임 변수, Enum 상태 전이 규칙까지 강제. be-implementer 에이전트가 TDD 구현 시 참조.
---

# Kotlin/Spring Implementation Skill

## 언제 사용하나
- Kotlin + Spring Boot BE 티켓 구현
- Entity/UseCase/Controller/Repository 작성
- 리팩토링 시 구조 개선
- 코드 리뷰 기준

## 6대 원칙 (필수)

1. **Kotlin 문법 적극 활용** — Java스러운 코드 금지
2. **디자인 패턴 적극 활용** — 분기 3개 이상 시 Strategy/State/Factory/Template Method 검토
3. **OOP 책임 분리** — 객체가 자기 행위를 캡슐화. 다른 객체의 필드를 꺼내 비교하지 않는다.
4. **Rich Domain Model** — 비즈니스 로직은 Entity에. Service는 오케스트레이션만.
5. **변수명 풀네임** — `workspaceId`, `userId`, `applicationId` (약어 금지)
6. **Enum 상태 전이 규칙** — 상태 enum 내부에 `canTransitTo`, `validateTransitTo` 캡슐화

---

## 1. Kotlin 문법 적극 활용

### 필수 사용
- `data class` — DTO/VO
- `sealed class` / `sealed interface` — 상태 계층, 이벤트 계층
- `value class` (`@JvmInline`) — 원시 타입 래핑 (`UserId`, `Money`)
- `object` — 싱글턴 (상수 모음, 팩토리)
- `companion object` — 정적 멤버, 팩토리 메서드
- `extension function` — 외부 타입에 도메인 의미 부여
- `scope function` (`let`, `also`, `apply`, `run`, `with`) — 적재적소 사용
- `when` 표현식 — sealed + 컴파일러가 exhaustive 검사
- `?:`, `?.`, `?.let` — Null 안전 (`!!` 절대 금지)
- `requireNotNull`, `checkNotNull`, `check`, `require` — 사전 조건

### 예시: Java스러운 코드 vs Kotlin다운 코드

```kotlin
// ❌ BAD — Java 스타일
fun findUser(id: Long): User {
    val user = userRepository.findById(id)
    if (user == null) {
        throw UserNotFoundException()
    }
    return user
}

// ✅ GOOD — Kotlin 스타일
fun findUser(userId: Long): User =
    userRepository.findById(userId) ?: throw UserNotFoundException(userId)
```

```kotlin
// ❌ BAD — getter/setter 나열
class Rental {
    private var status: RentalStatus = RentalStatus.REQUESTED
    fun getStatus() = status
    fun setStatus(newStatus: RentalStatus) { status = newStatus }
}

// ✅ GOOD — data class + val (immutability)
data class Rental(
    val id: Long,
    val renterId: Long,
    val productId: Long,
    val status: RentalStatus,
    val requestedAt: ZonedDateTime,
)
```

### Value Class 활용

```kotlin
// ❌ BAD — 원시 Long 남발
fun requestRental(renterId: Long, productId: Long, ownerId: Long)
// 실수로 순서 바꿔도 컴파일 통과

// ✅ GOOD — value class로 타입 안전
@JvmInline value class UserId(val value: Long)
@JvmInline value class ProductId(val value: Long)

fun requestRental(renterId: UserId, productId: ProductId, ownerId: UserId)
// 순서 바꾸면 컴파일 실패
```

### 함수형 프로그래밍 적극 활용

Kotlin은 다중 패러다임이지만, **컬렉션/에러/흐름 처리**는 함수형이 압도적으로 좋다.

#### 컬렉션 고차 함수 (for 루프 지양)

```kotlin
// ❌ BAD — 명령형 for 루프
val approvedRentals = mutableListOf<Rental>()
for (rental in rentals) {
    if (rental.status == RentalStatus.APPROVED) {
        approvedRentals.add(rental)
    }
}
val totalAmount = 0L
for (rental in approvedRentals) {
    totalAmount += rental.amount.value
}

// ✅ GOOD — 함수형 파이프라인
val totalAmount = rentals
    .filter { it.isApproved() }
    .sumOf { it.amount.value }
```

#### 필수 고차 함수

| 함수 | 용도 |
|---|---|
| `map` / `mapNotNull` | 변환 (null 제거 포함) |
| `filter` / `filterNot` / `filterIsInstance<T>` | 조건 선택 |
| `flatMap` | 평탄화 |
| `fold` / `reduce` | 집계 |
| `groupBy` / `associate` / `associateBy` | 키-값 매핑 |
| `partition` | true/false 분할 |
| `zip` / `zipWithNext` | 결합 |
| `chunked` / `windowed` | 분할 윈도우 |
| `distinct` / `distinctBy` | 중복 제거 |
| `sortedBy` / `sortedWith` | 정렬 |
| `count` / `sumOf` / `maxOf` / `minOf` | 집계 |
| `any` / `all` / `none` | 술어 |
| `take` / `drop` / `takeWhile` / `dropWhile` | 부분 추출 |

#### Sequence (지연 평가, 대량 데이터)

```kotlin
// ❌ BAD — 중간 컬렉션이 매 연산마다 생성됨 (10만건에 여러 번 allocation)
val result: List<String> = hugeList
    .map { heavyTransform(it) }      // 10만건 List 생성
    .filter { it.isValid() }          // 또 10만건 List 생성
    .take(100)                         // 100건 List 생성

// ✅ GOOD — 지연 평가, 필요한 100건만 처리
val result: List<String> = hugeList
    .asSequence()
    .map { heavyTransform(it) }
    .filter { it.isValid() }
    .take(100)
    .toList()
```

**규칙**: 요소 1000개 이상 + 체인 2단계 이상 → `asSequence()` 검토.

#### 불변 컬렉션 선호

```kotlin
// ❌ BAD — MutableList 남발
fun buildReport(): MutableList<String> {
    val result = mutableListOf<String>()
    result.add("header")
    result.addAll(rows.map { it.format() })
    return result
}

// ✅ GOOD — 불변 List + 파이프라인
fun buildReport(): List<String> =
    listOf("header") + rows.map { it.format() }
```

#### 순수 함수 (side-effect 격리)

```kotlin
// ❌ BAD — 함수 내부에서 mutation + IO
fun processRentals(rentals: List<Rental>): Int {
    var count = 0
    rentals.forEach {
        if (it.isOverdue()) {
            logger.info("overdue: ${it.id}")  // side effect
            rentalRepository.save(it.markOverdue())  // side effect
            count++  // mutation
        }
    }
    return count
}

// ✅ GOOD — 변환은 순수, side effect는 경계에 격리
fun findOverdueRentals(rentals: List<Rental>): List<Rental> =
    rentals.filter { it.isOverdue() }

fun markAllOverdue(rentals: List<Rental>): List<Rental> =
    rentals.map { it.markOverdue() }

// 외부에서 순수 함수 + side effect 조합
val overdue = findOverdueRentals(rentals)
overdue.forEach { logger.info("overdue: ${it.id}") }
rentalRepository.saveAll(markAllOverdue(overdue))
```

#### Result / Either 패턴으로 에러 흐름

```kotlin
// 예외 throw 대신 Result 모나드 (재귀 호출 시 스택 부담 ↓, 합성 용이)
fun parseAmount(raw: String): Result<Money> = runCatching {
    Money(raw.toLong().also { require(it > 0) { "음수 금액 불가" } })
}

fun chargeRental(rentalId: Long, rawAmount: String): Result<RentalPayment> =
    parseAmount(rawAmount)
        .mapCatching { amount -> rentalDomainService.charge(rentalId, amount) }
        .onFailure { logger.error("charge failed: $rentalId", it) }
```

또는 **arrow-kt `Either`** 도입 시 (프로젝트 규모 크면 권장):
```kotlin
fun approve(command: ApproveCommand): Either<DomainError, Rental> =
    either {
        val rental = rentalDomainService.getRental(command.rentalId).bind()
        ensure(rental.canBeApproved()) { AlreadyApprovedError(rental.id) }
        rental.approve(command.approverId).right().bind()
    }
```

#### 커링 / 부분 적용

```kotlin
// 공통 파라미터를 부분 고정해 재사용
fun filterByStatus(status: RentalStatus): (List<Rental>) -> List<Rental> =
    { rentals -> rentals.filter { it.status == status } }

val approvedOnly = filterByStatus(RentalStatus.APPROVED)
val approved = approvedOnly(rentals)
val overdue = approvedOnly(overdueRentals)
```

#### 표현식 중심 (문장 최소화)

```kotlin
// ❌ BAD — 문장 나열
fun getStatusLabel(status: RentalStatus): String {
    val label: String
    if (status == RentalStatus.APPROVED) {
        label = "승인됨"
    } else if (status == RentalStatus.REJECTED) {
        label = "거절됨"
    } else {
        label = "대기 중"
    }
    return label
}

// ✅ GOOD — when 표현식
fun getStatusLabel(status: RentalStatus): String = when (status) {
    RentalStatus.APPROVED -> "승인됨"
    RentalStatus.REJECTED -> "거절됨"
    else -> "대기 중"
}

// ✅ GOOD — 단일 표현식 함수 (`= expr` 문법)
fun getDiscountedAmount(base: Money, rate: Double): Money =
    Money((base.value * (1.0 - rate)).toLong())
```

#### 함수 조합 (compose)

```kotlin
// 작은 순수 함수를 합성해 큰 파이프라인 구성
val normalize: (String) -> String = { it.trim().lowercase() }
val validate: (String) -> String? = { it.takeIf { s -> s.matches(EMAIL_REGEX) } }
val parseEmail: (String) -> Email? = { raw ->
    validate(normalize(raw))?.let(::Email)
}
```

#### 함수형 원칙 체크리스트

- [ ] for 루프 0개 (컬렉션 조작은 고차 함수)
- [ ] `var` 최소화 (Entity 상태 제외)
- [ ] 대량 체인(2단계 이상 + 1000건 이상) → Sequence
- [ ] Mutation/IO는 경계(Adapter, UseCase 끝단)에만
- [ ] 표현식 함수(`= expr`) 우선, 단일 문장 함수는 `{ return ... }` 없이
- [ ] 에러 흐름은 Result/Either로 합성 가능하게

### Sealed Class로 결과 타입 표현

```kotlin
sealed class RentalResult {
    data class Success(val rental: Rental) : RentalResult()
    data class Failure(val reason: FailureReason) : RentalResult()
}

// when 표현식으로 exhaustive 처리
when (result) {
    is RentalResult.Success -> handleSuccess(result.rental)
    is RentalResult.Failure -> handleFailure(result.reason)
    // else 불필요 — 컴파일러가 검사
}
```

---

## 2. 디자인 패턴 적극 활용

### 패턴 선택 기준

| 상황 | 패턴 | 예시 |
|---|---|---|
| 동일 인터페이스의 여러 구현이 런타임 교체 | **Strategy** | PaymentGateway(Toss/Kakao/Mock), NotificationSender(Email/Push/SMS) |
| 객체 상태별 행위 분기 | **State** | RentalStatus별 허용 액션 |
| 객체 생성 로직이 복잡/조건별 타입 | **Factory** | DomainEvent 생성, Notification 생성 |
| 여러 클래스가 동일 골격 반복 | **Template Method** | Rental 상태 전이 UseCase (Approve/Reject/Start/Return/Cancel) |
| 한 이벤트에 여러 후속 처리 | **Observer** | RentalStatusChanged → 알림+Kafka+로그 |
| 체인 처리 | **Chain of Responsibility** | 요청 검증 체인 |
| 알고리즘 교체 가능 | **Strategy** | 할인 정책, 재고 할당 |

### Strategy 예시

```kotlin
// ❌ BAD — when 분기 나열
class PaymentService {
    fun pay(type: PaymentType, amount: Money) = when (type) {
        PaymentType.TOSS -> tossClient.pay(amount)
        PaymentType.KAKAO -> kakaoClient.pay(amount)
        PaymentType.MOCK -> mockClient.pay(amount)
    }
}

// ✅ GOOD — Strategy interface + 구현체 주입
interface PaymentGateway {
    val type: PaymentType
    fun pay(amount: Money): PaymentResult
}

@Component class TossPaymentGateway(...) : PaymentGateway { ... }
@Component class KakaoPaymentGateway(...) : PaymentGateway { ... }

@Component
class PaymentGatewayRouter(gateways: List<PaymentGateway>) {
    private val byType = gateways.associateBy { it.type }
    fun route(type: PaymentType): PaymentGateway =
        byType[type] ?: throw UnsupportedPaymentTypeException(type)
}
```

### Template Method 예시

```kotlin
// ✅ 여러 상태 전이 UseCase의 공통 골격을 추출
abstract class RentalStateTransitionUseCase<C : RentalCommand>(
    private val rentalDomainService: RentalDomainService,
) {
    @Transactional
    fun execute(command: C): RentalResult {
        val rental = rentalDomainService.getRental(command.rentalId)
        val transitioned = transition(rental, command)
        return RentalResult.of(transitioned)
    }

    protected abstract fun transition(rental: Rental, command: C): Rental
}

@Component
class ApproveRentalUseCase(service: RentalDomainService)
    : RentalStateTransitionUseCase<ApproveRentalCommand>(service) {
    override fun transition(rental: Rental, command: ApproveRentalCommand): Rental =
        rental.approve(command.approverId)
}
```

### 분기 3개 이상 감지 시 패턴 검토 강제

```
if-else / when 분기가 3개 이상 나오면 반드시 다음 중 하나 선택:
1. Strategy — 구현체 다형성
2. State — 상태 캡슐화
3. Map dispatch — `mapOf(...)` 룩업
```

---

## 3. OOP 책임 분리 (Tell, Don't Ask)

### 원칙
- **Tell, Don't Ask** — 객체의 필드를 꺼내 비교/판단하지 말고, 객체에게 메시지를 보내라
- 데이터와 행위는 함께 — getter/setter만 있는 클래스는 Anemic 안티패턴
- 자기 책임만 진다 — 한 클래스가 여러 책임을 지면 분리

### 안티패턴 (UseCase에서 Entity 필드 꺼내 비교)

```kotlin
// ❌ BAD — Entity 필드를 꺼내 UseCase에서 판단
class RequestRentalUseCase(...) {
    fun execute(command: RequestRentalCommand): Rental {
        val product = productService.get(command.productId)
        if (product.ownerId == command.renterId) {
            throw SelfRentalException()
        }
        if (product.status != ProductStatus.AVAILABLE) {
            throw NotAvailableException()
        }
        // ...
    }
}

// ✅ GOOD — Entity에게 질문
class RequestRentalUseCase(...) {
    fun execute(command: RequestRentalCommand): Rental =
        rentalDomainService.requestRental(command)
}

class Product(...) {
    fun validateRequestableBy(renterId: UserId) {
        if (isOwnedBy(renterId)) throw SelfRentalException(id, renterId)
        if (!status.canRent()) throw NotAvailableException(id, status)
    }

    fun isOwnedBy(userId: UserId): Boolean = ownerId == userId
}
```

### 책임 분리 체크리스트

- [ ] 한 클래스가 100줄 이하 (Entity는 Rich Domain이라 더 길 수 있음, 단 책임이 하나)
- [ ] 한 메서드가 15줄 이하 (DomainService), 10줄 이하 (UseCase)
- [ ] 한 메서드는 한 가지 추상화 수준
- [ ] "이 객체는 무엇을 아는가 / 무엇을 할 수 있는가"가 명확

---

## 4. Rich Domain Model + 얇은 Service

### 책임 분배

| 레이어 | 무엇을 하는가 |
|---|---|
| **Entity** | 비즈니스 로직, 상태 전이, 검증, 도메인 이벤트 발행 |
| **Value Object** | 불변 값 + 자체 검증 (`Money`, `Email`, `Period`) |
| **Domain Service** | Entity가 혼자 할 수 없는 로직 (여러 Entity 조합, 외부 Port 호출) |
| **UseCase (Application)** | 트랜잭션 경계 + DomainService 호출 오케스트레이션만 |
| **Controller (Presentation)** | HTTP 변환, UseCase 호출 |

### 좋은 Entity 예시

```kotlin
@Entity
@Table(name = "rental")
class Rental private constructor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val renterId: UserId,
    val productId: ProductId,
    val ownerId: UserId,
    @Enumerated(EnumType.STRING)
    var status: RentalStatus,
    val period: RentalPeriod,
    @CreatedDate
    val requestedAt: ZonedDateTime = ZonedDateTime.now(),
    var approvedAt: ZonedDateTime? = null,
    var returnedAt: ZonedDateTime? = null,
) : AbstractAggregateRoot<Rental>() {

    companion object {
        fun request(
            renterId: UserId,
            product: Product,
            period: RentalPeriod,
        ): Rental {
            product.validateRequestableBy(renterId)
            period.validateFuture()
            return Rental(
                renterId = renterId,
                productId = product.id,
                ownerId = product.ownerId,
                status = RentalStatus.REQUESTED,
                period = period,
            ).also {
                it.registerEvent(RentalRequestedEvent(it.id, it.renterId, it.productId))
            }
        }
    }

    fun approve(approverId: UserId) {
        validateApprover(approverId)
        status = status.transitTo(RentalStatus.APPROVED)
        approvedAt = ZonedDateTime.now()
        registerEvent(RentalApprovedEvent(id, approverId))
    }

    fun reject(approverId: UserId, reason: String) {
        validateApprover(approverId)
        status = status.transitTo(RentalStatus.REJECTED)
        registerEvent(RentalRejectedEvent(id, approverId, reason))
    }

    fun markReturned() {
        status = status.transitTo(RentalStatus.RETURNED)
        returnedAt = ZonedDateTime.now()
        registerEvent(RentalReturnedEvent(id))
    }

    private fun validateApprover(userId: UserId) {
        if (userId != ownerId) throw UnauthorizedApproverException(id, userId)
    }
}
```

### 얇은 Service 예시

```kotlin
// Application 레이어 — 오케스트레이션만
@Service
class RequestRentalUseCase(
    private val rentalDomainService: RentalDomainService,
) {
    @Transactional
    fun execute(command: RequestRentalCommand): RentalResult =
        RentalResult.of(rentalDomainService.request(command))
}

// Domain Service — Entity + Port 조합
@Service
class RentalDomainService(
    private val rentalRepository: RentalRepository,
    private val productDomainService: ProductDomainService,
    private val eventPublisher: DomainEventPublisher,
) {
    fun request(command: RequestRentalCommand): Rental {
        val product = productDomainService.getProduct(command.productId)
        val rental = Rental.request(command.renterId, product, command.period)
        return rentalRepository.save(rental).also {
            eventPublisher.publishAll(it.pullDomainEvents())
        }
    }
}
```

---

## 5. 변수명 풀네임

### 원칙
- **약어 금지** — 모든 변수/파라미터/필드는 의미를 알 수 있는 풀네임
- **1글자 변수 허용 범위**: 람다 인덱스(`i`), 이벤트(`e`) 정도만. 도메인 개념에는 금지.

### 금지 약어 → 풀네임 매핑

| 금지 | 풀네임 |
|---|---|
| `ws` | `workspaceId` |
| `usr` | `user`, `userId` |
| `prd` | `product`, `productId` |
| `comp` | `component`, `companyId` |
| `eval` | `evaluation` |
| `req` | `request` |
| `res` | `response` |
| `repo` | `repository` (단 의존성 주입 필드명은 관례적으로 허용) |
| `cfg` | `config`, `configuration` |
| `tx` | `transaction` (Spring `@Transactional`은 관례상 허용) |

### 예시

```kotlin
// ❌ BAD
fun find(id: Long, ws: Long): User {
    val u = userRepo.findByIdAndWs(id, ws)
    return u ?: throw Ex()
}

// ✅ GOOD
fun findUser(userId: Long, workspaceId: Long): User =
    userRepository.findByIdAndWorkspaceId(userId, workspaceId)
        ?: throw UserNotFoundException(userId, workspaceId)
```

### 파라미터명 네이밍 규칙
- PK: `{도메인명}Id` (예: `rentalId`, `productId`)
- 외래 참조: `{참조도메인}Id` (예: `renterId`, `ownerId`)
- 컬렉션: 복수형 (`rentals`, `products`)
- boolean: `is/has/can + 명사` (`isPublished`, `canRent`, `hasPermission`)

---

## 6. Enum 상태 전이 규칙

### 원칙
- 상태는 Enum으로 정의
- **전이 가능 여부는 Enum 내부에 캡슐화** (`canTransitTo`, `validateTransitTo`)
- Enum 밖에서 `if (status == X) status = Y` 금지
- UseCase/Service에서 `status.canTransitTo()` 호출 금지 (Entity 메서드가 전이 검증 수행)

### 구현 패턴

```kotlin
enum class RentalStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    IN_PROGRESS,
    RETURNED,
    CANCELLED;

    /**
     * 전이 허용 테이블. 각 상태가 자신에서 갈 수 있는 상태만 나열.
     */
    fun canTransitTo(target: RentalStatus): Boolean = target in allowedTransitions

    fun transitTo(target: RentalStatus): RentalStatus {
        if (!canTransitTo(target)) {
            throw IllegalStateTransitionException(this, target)
        }
        return target
    }

    private val allowedTransitions: Set<RentalStatus>
        get() = when (this) {
            REQUESTED   -> setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED    -> setOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> setOf(RETURNED)
            REJECTED, RETURNED, CANCELLED -> emptySet()  // 종결 상태
        }
}
```

### 사용 (Entity 내부에서만)

```kotlin
// ✅ GOOD — Entity 내부에서 transitTo 호출
class Rental(...) {
    fun approve(approverId: UserId) {
        validateApprover(approverId)
        status = status.transitTo(RentalStatus.APPROVED)  // ← 전이 검증 + 실행
        approvedAt = ZonedDateTime.now()
    }
}

// ❌ BAD — UseCase에서 상태 검사
class ApproveRentalUseCase(...) {
    fun execute(command: ApproveRentalCommand) {
        val rental = rentalService.get(command.rentalId)
        if (!rental.status.canTransitTo(RentalStatus.APPROVED)) {  // 하네스 차단
            throw IllegalStateException()
        }
        rental.status = RentalStatus.APPROVED  // Entity 외부에서 상태 변경 금지
    }
}
```

### 상태 + 행위 함께 캡슐화 (고급)

상태별 **허용 액션**이 많아지면 상태 자체가 행위를 가진 State 패턴으로 확장:

```kotlin
sealed interface RentalState {
    fun approve(rental: Rental, approverId: UserId): RentalState =
        throw IllegalStateTransitionException(rental.status, "APPROVE")
    fun cancel(rental: Rental, reason: String): RentalState =
        throw IllegalStateTransitionException(rental.status, "CANCEL")

    data object Requested : RentalState {
        override fun approve(rental: Rental, approverId: UserId) = Approved
        override fun cancel(rental: Rental, reason: String) = Cancelled
    }
    data object Approved : RentalState {
        override fun cancel(rental: Rental, reason: String) = Cancelled
    }
    data object Cancelled : RentalState
    // ...
}
```

---

## 체크리스트 (구현 완료 전)

### Kotlin 문법
- [ ] `!!` 0개 (하네스 차단)
- [ ] `data class`/`value class` 적절히 활용
- [ ] `sealed` 결과 타입 사용 (필요 시)
- [ ] `when` + exhaustive 처리

### 함수형 프로그래밍
- [ ] for 루프 0개 (고차 함수 사용)
- [ ] `var` 최소화 (Entity 상태 필드만 허용)
- [ ] 표현식 함수(`= expr`) 우선
- [ ] 대량 체인은 Sequence
- [ ] 순수 함수와 side-effect 분리

### 디자인 패턴
- [ ] `if-else/when` 분기 3개 이상 없음, 아니면 Strategy/State 적용
- [ ] 동일 구조 메서드 3개 이상 반복 없음 (Template Method)
- [ ] 한 이벤트 후속 처리 2개 이상은 Observer (`@TransactionalEventListener(AFTER_COMMIT)`)

### OOP
- [ ] UseCase/Service에서 Entity 필드 꺼내 비교하지 않음 (하네스 차단)
- [ ] Entity가 자기 행위 캡슐화 (Tell, Don't Ask)
- [ ] 한 메서드 한 추상화 수준

### Rich Domain Model
- [ ] Entity가 비즈니스 로직을 가짐 (Anemic 금지)
- [ ] UseCase `execute()` 10줄 이내
- [ ] DomainService 메서드 15줄 이내
- [ ] UseCase가 Repository/Gateway/Publisher 직접 참조 없음 (하네스 차단)

### 네이밍
- [ ] 모든 변수/파라미터/필드 풀네임
- [ ] boolean은 `is/has/can` 접두
- [ ] PK는 `id`, FK는 `~Id`

### Enum 상태 전이
- [ ] 상태 Enum에 `canTransitTo` + `transitTo` 메서드
- [ ] 전이 검증은 Enum 내부에서 수행
- [ ] Entity 외부에서 `status = X` 직접 대입 없음

---

## 참고 공통 가이드
- [be-code-convention](../../common/be-code-convention.md) — 레이어/네이밍/테스트 전반
- [tdd-template](../../common/tdd-template.md) — 설계 문서 섹션
- [output-style](../../common/output-style.md) — 코드 참조 형식