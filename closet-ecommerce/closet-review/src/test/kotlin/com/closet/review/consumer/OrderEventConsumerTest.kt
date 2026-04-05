package com.closet.review.consumer

import com.closet.review.application.ReviewService
import com.closet.review.consumer.event.OrderEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify

/**
 * OrderEventConsumer 테스트.
 *
 * event.closet.order 토픽에서 OrderItemConfirmed 이벤트를 수신했을 때,
 * Consumer는 이벤트 라우팅만 담당하고 비즈니스 로직은 Service에 위임한다.
 */
class OrderEventConsumerTest : BehaviorSpec({

    val reviewService = mockk<ReviewService>(relaxed = true)
    val consumer = OrderEventConsumer(reviewService)

    beforeTest {
        clearMocks(reviewService)
    }

    Given("주문 아이템 구매확정 이벤트 수신") {

        When("OrderItemConfirmed eventType을 수신하면") {
            val event = OrderEvent(
                eventType = "OrderItemConfirmed",
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                productId = 200L,
            )

            Then("ReviewService에 구매확정 정보를 전달한다") {
                consumer.handle(event)
                verify {
                    reviewService.onOrderItemConfirmed(
                        orderItemId = 10L,
                        memberId = 100L,
                        productId = 200L,
                    )
                }
            }
        }

        When("처리하지 않는 eventType을 수신하면") {
            val event = OrderEvent(
                eventType = "OrderCreated",
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                productId = 200L,
            )

            Then("무시한다 (Service 호출 없음)") {
                consumer.handle(event)
                verify(exactly = 0) {
                    reviewService.onOrderItemConfirmed(any(), any(), any())
                }
            }
        }
    }
})
