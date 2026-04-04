package com.closet.inventory.consumer

import com.closet.common.idempotency.IdempotencyChecker
import com.closet.inventory.application.InventoryService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(name = ["feature.inventory-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class ReturnApprovedConsumer(
    private val inventoryService: InventoryService,
    private val idempotencyChecker: IdempotencyChecker,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "return.approved"
        private const val CONSUMER_GROUP = "inventory-service"
    }

    data class ReturnApprovedPayload(
        val orderId: Long,
        val items: List<ReturnItemInfo>,
    ) {
        data class ReturnItemInfo(
            val productOptionId: Long,
            val quantity: Int,
        )
    }

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReturnApprovedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "return.approved 메시지 파싱 실패: ${record.value()}" }
            return
        }

        val eventId = "return-approved-${payload.orderId}"
        logger.info { "return.approved 수신: orderId=${payload.orderId}, items=${payload.items.size}" }

        idempotencyChecker.process(eventId, TOPIC, CONSUMER_GROUP) {
            for (item in payload.items) {
                inventoryService.returnRestore(
                    productOptionId = item.productOptionId,
                    quantity = item.quantity,
                    orderId = payload.orderId,
                )
            }
        }
    }
}
