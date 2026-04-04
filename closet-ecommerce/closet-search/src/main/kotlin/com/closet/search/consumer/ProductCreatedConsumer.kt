package com.closet.search.consumer

import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

/**
 * product.created Kafka Consumer.
 *
 * closet-product가 발행한 상품 생성 이벤트를 수신하여 ES에 인덱싱한다.
 * SEARCH_INDEXING_ENABLED Feature Flag로 활성화/비활성화를 제어한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ProductCreatedConsumer(
    private val productSearchService: ProductSearchService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "product.created"
        private const val CONSUMER_GROUP = "search-service"
    }

    data class ProductCreatedPayload(
        val productId: Long,
        val name: String,
        val description: String,
        val brandId: Long,
        val categoryId: Long,
        val basePrice: BigDecimal,
        val salePrice: BigDecimal,
        val discountRate: Int,
        val status: String,
        val season: String?,
        val fitType: String?,
        val gender: String?,
        val sizes: List<String>,
        val colors: List<String>,
        val imageUrl: String?,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ProductCreatedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "product.created 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "product.created 수신: productId=${payload.productId}, name=${payload.name}" }

        try {
            productSearchService.indexProduct(
                productId = payload.productId,
                name = payload.name,
                description = payload.description,
                brandId = payload.brandId,
                categoryId = payload.categoryId,
                basePrice = payload.basePrice,
                salePrice = payload.salePrice,
                discountRate = payload.discountRate,
                status = payload.status,
                season = payload.season,
                fitType = payload.fitType,
                gender = payload.gender,
                sizes = payload.sizes,
                colors = payload.colors,
                imageUrl = payload.imageUrl,
            )
        } catch (e: Exception) {
            logger.error(e) { "product.created ES 인덱싱 실패: productId=${payload.productId}" }
            throw e // DLQ로 전달하기 위해 재throw
        }
    }
}
