package com.closet.order.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.common.vo.Money
import com.closet.order.consumer.event.ShippingEvent
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ShippingStatusConsumerTest : BehaviorSpec({

    val orderRepository = mockk<OrderRepository>()
    val orderStatusHistoryRepository = mockk<OrderStatusHistoryRepository>(relaxed = true)
    val idempotencyChecker = mockk<IdempotencyChecker>()

    val consumer =
        ShippingStatusConsumer(
            orderRepository = orderRepository,
            orderStatusHistoryRepository = orderStatusHistoryRepository,
            idempotencyChecker = idempotencyChecker,
        )

    fun createOrder(): Order {
        return Order.create(
            memberId = 1L,
            sellerId = 10L,
            items =
                listOf(
                    OrderItem.create(
                        productId = 100L,
                        productOptionId = 1000L,
                        productName = "슬림핏 청바지",
                        optionName = "M / 블루",
                        categoryId = 5L,
                        quantity = 1,
                        unitPrice = Money.of(39900),
                    ),
                ),
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            zipCode = "06234",
            address = "서울시 강남구",
            detailAddress = "역삼동 123-4",
        )
    }

    fun makeEvent(
        eventId: String,
        orderId: Long,
        shippingStatus: String,
    ): ShippingEvent {
        return ShippingEvent(
            eventType = "ShippingStatusChanged",
            eventId = eventId,
            orderId = orderId,
            shippingStatus = shippingStatus,
        )
    }

    Given("PAID 상태 주문에 READY 배송 이벤트 수신") {
        val order = createOrder()
        order.place() // PENDING -> STOCK_RESERVED
        order.pay() // STOCK_RESERVED -> PAID

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStatusChanged(READY) 이벤트 수신") {
            consumer.handle(makeEvent("evt-1", order.id, "READY"))

            Then("주문 상태가 PREPARING으로 변경된다") {
                order.status shouldBe OrderStatus.PREPARING
            }
        }
    }

    Given("PREPARING 상태 주문에 IN_TRANSIT 배송 이벤트 수신") {
        val order = createOrder()
        order.place()
        order.pay()
        order.prepare()

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStatusChanged(IN_TRANSIT) 이벤트 수신") {
            consumer.handle(makeEvent("evt-2", order.id, "IN_TRANSIT"))

            Then("주문 상태가 SHIPPED로 변경된다") {
                order.status shouldBe OrderStatus.SHIPPED
            }
        }
    }

    Given("SHIPPED 상태 주문에 DELIVERED 배송 이벤트 수신") {
        val order = createOrder()
        order.place()
        order.pay()
        order.prepare()
        order.ship()

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStatusChanged(DELIVERED) 이벤트 수신") {
            consumer.handle(makeEvent("evt-3", order.id, "DELIVERED"))

            Then("주문 상태가 DELIVERED로 변경된다") {
                order.status shouldBe OrderStatus.DELIVERED
            }
        }
    }

    Given("이미 CANCELLED 상태인 주문에 배송 이벤트 수신") {
        val order = createOrder()
        order.place()
        order.cancel("테스트 취소")

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStatusChanged(READY) 이벤트 수신") {
            consumer.handle(makeEvent("evt-4", order.id, "READY"))

            Then("주문 상태가 변경되지 않는다") {
                order.status shouldBe OrderStatus.CANCELLED
            }
        }
    }

    Given("잘못된 상태 전이 시도") {
        val order = createOrder()
        order.place()
        order.pay()

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("PAID 상태에서 IN_TRANSIT(SHIPPED) 이벤트 수신") {
            consumer.handle(makeEvent("evt-5", order.id, "IN_TRANSIT"))

            Then("주문 상태가 변경되지 않는다 (PAID -> SHIPPED 불가)") {
                order.status shouldBe OrderStatus.PAID
            }
        }
    }

    Given("ShippingStarted 이벤트 수신 — 주문 상태 SHIPPING 전이") {
        val order = createOrder()
        order.place()
        order.pay()

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStarted 이벤트 수신") {
            val event =
                ShippingEvent(
                    eventType = "ShippingStarted",
                    eventId = "evt-shipping-started-1",
                    orderId = order.id,
                    shippingId = 100L,
                    carrier = "CJ",
                    trackingNumber = "CJ1234567890",
                )
            consumer.handle(event)

            Then("주문 상태가 PREPARING으로 변경된다") {
                order.status shouldBe OrderStatus.PREPARING
            }
        }
    }

    Given("DeliveryConfirmed 이벤트 수신 — 주문 상태 CONFIRMED 전이") {
        val order = createOrder()
        order.place()
        order.pay()
        order.prepare()
        order.ship()
        order.deliver()

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("DeliveryConfirmed 이벤트 수신") {
            val event =
                ShippingEvent(
                    eventType = "DeliveryConfirmed",
                    eventId = "evt-delivery-confirmed-1",
                    orderId = order.id,
                    memberId = 1L,
                    shippingId = 100L,
                )
            consumer.handle(event)

            Then("주문 상태가 CONFIRMED로 변경된다") {
                order.status shouldBe OrderStatus.CONFIRMED
            }
        }
    }

    Given("DeliveryConfirmed 수신 — DELIVERED가 아닌 상태에서는 무시") {
        val order = createOrder()
        order.place()
        order.pay()
        order.prepare()
        order.ship()
        // SHIPPED 상태 (DELIVERED 아님)

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("DeliveryConfirmed 이벤트 수신") {
            val event =
                ShippingEvent(
                    eventType = "DeliveryConfirmed",
                    eventId = "evt-delivery-confirmed-2",
                    orderId = order.id,
                    memberId = 1L,
                    shippingId = 100L,
                )
            consumer.handle(event)

            Then("주문 상태가 변경되지 않는다 (SHIPPED -> CONFIRMED 불가)") {
                order.status shouldBe OrderStatus.SHIPPED
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val event =
            ShippingEvent(
                eventType = "ReturnApproved",
                orderId = 1L,
            )

        When("ReturnApproved 이벤트 수신") {
            consumer.handle(event)

            Then("무시된다") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
