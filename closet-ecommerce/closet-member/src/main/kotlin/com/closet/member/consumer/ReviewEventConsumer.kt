package com.closet.member.consumer

import com.closet.common.event.ClosetTopics
import com.closet.member.application.PointService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.review 토픽 Consumer (CP-27).
 *
 * 리뷰 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ReviewCreated: 포인트 적립 (텍스트 200P, 포토 500P, 일일 한도 5,000P PD-37)
 * - ReviewDeleted: 포인트 회수 (잔액 부족 시 마이너스 허용, PD-36)
 */
@Component
@ConditionalOnProperty(name = ["feature.review-point-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewEventConsumer(
    private val pointService: PointService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val CONSUMER_GROUP = "member-service"
    }

    data class ReviewEventEnvelope(
        val eventType: String,
        val eventId: String? = null,
        val reviewId: Long? = null,
        val productId: Long? = null,
        val memberId: Long? = null,
        val rating: Int? = null,
        val isPhotoReview: Boolean? = null,
        val pointAmount: Int? = null,
    )

    @KafkaListener(topics = [ClosetTopics.REVIEW], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val envelope = try {
            objectMapper.readValue(record.value(), ReviewEventEnvelope::class.java)
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "${ClosetTopics.REVIEW} 수신: eventType=${envelope.eventType}" }

        try {
            when (envelope.eventType) {
                "ReviewCreated" -> handleReviewCreated(envelope)
                "ReviewDeleted" -> handleReviewDeleted(envelope)
                else -> logger.info { "처리하지 않는 eventType 무시: ${envelope.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 처리 실패: eventType=${envelope.eventType}" }
            throw e
        }
    }

    private fun handleReviewCreated(envelope: ReviewEventEnvelope) {
        val memberId = envelope.memberId ?: return
        val reviewId = envelope.reviewId ?: return
        val pointAmount = envelope.pointAmount ?: return

        logger.info { "ReviewCreated: reviewId=$reviewId, memberId=$memberId, pointAmount=$pointAmount" }

        val earned = pointService.earnReviewPoint(
            memberId = memberId,
            reviewId = reviewId,
            amount = pointAmount,
        )
        logger.info { "리뷰 포인트 적립 처리 완료: reviewId=$reviewId, earned=$earned" }
    }

    private fun handleReviewDeleted(envelope: ReviewEventEnvelope) {
        val memberId = envelope.memberId ?: return
        val reviewId = envelope.reviewId ?: return
        val pointAmount = envelope.pointAmount ?: return

        logger.info { "ReviewDeleted: reviewId=$reviewId, memberId=$memberId, pointAmount=$pointAmount" }

        pointService.revokeReviewPoint(
            memberId = memberId,
            reviewId = reviewId,
            amount = pointAmount,
        )
        logger.info { "리뷰 포인트 회수 처리 완료: reviewId=$reviewId" }
    }
}
