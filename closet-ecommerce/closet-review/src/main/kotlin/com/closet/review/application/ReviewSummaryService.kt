package com.closet.review.application

import com.closet.common.event.ClosetTopics
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewSummary
import com.closet.review.domain.ReviewSummaryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 집계 서비스 (CP-25).
 *
 * 리뷰 생성/삭제 시 ReviewSummary를 갱신하고,
 * review.summary.updated Kafka 이벤트를 발행하여 search-service에 전파한다.
 */
@Service
@Transactional
class ReviewSummaryService(
    private val reviewSummaryRepository: ReviewSummaryRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {

    /**
     * 리뷰 생성 시 집계 갱신.
     */
    fun onReviewCreated(review: Review) {
        val summary = getOrCreateSummary(review.productId)
        summary.addReview(
            rating = review.rating,
            fitType = review.fitType,
            isPhotoReview = review.isPhotoReview(),
        )
        reviewSummaryRepository.save(summary)
        publishSummaryUpdatedEvent(summary)

        logger.info { "리뷰 집계 갱신 (추가): productId=${review.productId}, totalCount=${summary.totalCount}, avgRating=${summary.avgRating}" }
    }

    /**
     * 리뷰 삭제 시 집계 갱신.
     */
    fun onReviewDeleted(review: Review) {
        val summary = reviewSummaryRepository.findByProductId(review.productId) ?: return
        summary.removeReview(
            rating = review.rating,
            fitType = review.fitType,
            isPhotoReview = review.isPhotoReview(),
        )
        reviewSummaryRepository.save(summary)
        publishSummaryUpdatedEvent(summary)

        logger.info { "리뷰 집계 갱신 (삭제): productId=${review.productId}, totalCount=${summary.totalCount}, avgRating=${summary.avgRating}" }
    }

    /**
     * 상품별 리뷰 집계 조회 (CP-25).
     */
    @Transactional(readOnly = true)
    fun getSummary(productId: Long): ReviewSummaryResponse? {
        val summary = reviewSummaryRepository.findByProductId(productId) ?: return null
        return ReviewSummaryResponse.from(summary)
    }

    private fun getOrCreateSummary(productId: Long): ReviewSummary {
        return reviewSummaryRepository.findByProductId(productId)
            ?: reviewSummaryRepository.save(ReviewSummary.create(productId))
    }

    private fun publishSummaryUpdatedEvent(summary: ReviewSummary) {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "productId" to summary.productId,
                "reviewCount" to summary.totalCount,
                "avgRating" to summary.avgRating,
                "timestamp" to ZonedDateTime.now().toString(),
            )
        )

        outboxEventPublisher.publish(
            aggregateType = "ReviewSummary",
            aggregateId = summary.productId.toString(),
            eventType = "ReviewSummaryUpdated",
            topic = ClosetTopics.REVIEW,
            partitionKey = summary.productId.toString(),
            payload = payload,
        )
    }
}
