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
 * review.summary.updated Kafka Consumer.
 *
 * closet-review가 발행한 리뷰 집계 변경 이벤트를 수신하여
 * ES 문서의 reviewCount, avgRating 필드를 부분 업데이트한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewSummaryConsumer(
    private val productSearchService: ProductSearchService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "review.summary.updated"
        private const val CONSUMER_GROUP = "search-service"
    }

    data class ReviewSummaryPayload(
        val productId: Long,
        val reviewCount: Int,
        val avgRating: Double,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReviewSummaryPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "review.summary.updated 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "review.summary.updated 수신: productId=${payload.productId}, reviewCount=${payload.reviewCount}, avgRating=${payload.avgRating}" }

        try {
            productSearchService.updateReviewSummary(
                productId = payload.productId,
                reviewCount = payload.reviewCount,
                avgRating = payload.avgRating,
            )
        } catch (e: Exception) {
            logger.error(e) { "review.summary.updated ES 업데이트 실패: productId=${payload.productId}" }
            throw e
        }
    }
}
