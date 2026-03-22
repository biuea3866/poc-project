package com.closet.order.infrastructure.kafka

import com.closet.common.event.DomainEvent
import com.closet.common.event.IdempotencyService
import com.closet.common.event.KafkaTopics
import com.closet.order.application.OrderSagaOrchestrator
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Order Saga 이벤트 리스너.
 * Inventory, Payment 서비스로부터의 응답 이벤트를 수신하여 Saga를 진행한다.
 */
@Component
class OrderSagaEventListener(
    private val orchestrator: OrderSagaOrchestrator,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "order-saga"
    }

    @KafkaListener(topics = [KafkaTopics.INVENTORY_EVENTS], groupId = CONSUMER_GROUP)
    fun handleInventoryEvent(message: String) {
        val event = objectMapper.readValue(message, DomainEvent::class.java)
        logger.info { "Inventory 이벤트 수신: eventType=${event.eventType}, eventId=${event.eventId}" }

        if (idempotencyService.isProcessed(event.eventId, CONSUMER_GROUP)) {
            logger.debug { "이미 처리된 이벤트 (skip): eventId=${event.eventId}" }
            return
        }

        when (event.eventType) {
            "InventoryReserved" -> {
                val sagaId = extractSagaId(event)
                orchestrator.handleInventoryReserved(sagaId)
            }
            "InventoryReserveFailed" -> {
                val sagaId = extractSagaId(event)
                val reason = extractReason(event)
                orchestrator.handleInventoryFailed(sagaId, reason)
            }
            else -> logger.warn { "처리되지 않는 Inventory 이벤트: ${event.eventType}" }
        }

        idempotencyService.markProcessed(event.eventId, CONSUMER_GROUP)
    }

    @KafkaListener(topics = [KafkaTopics.PAYMENT_EVENTS], groupId = CONSUMER_GROUP)
    fun handlePaymentEvent(message: String) {
        val event = objectMapper.readValue(message, DomainEvent::class.java)
        logger.info { "Payment 이벤트 수신: eventType=${event.eventType}, eventId=${event.eventId}" }

        if (idempotencyService.isProcessed(event.eventId, CONSUMER_GROUP)) {
            logger.debug { "이미 처리된 이벤트 (skip): eventId=${event.eventId}" }
            return
        }

        when (event.eventType) {
            "PaymentApproved" -> {
                val sagaId = extractSagaId(event)
                orchestrator.handlePaymentApproved(sagaId)
            }
            "PaymentFailed" -> {
                val sagaId = extractSagaId(event)
                val reason = extractReason(event)
                orchestrator.handlePaymentFailed(sagaId, reason)
            }
            else -> logger.warn { "처리되지 않는 Payment 이벤트: ${event.eventType}" }
        }

        idempotencyService.markProcessed(event.eventId, CONSUMER_GROUP)
    }

    private fun extractSagaId(event: DomainEvent): String {
        val payload = objectMapper.readTree(event.payload)
        return payload.get("sagaId").asText()
    }

    private fun extractReason(event: DomainEvent): String {
        val payload = objectMapper.readTree(event.payload)
        return payload.get("reason")?.asText() ?: "Unknown reason"
    }
}
