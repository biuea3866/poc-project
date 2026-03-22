package com.closet.order.application

import com.closet.common.event.OutboxEvent
import com.closet.common.event.OutboxService
import com.closet.common.vo.Money
import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.saga.SagaExecution
import com.closet.order.domain.saga.SagaStatus
import com.closet.order.domain.saga.SagaStep
import com.closet.order.repository.OrderItemRepository
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.SagaExecutionRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional

class OrderSagaOrchestratorTest : BehaviorSpec({

    val sagaRepository = mockk<SagaExecutionRepository>()
    val outboxService = mockk<OutboxService>(relaxed = true)
    val orderRepository = mockk<OrderRepository>()
    val orderItemRepository = mockk<OrderItemRepository>()

    val orchestrator = OrderSagaOrchestrator(
        sagaRepository = sagaRepository,
        outboxService = outboxService,
        orderRepository = orderRepository,
        orderItemRepository = orderItemRepository,
    )

    fun createTestOrder(): Order {
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
                    quantity = 2,
                    unitPrice = Money.of(39900),
                ),
            ),
            receiverName = "홍길동",
            receiverPhone = "010-1234-5678",
            zipCode = "06234",
            address = "서울시 강남구",
            detailAddress = "역삼동 123-4",
        )
        order.place()
        return order
    }

    Given("Saga Happy Path — 주문 생성 → 재고 예약 → 결제 승인 → 완료") {

        When("startSaga 호출") {
            val order = createTestOrder()
            val sagaSlot = slot<SagaExecution>()
            val items = listOf(
                OrderItem.create(
                    orderId = order.id,
                    productId = 100L,
                    productOptionId = 1000L,
                    productName = "슬림핏 청바지",
                    optionName = "M / 블루",
                    categoryId = 5L,
                    quantity = 2,
                    unitPrice = Money.of(39900),
                ),
            )

            every { orderItemRepository.findByOrderId(any()) } returns items
            every { sagaRepository.save(capture(sagaSlot)) } answers { sagaSlot.captured }

            val saga = orchestrator.startSaga(order)

            Then("Saga가 INVENTORY_RESERVING 단계로 생성된다") {
                saga.currentStep shouldBe SagaStep.INVENTORY_RESERVING
                saga.status shouldBe SagaStatus.STARTED
                saga.orderId shouldBe order.id
                saga.sagaId shouldNotBe null
            }

            Then("OrderCreated Outbox 이벤트가 저장된다") {
                verify(exactly = 1) {
                    outboxService.save(
                        aggregateType = "Order",
                        aggregateId = order.id.toString(),
                        eventType = "OrderCreated",
                        payload = any(),
                    )
                }
            }
        }

        When("handleInventoryReserved 호출") {
            val order = createTestOrder()
            val saga = SagaExecution(orderId = order.id)
            saga.currentStep = SagaStep.INVENTORY_RESERVING
            val sagaSlot = slot<SagaExecution>()

            every { sagaRepository.findBySagaId(saga.sagaId) } returns saga
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { sagaRepository.save(capture(sagaSlot)) } answers { sagaSlot.captured }

            orchestrator.handleInventoryReserved(saga.sagaId)

            Then("Saga가 PAYMENT_PROCESSING 단계로 진행된다") {
                saga.status shouldBe SagaStatus.INVENTORY_RESERVED
                saga.currentStep shouldBe SagaStep.PAYMENT_PROCESSING
            }

            Then("PaymentRequested Outbox 이벤트가 저장된다") {
                verify(exactly = 1) {
                    outboxService.save(
                        aggregateType = "Order",
                        aggregateId = order.id.toString(),
                        eventType = "PaymentRequested",
                        payload = any(),
                    )
                }
            }
        }

        When("handlePaymentApproved 호출") {
            val order = createTestOrder()
            val saga = SagaExecution(orderId = order.id)
            saga.status = SagaStatus.INVENTORY_RESERVED
            saga.currentStep = SagaStep.PAYMENT_PROCESSING
            val sagaSlot = slot<SagaExecution>()

            every { sagaRepository.findBySagaId(saga.sagaId) } returns saga
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { orderRepository.save(any()) } answers { firstArg() }
            every { sagaRepository.save(capture(sagaSlot)) } answers { sagaSlot.captured }

            orchestrator.handlePaymentApproved(saga.sagaId)

            Then("Saga가 COMPLETED 상태로 완료된다") {
                saga.status shouldBe SagaStatus.COMPLETED
                saga.currentStep shouldBe SagaStep.COMPLETED
            }

            Then("주문이 PAID 상태로 변경된다") {
                order.status shouldBe OrderStatus.PAID
            }
        }
    }

    Given("Saga 실패 — 재고 부족") {

        When("handleInventoryFailed 호출") {
            val order = createTestOrder()
            val saga = SagaExecution(orderId = order.id)
            saga.currentStep = SagaStep.INVENTORY_RESERVING
            val sagaSlot = slot<SagaExecution>()

            every { sagaRepository.findBySagaId(saga.sagaId) } returns saga
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { orderRepository.save(any()) } answers { firstArg() }
            every { sagaRepository.save(capture(sagaSlot)) } answers { sagaSlot.captured }

            orchestrator.handleInventoryFailed(saga.sagaId, "재고 부족: productOptionId=1000")

            Then("Saga가 FAILED 상태로 변경된다") {
                saga.status shouldBe SagaStatus.FAILED
                saga.failureReason shouldBe "재고 부족: productOptionId=1000"
            }

            Then("주문이 FAILED 상태로 변경된다") {
                order.status shouldBe OrderStatus.FAILED
            }
        }
    }

    Given("Saga 보상 — 결제 실패 후 재고 해제") {

        When("handlePaymentFailed 호출") {
            val order = createTestOrder()
            val saga = SagaExecution(orderId = order.id)
            saga.status = SagaStatus.INVENTORY_RESERVED
            saga.currentStep = SagaStep.PAYMENT_PROCESSING
            val sagaSlot = slot<SagaExecution>()
            val items = listOf(
                OrderItem.create(
                    orderId = order.id,
                    productId = 100L,
                    productOptionId = 1000L,
                    productName = "슬림핏 청바지",
                    optionName = "M / 블루",
                    categoryId = 5L,
                    quantity = 2,
                    unitPrice = Money.of(39900),
                ),
            )

            every { sagaRepository.findBySagaId(saga.sagaId) } returns saga
            every { orderRepository.findById(order.id) } returns Optional.of(order)
            every { orderItemRepository.findByOrderId(order.id) } returns items
            every { orderRepository.save(any()) } answers { firstArg() }
            every { sagaRepository.save(capture(sagaSlot)) } answers { sagaSlot.captured }

            orchestrator.handlePaymentFailed(saga.sagaId, "잔액 부족")

            Then("Saga가 COMPENSATING 상태로 변경된다") {
                saga.status shouldBe SagaStatus.COMPENSATING
                saga.currentStep shouldBe SagaStep.COMPENSATING
                saga.failureReason shouldBe "잔액 부족"
            }

            Then("InventoryReleaseRequested 보상 이벤트가 발행된다") {
                verify(exactly = 1) {
                    outboxService.save(
                        aggregateType = "Order",
                        aggregateId = order.id.toString(),
                        eventType = "InventoryReleaseRequested",
                        payload = any(),
                    )
                }
            }

            Then("주문이 FAILED 상태로 변경된다") {
                order.status shouldBe OrderStatus.FAILED
            }
        }
    }

    Given("멱등성 — Saga를 찾을 수 없는 경우") {

        When("존재하지 않는 sagaId로 handleInventoryReserved 호출") {
            clearMocks(orderRepository, recordedCalls = true, answers = false)
            every { sagaRepository.findBySagaId("nonexistent-saga-id") } returns null

            orchestrator.handleInventoryReserved("nonexistent-saga-id")

            Then("예외 없이 무시된다") {
                verify(exactly = 0) { orderRepository.findById(any()) }
            }
        }

        When("존재하지 않는 sagaId로 handlePaymentApproved 호출") {
            clearMocks(orderRepository, recordedCalls = true, answers = false)
            every { sagaRepository.findBySagaId("nonexistent-saga-id") } returns null

            orchestrator.handlePaymentApproved("nonexistent-saga-id")

            Then("예외 없이 무시된다") {
                verify(exactly = 0) { orderRepository.findById(any()) }
            }
        }
    }
})
