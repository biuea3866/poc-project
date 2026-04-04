package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * product.deleted Kafka Consumer.
 *
 * closet-product가 발행한 상품 삭제 이벤트를 수신하여 ES 문서를 삭제한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ProductDeletedConsumer(
    private val productSearchService: ProductSearchService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "product.deleted"
        private const val CONSUMER_GROUP = "search-service"
    }

    data class ProductDeletedPayload(
        val productId: Long,
        val name: String,
        val brandId: Long,
        val categoryId: Long,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ProductDeletedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "product.deleted 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "product.deleted 수신: productId=${payload.productId}, name=${payload.name}" }

        try {
            productSearchService.deleteProduct(payload.productId)
        } catch (e: Exception) {
            logger.error(e) { "product.deleted ES 삭제 실패: productId=${payload.productId}" }
            throw e
        }
    }
}
