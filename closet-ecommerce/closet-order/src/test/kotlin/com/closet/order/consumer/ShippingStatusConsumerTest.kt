package com.closet.order.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.common.vo.Money
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord

class ShippingStatusConsumerTest : BehaviorSpec({

    val orderRepository = mockk<OrderRepository>()
    val orderStatusHistoryRepository = mockk<OrderStatusHistoryRepository>(relaxed = true)
    val idempotencyChecker = mockk<IdempotencyChecker>()
    val objectMapper = jacksonObjectMapper()

    val consumer = ShippingStatusConsumer(
        orderRepository = orderRepository,
        orderStatusHistoryRepository = orderStatusHistoryRepository,
        idempotencyChecker = idempotencyChecker,
        objectMapper = objectMapper,
    )

    fun createOrder(): Order {
        return Order.create(
            memberId = 1L,
            sellerId = 10L,
            items = listOf(
                OrderItem.create(
                    productId = 100L,
                    productOptionId = 1000L,
                    productName = "슬림핏 청바지",
                    optionName = "M / 블루",
                    categoryId = 5L,
                    quantity = 1,
                    unitPrice = Money.of(39900),
                )
            ),
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            zipCode = "06234",
            address = "서울시 강남구",
            detailAddress = "역삼동 123-4",
        )
    }

    fun makeRecord(eventId: String, orderId: Long, shippingStatus: String): ConsumerRecord<String, String> {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventType" to "ShippingStatusChanged",
                "eventId" to eventId,
                "orderId" to orderId,
                "shippingStatus" to shippingStatus,
            )
        )
        return ConsumerRecord("event.closet.shipping", 0, 0, orderId.toString(), payload)
    }

    Given("PAID 상태 주문에 READY 배송 이벤트 수신") {
        val order = createOrder()
        order.place() // PENDING -> STOCK_RESERVED
        order.pay()   // STOCK_RESERVED -> PAID

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("ShippingStatusChanged(READY) 이벤트 수신") {
            consumer.consume(makeRecord("evt-1", order.id, "READY"))

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
            consumer.consume(makeRecord("evt-2", order.id, "IN_TRANSIT"))

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
            consumer.consume(makeRecord("evt-3", order.id, "DELIVERED"))

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
            consumer.consume(makeRecord("evt-4", order.id, "READY"))

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
            consumer.consume(makeRecord("evt-5", order.id, "IN_TRANSIT"))

            Then("주문 상태가 변경되지 않는다 (PAID -> SHIPPED 불가)") {
                order.status shouldBe OrderStatus.PAID
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventType" to "ReturnApproved",
                "orderId" to 1L,
            )
        )
        val record = ConsumerRecord<String, String>("event.closet.shipping", 0, 0, "1", payload)

        When("ReturnApproved 이벤트 수신") {
            consumer.consume(record)

            Then("무시된다") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
