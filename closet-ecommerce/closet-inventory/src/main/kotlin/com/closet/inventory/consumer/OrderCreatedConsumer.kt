package com.closet.inventory.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.inventory.application.InventoryService
import com.closet.inventory.application.ReserveItemRequest
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["feature.inventory-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderCreatedConsumer(
    private val inventoryService: InventoryService,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "order.created"
        private const val CONSUMER_GROUP = "inventory-service"
    }

    data class OrderCreatedPayload(
        val orderId: Long,
        val memberId: Long,
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
            objectMapper.readValue(record.value(), OrderCreatedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "order.created 메시지 파싱 실패: ${record.value()}" }
            return
        }

        val eventId = "order-created-${payload.orderId}"
        logger.info { "order.created 수신: orderId=${payload.orderId}, items=${payload.items.size}" }

        idempotencyChecker.process(eventId, TOPIC, CONSUMER_GROUP) {
            val reserveItems = payload.items.map {
                ReserveItemRequest(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            }

            inventoryService.reserveAll(payload.orderId, reserveItems)
        }
    }
}
