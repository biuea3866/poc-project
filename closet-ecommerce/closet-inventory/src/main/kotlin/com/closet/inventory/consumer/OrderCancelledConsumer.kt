package com.closet.inventory.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.inventory.application.InventoryService
import com.closet.inventory.application.ReleaseItemRequest
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["feature.inventory-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderCancelledConsumer(
    private val inventoryService: InventoryService,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "order.cancelled"
        private const val CONSUMER_GROUP = "inventory-service"
    }

    data class OrderCancelledPayload(
        val orderId: Long,
        val reason: String,
        val items: List<OrderItemInfo>,
    ) {
        data class OrderItemInfo(
            val productOptionId: Long,
            val quantity: Int,
        )
    }

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), OrderCancelledPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "order.cancelled 메시지 파싱 실패: ${record.value()}" }
            return
        }

        val eventId = "order-cancelled-${payload.orderId}"
        logger.info { "order.cancelled 수신: orderId=${payload.orderId}, reason=${payload.reason}" }

        idempotencyChecker.process(eventId, TOPIC, CONSUMER_GROUP) {
            val releaseItems = payload.items.map {
                ReleaseItemRequest(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            }

            inventoryService.releaseAll(payload.orderId, releaseItems, payload.reason)
        }
    }
}
