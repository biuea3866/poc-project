package com.closet.inventory.infrastructure.kafka

import com.closet.common.event.DomainEvent
import com.closet.common.event.IdempotencyService
import com.closet.common.event.InventoryFailedPayload
import com.closet.common.event.InventoryReleasePayload
import com.closet.common.event.InventoryReservedPayload
import com.closet.common.event.KafkaTopics
import com.closet.common.event.OrderCreatedPayload
import com.closet.common.event.OutboxService
import com.closet.inventory.application.InventoryService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Inventory 이벤트 리스너.
 * Order 서비스의 주문 생성/재고 해제 요청 이벤트를 수신하여 재고를 처리한다.
 */
@Component
class InventoryEventListener(
    private val inventoryService: InventoryService,
    private val outboxService: OutboxService,
    private val idempotencyService: IdempotencyService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "inventory-consumer"
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
            "OrderCreated" -> handleOrderCreated(event)
            "InventoryReleaseRequested" -> handleInventoryRelease(event)
            else -> logger.warn { "처리되지 않는 Order 이벤트: ${event.eventType}" }
        }

        idempotencyService.markProcessed(event.eventId, CONSUMER_GROUP)
    }

    private fun handleOrderCreated(event: DomainEvent) {
        val payload = objectMapper.readValue(event.payload, OrderCreatedPayload::class.java)

        try {
            // 각 항목의 재고 예약
            payload.items.forEach { item ->
                inventoryService.reserveStock(
                    productOptionId = item.productOptionId,
                    quantity = item.quantity,
                    orderId = payload.orderId.toString(),
                )
            }

            // 재고 예약 성공 이벤트 발행
            outboxService.save(
                aggregateType = "Inventory",
                aggregateId = payload.orderId.toString(),
                eventType = "InventoryReserved",
                payload = InventoryReservedPayload(
                    orderId = payload.orderId,
                    sagaId = payload.sagaId,
                ),
            )

            logger.info { "재고 예약 성공: orderId=${payload.orderId}, sagaId=${payload.sagaId}" }
        } catch (e: Exception) {
            logger.error(e) { "재고 예약 실패: orderId=${payload.orderId}, sagaId=${payload.sagaId}" }

            // 재고 예약 실패 이벤트 발행
            outboxService.save(
                aggregateType = "Inventory",
                aggregateId = payload.orderId.toString(),
                eventType = "InventoryReserveFailed",
                payload = InventoryFailedPayload(
                    orderId = payload.orderId,
                    sagaId = payload.sagaId,
                    reason = e.message,
                ),
            )
        }
    }

    private fun handleInventoryRelease(event: DomainEvent) {
        val payload = objectMapper.readValue(event.payload, InventoryReleasePayload::class.java)

        try {
            // 보상: 예약된 재고 해제
            payload.items.forEach { item ->
                inventoryService.releaseStock(
                    productOptionId = item.productOptionId,
                    quantity = item.quantity,
                    orderId = payload.orderId.toString(),
                )
            }

            logger.info { "재고 해제 완료 (보상): orderId=${payload.orderId}, sagaId=${payload.sagaId}" }
        } catch (e: Exception) {
            logger.error(e) { "재고 해제 실패 (보상): orderId=${payload.orderId}, sagaId=${payload.sagaId}" }
        }
    }
}
