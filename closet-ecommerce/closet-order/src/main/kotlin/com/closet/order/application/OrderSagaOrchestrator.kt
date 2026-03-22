package com.closet.order.application

import com.closet.common.event.InventoryReleasePayload
import com.closet.common.event.ItemPayload
import com.closet.common.event.OrderCreatedPayload
import com.closet.common.event.OutboxService
import com.closet.common.event.PaymentRequestedPayload
import com.closet.order.domain.order.Order
import com.closet.order.domain.saga.SagaExecution
import com.closet.order.domain.saga.SagaStatus
import com.closet.order.domain.saga.SagaStep
import com.closet.order.repository.OrderItemRepository
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.SagaExecutionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * 주문-재고-결제 Saga Orchestrator.
 *
 * Saga 흐름:
 * 1. OrderCreated → Inventory 재고 예약 요청
 * 2. InventoryReserved → Payment 결제 요청
 * 3. PaymentApproved → 주문 완료
 *
 * 보상 흐름:
 * - InventoryReserveFailed → 주문 실패
 * - PaymentFailed → 재고 해제 + 주문 실패
 */
@Service
class OrderSagaOrchestrator(
    private val sagaRepository: SagaExecutionRepository,
    private val outboxService: OutboxService,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional
    fun startSaga(order: Order): SagaExecution {
        val items = orderItemRepository.findByOrderId(order.id)
        val saga = sagaRepository.save(SagaExecution(orderId = order.id))

        // Step 1: OrderCreated → Inventory에서 재고 예약
        outboxService.save(
            aggregateType = "Order",
            aggregateId = order.id.toString(),
            eventType = "OrderCreated",
            payload = OrderCreatedPayload(
                orderId = order.id,
                sagaId = saga.sagaId,
                items = items.map { ItemPayload(it.productOptionId, it.quantity) },
            ),
        )

        saga.currentStep = SagaStep.INVENTORY_RESERVING
        val saved = sagaRepository.save(saga)

        logger.info { "Saga 시작: sagaId=${saga.sagaId}, orderId=${order.id}" }
        return saved
    }

    @Transactional
    fun handleInventoryReserved(sagaId: String) {
        val saga = sagaRepository.findBySagaId(sagaId) ?: run {
            logger.warn { "Saga를 찾을 수 없습니다: sagaId=$sagaId" }
            return
        }

        saga.status = SagaStatus.INVENTORY_RESERVED
        saga.currentStep = SagaStep.PAYMENT_PROCESSING

        val order = orderRepository.findById(saga.orderId).orElseThrow {
            IllegalStateException("주문을 찾을 수 없습니다: orderId=${saga.orderId}")
        }

        // Step 2: PaymentRequested → Payment에서 결제 처리
        outboxService.save(
            aggregateType = "Order",
            aggregateId = order.id.toString(),
            eventType = "PaymentRequested",
            payload = PaymentRequestedPayload(
                orderId = order.id,
                sagaId = sagaId,
                amount = order.paymentAmount.amount.toLong(),
            ),
        )

        sagaRepository.save(saga)
        logger.info { "재고 예약 완료 → 결제 요청: sagaId=$sagaId, orderId=${order.id}" }
    }

    @Transactional
    fun handlePaymentApproved(sagaId: String) {
        val saga = sagaRepository.findBySagaId(sagaId) ?: run {
            logger.warn { "Saga를 찾을 수 없습니다: sagaId=$sagaId" }
            return
        }

        saga.status = SagaStatus.COMPLETED
        saga.currentStep = SagaStep.COMPLETED

        val order = orderRepository.findById(saga.orderId).orElseThrow {
            IllegalStateException("주문을 찾을 수 없습니다: orderId=${saga.orderId}")
        }
        order.pay() // STOCK_RESERVED → PAID
        orderRepository.save(order)
        sagaRepository.save(saga)

        logger.info { "Saga 완료 (결제 승인): sagaId=$sagaId, orderId=${order.id}" }
    }

    @Transactional
    fun handleInventoryFailed(sagaId: String, reason: String) {
        val saga = sagaRepository.findBySagaId(sagaId) ?: run {
            logger.warn { "Saga를 찾을 수 없습니다: sagaId=$sagaId" }
            return
        }

        saga.status = SagaStatus.FAILED
        saga.failureReason = reason

        val order = orderRepository.findById(saga.orderId).orElseThrow {
            IllegalStateException("주문을 찾을 수 없습니다: orderId=${saga.orderId}")
        }
        order.fail() // → FAILED
        orderRepository.save(order)
        sagaRepository.save(saga)

        logger.info { "Saga 실패 (재고 부족): sagaId=$sagaId, orderId=${order.id}, reason=$reason" }
    }

    @Transactional
    fun handlePaymentFailed(sagaId: String, reason: String) {
        val saga = sagaRepository.findBySagaId(sagaId) ?: run {
            logger.warn { "Saga를 찾을 수 없습니다: sagaId=$sagaId" }
            return
        }

        saga.status = SagaStatus.COMPENSATING
        saga.currentStep = SagaStep.COMPENSATING
        saga.failureReason = reason

        val order = orderRepository.findById(saga.orderId).orElseThrow {
            IllegalStateException("주문을 찾을 수 없습니다: orderId=${saga.orderId}")
        }

        val items = orderItemRepository.findByOrderId(order.id)

        // 보상 트랜잭션: 재고 해제 요청
        outboxService.save(
            aggregateType = "Order",
            aggregateId = order.id.toString(),
            eventType = "InventoryReleaseRequested",
            payload = InventoryReleasePayload(
                orderId = order.id,
                sagaId = sagaId,
                items = items.map { ItemPayload(it.productOptionId, it.quantity) },
            ),
        )

        order.fail() // → FAILED
        orderRepository.save(order)
        sagaRepository.save(saga)

        logger.info { "Saga 보상 시작 (결제 실패 → 재고 해제): sagaId=$sagaId, orderId=${order.id}, reason=$reason" }
    }
}
