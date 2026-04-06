package com.closet.member.consumer

import com.closet.common.event.ClosetTopics
import com.closet.member.application.PointService
import com.closet.member.consumer.event.ReviewEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.review 토픽 Consumer (CP-27).
 *
 * 리뷰 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ReviewCreated: 포인트 적립 (텍스트 100P, 포토 300P, 사이즈정보 +50P, 최대 350P, 일일 한도 5,000P)
 * - ReviewDeleted: 포인트 회수 (잔액 부족 시 마이너스 허용)
 */
@Component
@ConditionalOnProperty(name = ["feature.review-point-enabled"], havingValue = "true", matchIfMissing = true)
class ReviewEventConsumer(
    private val pointService: PointService,
) {
    @KafkaListener(topics = [ClosetTopics.REVIEW], groupId = "member-service")
    fun handle(event: ReviewEvent) {
        logger.info { "${ClosetTopics.REVIEW} 수신: eventType=${event.eventType}" }

        try {
            when (event.eventType) {
                "ReviewCreated" -> {
                    val created = event.toReviewCreatedEvent()
                    logger.info { "ReviewCreated: reviewId=${created.reviewId}, memberId=${created.memberId}" }

                    val earned =
                        pointService.earnReviewPoint(
                            memberId = created.memberId,
                            reviewId = created.reviewId,
                            amount = created.pointAmount,
                        )
                    logger.info { "리뷰 포인트 적립 처리 완료: reviewId=${created.reviewId}, earned=$earned" }
                }
                "ReviewDeleted" -> {
                    val deleted = event.toReviewDeletedEvent()
                    logger.info { "ReviewDeleted: reviewId=${deleted.reviewId}, memberId=${deleted.memberId}" }

                    pointService.revokeReviewPoint(
                        memberId = deleted.memberId,
                        reviewId = deleted.reviewId,
                        amount = deleted.pointAmount,
                    )
                    logger.info { "리뷰 포인트 회수 처리 완료: reviewId=${deleted.reviewId}" }
                }
                else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.REVIEW} 처리 실패: eventType=${event.eventType}" }
            throw e
        }
    }
}
