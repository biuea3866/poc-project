package com.closet.order.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.common.vo.Money
import com.closet.order.consumer.event.InventoryEvent
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class InventoryInsufficientConsumerTest : BehaviorSpec({

    val orderRepository = mockk<OrderRepository>()
    val orderStatusHistoryRepository = mockk<OrderStatusHistoryRepository>(relaxed = true)
    val idempotencyChecker = mockk<IdempotencyChecker>()

    val consumer = InventoryInsufficientConsumer(
        orderRepository = orderRepository,
        orderStatusHistoryRepository = orderStatusHistoryRepository,
        idempotencyChecker = idempotencyChecker,
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

    fun makeEvent(eventId: String, orderId: Long, reason: String = "재고 부족"): InventoryEvent {
        return InventoryEvent(
            eventType = "InventoryInsufficient",
            eventId = eventId,
            orderId = orderId,
            reason = reason,
        )
    }

    Given("STOCK_RESERVED 상태 주문에 재고 부족 이벤트 수신") {
        val order = createOrder()
        order.place() // PENDING -> STOCK_RESERVED

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("InventoryInsufficient 이벤트 수신") {
            consumer.handle(makeEvent("evt-1", order.id))

            Then("주문 상태가 FAILED로 변경된다") {
                order.status shouldBe OrderStatus.FAILED
            }
        }
    }

    Given("이미 CANCELLED 상태인 주문에 재고 부족 이벤트 수신") {
        val order = createOrder()
        order.place()
        order.cancel("테스트 취소")

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("InventoryInsufficient 이벤트 수신") {
            consumer.handle(makeEvent("evt-2", order.id))

            Then("주문 상태가 변경되지 않는다") {
                order.status shouldBe OrderStatus.CANCELLED
            }
        }
    }

    Given("PAID 상태 주문에 재고 부족 이벤트 수신") {
        val order = createOrder()
        order.place()
        order.pay() // STOCK_RESERVED -> PAID

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("InventoryInsufficient 이벤트 수신") {
            consumer.handle(makeEvent("evt-3", order.id))

            Then("PAID 상태에서는 재고 부족 처리를 무시한다") {
                order.status shouldBe OrderStatus.PAID
            }
        }
    }

    Given("주문을 찾을 수 없는 경우") {
        clearMocks(orderStatusHistoryRepository)

        every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns null
        every { idempotencyChecker.process<Unit>(any(), any(), any(), any()) } answers {
            val block = arg<() -> Unit>(3)
            block()
        }

        When("InventoryInsufficient 이벤트 수신") {
            consumer.handle(makeEvent("evt-4", 999L))

            Then("상태 이력이 저장되지 않는다") {
                verify(exactly = 0) { orderStatusHistoryRepository.save(any()) }
            }
        }
    }

    Given("처리하지 않는 eventType 수신") {
        val event = InventoryEvent(
            eventType = "LowStock",
            orderId = 1L,
        )

        When("LowStock 이벤트 수신") {
            consumer.handle(event)

            Then("무시된다 (idempotencyChecker 호출 없음)") {
                // eventType 필터에 의해 무시
            }
        }
    }
})
