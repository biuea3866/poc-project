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
 * review.deleted Kafka Consumer (CP-27, PD-36).
 *
 * 리뷰 삭제 시 적립 포인트를 회수한다.
 * 잔액 부족 시 마이너스 잔액 허용 (다음 적립에서 상계).
 */
@Component
@ConditionalOnProperty(name = ["feature.review-point-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewDeletedConsumer(
    private val pointService: PointService,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val TOPIC = "review.deleted"
        private const val CONSUMER_GROUP = "member-service"
    }

    data class ReviewDeletedPayload(
        val eventId: String,
        val reviewId: Long,
        val productId: Long,
        val memberId: Long,
        val pointAmount: Int,
    )

    @KafkaListener(topics = [TOPIC], groupId = CONSUMER_GROUP)
    fun consume(record: ConsumerRecord<String, String>) {
        val payload = try {
            objectMapper.readValue(record.value(), ReviewDeletedPayload::class.java)
        } catch (e: Exception) {
            logger.error(e) { "review.deleted 메시지 파싱 실패: ${record.value()}" }
            return
        }

        logger.info { "review.deleted 수신: reviewId=${payload.reviewId}, memberId=${payload.memberId}, pointAmount=${payload.pointAmount}" }

        try {
            pointService.revokeReviewPoint(
                memberId = payload.memberId,
                reviewId = payload.reviewId,
                amount = payload.pointAmount,
            )
            logger.info { "리뷰 포인트 회수 처리 완료: reviewId=${payload.reviewId}" }
        } catch (e: Exception) {
            logger.error(e) { "리뷰 포인트 회수 실패: reviewId=${payload.reviewId}, memberId=${payload.memberId}" }
            throw e
        }
    }
}
