package com.closet.search.consumer

import com.closet.common.event.ClosetTopics
import com.closet.search.application.service.ProductSearchService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.review 토픽 Consumer.
 *
 * 리뷰 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ReviewSummaryUpdated: ES 문서의 reviewCount, avgRating 부분 업데이트
 *
 * SEARCH_INDEXING_ENABLED Feature Flag로 활성화/비활성화를 제어한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewEventConsumer(
    private val productSearchService: ProductSearchService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "search-service"
    }

    data class ReviewEventEnvelope(
        val eventType: String,
        val productId: Long = 0L,
        val reviewCount: Int = 0,
        val avgRating: Double = 0.0,
    )

    @KafkaListener(topics = [ClosetTopics.REVIEW], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReviewEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "${ClosetTopics.REVIEW} 수신: eventType=${payload.eventType}, productId=${payload.productId}" }

        try {
            when (payload.eventType) {
                "ReviewSummaryUpdated" -> productSearchService.updateReviewSummary(
                    productId = payload.productId,
                    reviewCount = payload.reviewCount,
                    avgRating = payload.avgRating,
                )

                else -> logger.info { "처리하지 않는 eventType 무시: ${payload.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 처리 실패: eventType=${payload.eventType}, productId=${payload.productId}" }
            throw e
        }
    }
}
