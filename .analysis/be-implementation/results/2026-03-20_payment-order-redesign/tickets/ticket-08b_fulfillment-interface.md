# [Ticket #8b] FulfillmentStrategy 인터페이스 + Resolver

## 개요
- TDD 참조: tdd.md 섹션 3.5, 4.2
- 선행 티켓: #8a (Order 엔티티 모델)
- 크기: S
- 원본: ticket-08_order-domain.md에서 분리

## 배경

FulfillmentStrategy 인터페이스와 FulfillmentStrategyResolver를 정의한다. **구현체(Subscription, Credit, OneTime)는 #12a, #12b, #12c에서 작성**한다.

- `OrderFacade.processOrder()` → `FulfillmentStrategyResolver.resolve(productType)` → `fulfill(order)`
- `SubscriptionService`, `CreditService`는 **존재하지 않는다** -- Fulfillment 패턴으로 통합

---

## 작업 내용

### FulfillmentStrategy 인터페이스 (구현체는 #12a, #12b, #12c)

```kotlin
package com.greeting.payment.domain.order.fulfillment

import com.greeting.payment.domain.order.Order

/**
 * 상품 유형별 주문 이행 전략.
 *
 * - SubscriptionFulfillment: 구독 생성/갱신 (#12a)
 * - CreditFulfillment: 크레딧 충전 (#12b)
 * - OneTimeFulfillment: 즉시 완료 (#12c)
 *
 * OrderFacade.processOrder()에서 order.resolveProductType()으로 전략을 선택하여 호출한다.
 */
interface FulfillmentStrategy {

    fun fulfill(order: Order)

    fun revoke(order: Order)
}

class FulfillmentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

### FulfillmentStrategyResolver

```kotlin
package com.greeting.payment.domain.order.fulfillment

import com.greeting.payment.domain.product.ProductType
import org.springframework.stereotype.Component

@Component
class FulfillmentStrategyResolver(
    private val subscriptionFulfillment: SubscriptionFulfillment,
    private val creditFulfillment: CreditFulfillment,
    private val oneTimeFulfillment: OneTimeFulfillment,
) {

    fun resolve(productType: ProductType): FulfillmentStrategy {
        return when (productType) {
            ProductType.SUBSCRIPTION -> subscriptionFulfillment
            ProductType.CONSUMABLE -> creditFulfillment
            ProductType.ONE_TIME -> oneTimeFulfillment
        }
    }
}
```

### 수정 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `domain/order/fulfillment/FulfillmentStrategy.kt` | 신규 | 인터페이스 정의: `fulfill(order)`, `revoke(order)` |
| `domain/order/fulfillment/FulfillmentException.kt` | 신규 | 이행 실패 예외 |
| `domain/order/fulfillment/FulfillmentStrategyResolver.kt` | 신규 | ProductType -> Strategy 매핑 |

---

## 테스트 케이스

### 정상 케이스

| # | 테스트 | 입력 | 기대 결과 |
|---|--------|------|----------|
| 1 | `FulfillmentStrategyResolver.resolve` - SUBSCRIPTION | ProductType.SUBSCRIPTION | SubscriptionFulfillment 반환 |
| 2 | `FulfillmentStrategyResolver.resolve` - CONSUMABLE | ProductType.CONSUMABLE | CreditFulfillment 반환 |
| 3 | `FulfillmentStrategyResolver.resolve` - ONE_TIME | ProductType.ONE_TIME | OneTimeFulfillment 반환 |

---

## 기대 결과 (AC)

- [ ] `FulfillmentStrategy` 인터페이스가 `fulfill(order)`, `revoke(order)` 시그니처로 정의
- [ ] `FulfillmentStrategyResolver`가 `ProductType`으로 올바른 전략 선택
- [ ] `FulfillmentException`이 커스텀 예외로 정의
- [ ] 단위 테스트: 정상 3건 통과
