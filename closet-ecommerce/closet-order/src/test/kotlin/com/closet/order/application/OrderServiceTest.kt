package com.closet.order.application

import com.closet.common.exception.BusinessException
import com.closet.common.vo.Money
import com.closet.order.domain.event.OrderCreatedEvent
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.presentation.dto.CreateOrderItemRequest
import com.closet.order.presentation.dto.CreateOrderRequest
import com.closet.order.repository.OrderItemRepository
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

class OrderServiceTest : BehaviorSpec({

    val orderRepository = mockk<OrderRepository>()
    val orderItemRepository = mockk<OrderItemRepository>()
    val orderStatusHistoryRepository = mockk<OrderStatusHistoryRepository>()
    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    val sagaOrchestrator = mockk<OrderSagaOrchestrator>(relaxed = true)

    val orderService = OrderService(
        orderRepository = orderRepository,
        orderItemRepository = orderItemRepository,
        orderStatusHistoryRepository = orderStatusHistoryRepository,
        eventPublisher = eventPublisher,
        sagaOrchestrator = sagaOrchestrator,
    )

    Given("주문 생성") {
        val request = CreateOrderRequest(
            memberId = 1L,
            sellerId = 10L,
            items = listOf(
                CreateOrderItemRequest(
                    productId = 100L,
                    productOptionId = 1000L,
                    productName = "슬림핏 청바지",
                    optionName = "M / 블루",
                    categoryId = 5L,
                    quantity = 2,
                    unitPrice = BigDecimal("39900"),
                )
            ),
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            zipCode = "06234",
            address = "서울시 강남구",
            detailAddress = "역삼동 123-4",
            shippingFee = BigDecimal("3000"),
            discountAmount = BigDecimal.ZERO,
        )

        val orderSlot = slot<Order>()
        val orderItemSlot = slot<OrderItem>()
        val historySlot = slot<OrderStatusHistory>()

        every { orderRepository.save(capture(orderSlot)) } answers {
            orderSlot.captured
        }
        every { orderItemRepository.save(capture(orderItemSlot)) } answers {
            orderItemSlot.captured
        }
        every { orderStatusHistoryRepository.save(capture(historySlot)) } answers {
            historySlot.captured
        }

        When("정상 주문 생성 요청") {
            val result = orderService.createOrder(request)

            Then("주문이 STOCK_RESERVED 상태로 생성된다") {
                result.status shouldBe OrderStatus.STOCK_RESERVED
            }

            Then("결제 금액이 올바르게 계산된다") {
                // totalAmount = 39900 * 2 = 79800, shippingFee = 3000, discount = 0
                result.paymentAmount.compareTo(BigDecimal("82800")) shouldBe 0
            }

            Then("주문번호가 생성된다") {
                result.orderNumber shouldNotBe null
                result.orderNumber.length shouldBe 20 // yyyyMMddHHmmss(14) + random(6)
            }

            Then("이벤트가 발행된다") {
                verify(atLeast = 1) { eventPublisher.publishEvent(any<OrderCreatedEvent>()) }
            }
        }
    }

    Given("주문 취소") {
        When("PAID 상태 주문 취소") {
            val order = Order.create(
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
            order.place()
            order.pay()

            val items = listOf(
                OrderItem.create(
                    orderId = order.id,
                    productId = 100L,
                    productOptionId = 1000L,
                    productName = "슬림핏 청바지",
                    optionName = "M / 블루",
                    categoryId = 5L,
                    quantity = 1,
                    unitPrice = Money.of(39900),
                )
            )

            every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order
            every { orderItemRepository.findByOrderId(any()) } returns items
            every { orderStatusHistoryRepository.save(any()) } answers { firstArg() }

            val result = orderService.cancelOrder(order.id, "단순 변심")

            Then("주문이 CANCELLED 상태로 변경된다") {
                result.status shouldBe OrderStatus.CANCELLED
            }
        }

        When("SHIPPED 상태 주문 취소 시도") {
            val order = Order.create(
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
            order.place()
            order.pay()
            order.prepare()
            order.ship()

            every { orderRepository.findByIdAndDeletedAtIsNull(any()) } returns order

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    orderService.cancelOrder(order.id, "단순 변심")
                }
            }
        }
    }

    Given("잘못된 상태 전이") {
        When("PENDING에서 PAID로 직접 전이 시도") {
            val order = Order.create(
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

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    order.pay() // PENDING -> PAID 불가
                }
            }
        }
    }
})
