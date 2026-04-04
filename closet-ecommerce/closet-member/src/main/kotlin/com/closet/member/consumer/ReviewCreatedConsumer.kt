package com.closet.member.consumer

import com.closet.member.application.PointService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * review.created Kafka Consumer (CP-27).
 *
 * 리뷰 작성 시 포인트를 적립한다.
 * - 텍스트 리뷰: 200P
 * - 포토 리뷰: 500P
 * - 일일 한도: 5,000P (PD-37)
 */
@Component
@ConditionalOnProperty(name = ["feature.review-point-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewCreatedConsumer(
    private val pointService: PointService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "review.created"
        private const val CONSUMER_GROUP = "member-service"
    }

    data class ReviewCreatedPayload(
        val eventId: String,
        val reviewId: Long,
        val productId: Long,
        val memberId: Long,
        val rating: Int,
        val isPhotoReview: Boolean,
        val pointAmount: Int,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReviewCreatedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "review.created 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "review.created 수신: reviewId=${payload.reviewId}, memberId=${payload.memberId}, pointAmount=${payload.pointAmount}" }

        try {
            val earned = pointService.earnReviewPoint(
                memberId = payload.memberId,
                reviewId = payload.reviewId,
                amount = payload.pointAmount,
            )
            logger.info { "리뷰 포인트 적립 처리 완료: reviewId=${payload.reviewId}, earned=$earned" }
        } catch (e: Exception) {
            logger.error(e) { "리뷰 포인트 적립 실패: reviewId=${payload.reviewId}, memberId=${payload.memberId}" }
            throw e
        }
    }
}
