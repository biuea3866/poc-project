package com.closet.search.consumer

import com.closet.common.event.ClosetTopics
import com.closet.search.application.facade.SearchFacade
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

/**
 * event.closet.product 토픽 Consumer.
 *
 * 상품 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ProductCreated: ES 인덱싱
 * - ProductUpdated: ES 문서 갱신
 * - ProductDeleted: ES 문서 삭제
 *
 * Consumer는 이벤트 수신 및 라우팅만 담당하고,
 * 실제 비즈니스 로직은 SearchFacade에 위임한다.
 *
 * SEARCH_INDEXING_ENABLED Feature Flag로 활성화/비활성화를 제어한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ProductEventConsumer(
    private val searchFacade: SearchFacade,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "search-service"
    }

    data class ProductEventEnvelope(
        val eventType: String,
        val productId: Long,
        val name: String = "",
        val description: String = "",
        val brandId: Long = 0L,
        val categoryId: Long = 0L,
        val basePrice: BigDecimal = BigDecimal.ZERO,
        val salePrice: BigDecimal = BigDecimal.ZERO,
        val discountRate: Int = 0,
        val status: String = "",
        val season: String? = null,
        val fitType: String? = null,
        val gender: String? = null,
        val sizes: List<String> = emptyList(),
        val colors: List<String> = emptyList(),
        val imageUrl: String? = null,
    )

    @KafkaListener(topics = [ClosetTopics.PRODUCT], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ProductEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.PRODUCT} 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "${ClosetTopics.PRODUCT} 수신: eventType=${payload.eventType}, productId=${payload.productId}" }

        try {
            when (payload.eventType) {
                "ProductCreated" -> searchFacade.handleProductCreated(
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

                "ProductUpdated" -> searchFacade.handleProductUpdated(
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

                "ProductDeleted" -> searchFacade.handleProductDeleted(payload.productId)

                else -> logger.info { "처리하지 않는 eventType 무시: ${payload.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.PRODUCT} 처리 실패: eventType=${payload.eventType}, productId=${payload.productId}" }
            throw e // DLQ로 전달하기 위해 재throw
        }
    }
}
