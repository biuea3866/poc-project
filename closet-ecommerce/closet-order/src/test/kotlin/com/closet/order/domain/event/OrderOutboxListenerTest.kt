package com.closet.order.domain.event

import com.closet.common.outbox.OutboxEvent
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.common.vo.Money
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class OrderOutboxListenerTest : BehaviorSpec({

    val outboxEventPublisher = mockk<OutboxEventPublisher>()
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val listener =
        OrderOutboxListener(
            outboxEventPublisher = outboxEventPublisher,
            objectMapper = objectMapper,
        )

    Given("OrderCreatedEvent 발생") {
        val event =
            OrderCreatedEvent(
                orderId = 1L,
                memberId = 100L,
                items =
                    listOf(
                        OrderCreatedEvent.OrderItemInfo(productOptionId = 1000L, quantity = 2),
                    ),
            )

        val topicSlot = slot<String>()
        val eventTypeSlot = slot<String>()
        val aggregateIdSlot = slot<String>()

        every {
            outboxEventPublisher.publish(
                aggregateType = any(),
                aggregateId = capture(aggregateIdSlot),
                eventType = capture(eventTypeSlot),
                topic = capture(topicSlot),
                partitionKey = any(),
                payload = any(),
            )
        } returns mockk<OutboxEvent>()

        When("OrderOutboxListener가 이벤트를 처리하면") {
            listener.handleOrderCreated(event)

            Then("event.closet.order 토픽으로 outbox 이벤트가 저장된다") {
                topicSlot.captured shouldBe "event.closet.order"
                eventTypeSlot.captured shouldBe "OrderCreated"
                aggregateIdSlot.captured shouldBe "1"
            }

            Then("OutboxEventPublisher.publish가 호출된다") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Order",
                        aggregateId = "1",
                        eventType = "OrderCreated",
                        topic = "event.closet.order",
                        partitionKey = "1",
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("OrderCancelledEvent 발생") {
        val event =
            OrderCancelledEvent(
                orderId = 2L,
                reason = "단순 변심",
                items =
                    listOf(
                        OrderCancelledEvent.OrderItemInfo(productOptionId = 2000L, quantity = 1),
                    ),
            )

        every {
            outboxEventPublisher.publish(
                aggregateType = any(),
                aggregateId = any(),
                eventType = any(),
                topic = any(),
                partitionKey = any(),
                payload = any(),
            )
        } returns mockk<OutboxEvent>()

        When("OrderOutboxListener가 이벤트를 처리하면") {
            listener.handleOrderCancelled(event)

            Then("event.closet.order 토픽으로 outbox 이벤트가 저장된다") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Order",
                        aggregateId = "2",
                        eventType = "OrderCancelled",
                        topic = "event.closet.order",
                        partitionKey = "2",
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("OrderPaidEvent 발생") {
        val event =
            OrderPaidEvent(
                orderId = 3L,
                paymentAmount = Money.of(50000),
            )

        every {
            outboxEventPublisher.publish(
                aggregateType = any(),
                aggregateId = any(),
                eventType = any(),
                topic = any(),
                partitionKey = any(),
                payload = any(),
            )
        } returns mockk<OutboxEvent>()

        When("OrderOutboxListener가 이벤트를 처리하면") {
            listener.handleOrderPaid(event)

            Then("event.closet.order 토픽으로 outbox 이벤트가 저장된다") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Order",
                        aggregateId = "3",
                        eventType = "OrderPaid",
                        topic = "event.closet.order",
                        partitionKey = "3",
                        payload = any(),
                    )
                }
            }
        }
    }

    Given("OrderConfirmedEvent 발생") {
        val event =
            OrderConfirmedEvent(
                orderId = 4L,
                memberId = 400L,
            )

        every {
            outboxEventPublisher.publish(
                aggregateType = any(),
                aggregateId = any(),
                eventType = any(),
                topic = any(),
                partitionKey = any(),
                payload = any(),
            )
        } returns mockk<OutboxEvent>()

        When("OrderOutboxListener가 이벤트를 처리하면") {
            listener.handleOrderConfirmed(event)

            Then("event.closet.order 토픽으로 outbox 이벤트가 저장된다") {
                verify(exactly = 1) {
                    outboxEventPublisher.publish(
                        aggregateType = "Order",
                        aggregateId = "4",
                        eventType = "OrderConfirmed",
                        topic = "event.closet.order",
                        partitionKey = "4",
                        payload = any(),
                    )
                }
            }
        }
    }
})
