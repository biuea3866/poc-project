package com.closet.payment.infrastructure.kafka

import com.closet.common.kafka.DomainEvent
import com.closet.common.kafka.idempotency.IdempotencyService
import com.closet.common.kafka.KafkaTopics
import com.closet.common.kafka.outbox.OutboxService
import com.closet.common.kafka.PaymentApprovedPayload
import com.closet.common.kafka.PaymentFailedPayload
import com.closet.common.kafka.PaymentRequestedPayload
import com.closet.payment.application.ConfirmPaymentRequest
import com.closet.payment.application.PaymentService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Payment 이벤트 리스너.
 * Order 서비스의 결제 요청 이벤트를 수신하여 결제를 처리한다.
 * (InventoryReserved 후 Order가 PaymentRequested를 order.events로 발행 → Payment가 구독)
 */
@Component
class PaymentEventListener(
    private val paymentService: PaymentService,
    private val outboxService: OutboxService,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "payment-consumer"
    }

    @KafkaListener(topics = [KafkaTopics.ORDER_EVENTS], groupId = CONSUMER_GROUP)
    fun handleOrderEvent(message: String) {
        val event = objectMapper.readValue(message, DomainEvent::class.java)
        logger.info { "Order 이벤트 수신: eventType=${event.eventType}, eventId=${event.eventId}" }

        if (idempotencyService.isProcessed(event.eventId, CONSUMER_GROUP)) {
            logger.debug { "이미 처리된 이벤트 (skip): eventId=${event.eventId}" }
            return
        }

        when (event.eventType) {
            "PaymentRequested" -> handlePaymentRequested(event)
            else -> {
                // PaymentRequested 이외의 Order 이벤트는 무시
            }
        }

        idempotencyService.markProcessed(event.eventId, CONSUMER_GROUP)
    }

    private fun handlePaymentRequested(event: DomainEvent) {
        val payload = objectMapper.readValue(event.payload, PaymentRequestedPayload::class.java)

        try {
            val result = paymentService.confirm(
                ConfirmPaymentRequest(
                    paymentKey = "saga-${UUID.randomUUID()}",
                    orderId = payload.orderId,
                    amount = payload.amount,
                ),
            )

            // 결제 승인 성공 이벤트 발행
            outboxService.save(
                aggregateType = "Payment",
                aggregateId = payload.orderId.toString(),
                eventType = "PaymentApproved",
                payload = PaymentApprovedPayload(
                    orderId = payload.orderId,
                    sagaId = payload.sagaId,
                    paymentId = result.id,
                ),
            )

            logger.info { "결제 승인 성공: orderId=${payload.orderId}, sagaId=${payload.sagaId}, paymentId=${result.id}" }
        } catch (e: Exception) {
            logger.error(e) { "결제 승인 실패: orderId=${payload.orderId}, sagaId=${payload.sagaId}" }

            // 결제 실패 이벤트 발행
            outboxService.save(
                aggregateType = "Payment",
                aggregateId = payload.orderId.toString(),
                eventType = "PaymentFailed",
                payload = PaymentFailedPayload(
                    orderId = payload.orderId,
                    sagaId = payload.sagaId,
                    reason = e.message,
                ),
            )
        }
    }
}
