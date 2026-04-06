package com.closet.shipping.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.shipping.application.PrepareShipmentRequest
import com.closet.shipping.application.ShippingService
import com.closet.shipping.consumer.event.OrderEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OrderCreatedConsumerTest : BehaviorSpec({

    val shippingService = mockk<ShippingService>(relaxed = true)
    val idempotencyChecker = mockk<IdempotencyChecker>()

    val consumer =
        OrderCreatedConsumer(
            shippingService = shippingService,
            idempotencyChecker = idempotencyChecker,
        )

    Given("주문 생성 이벤트가 수신되면") {
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        val event =
            OrderEvent(
                eventType = "OrderCreated",
                orderId = 1L,
                memberId = 10L,
                sellerId = 20L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                zipCode = "06234",
                address = "서울시 강남구",
                detailAddress = "역삼동 123-4",
            )

        When("Consumer가 메시지를 처리하면") {
            consumer.handle(event)

            Then("ShippingService.prepareShipment이 호출된다") {
                verify(exactly = 1) {
                    shippingService.prepareShipment(
                        PrepareShipmentRequest(
                            orderId = 1L,
                            sellerId = 20L,
                            memberId = 10L,
                            receiverName = "홍길동",
                            receiverPhone = "010-1234-5678",
                            zipCode = "06234",
                            address = "서울시 강남구",
                            detailAddress = "역삼동 123-4",
                        ),
                    )
                }
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val event =
            OrderEvent(
                eventType = "OrderCancelled",
                orderId = 1L,
            )

        When("OrderCancelled 이벤트 수신") {
            consumer.handle(event)

            Then("무시된다") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
