package com.closet.review.application

import com.closet.common.event.ClosetTopics
import com.closet.common.outbox.OutboxEventPublisher
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewSummary
import com.closet.review.domain.ReviewSummaryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 집계 서비스 (US-804).
 *
 * 리뷰 생성/수정/삭제 시 ReviewSummary를 갱신하고,
 * Redis 캐시(review_summary:{productId})를 갱신하며,
 * review.summary.updated Kafka 이벤트를 발행하여 ES(search-service)에 전파한다.
 */
@Service
@Transactional
class ReviewSummaryService(
    private val reviewSummaryRepository: ReviewSummaryRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate,
) {

    companion object {
        private const val CACHE_KEY_PREFIX = "review_summary:"
        private val CACHE_TTL = Duration.ofHours(24)
    }

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
        updateCache(summary)
        publishSummaryUpdatedEvent(summary)

        logger.info { "리뷰 집계 갱신 (추가): productId=${review.productId}, totalCount=${summary.totalCount}, avgRating=${summary.avgRating}" }
    }

    /**
     * 리뷰 수정 시 집계 갱신.
     * 포토 리뷰 여부 변경을 반영하기 위해 전체 재계산은 하지 않고,
     * 현재 이벤트만 전파한다 (캐시 무효화).
     */
    fun onReviewUpdated(review: Review) {
        val summary = reviewSummaryRepository.findByProductId(review.productId) ?: return
        evictCache(review.productId)
        publishSummaryUpdatedEvent(summary)

        logger.info { "리뷰 집계 캐시 무효화 (수정): productId=${review.productId}" }
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
        updateCache(summary)
        publishSummaryUpdatedEvent(summary)

        logger.info { "리뷰 집계 갱신 (삭제): productId=${review.productId}, totalCount=${summary.totalCount}, avgRating=${summary.avgRating}" }
    }

    /**
     * 상품별 리뷰 집계 조회 (US-804).
     * Redis 캐시 우선, 미스 시 DB에서 조회 후 캐시에 저장.
     */
    @Transactional(readOnly = true)
    fun getSummary(productId: Long): ReviewSummaryResponse? {
        // Redis 캐시 조회
        val cacheKey = "$CACHE_KEY_PREFIX$productId"
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return try {
                objectMapper.readValue(cached, ReviewSummaryResponse::class.java)
            } catch (e: Exception) {
                logger.warn(e) { "Redis 캐시 역직렬화 실패, DB에서 조회: productId=$productId" }
                null
            }
        }

        // DB 조회
        val summary = reviewSummaryRepository.findByProductId(productId) ?: return null
        val response = ReviewSummaryResponse.from(summary)

        // 캐시 저장
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL)
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 저장 실패: productId=$productId" }
        }

        return response
    }

    private fun getOrCreateSummary(productId: Long): ReviewSummary {
        return reviewSummaryRepository.findByProductId(productId)
            ?: reviewSummaryRepository.save(ReviewSummary.create(productId))
    }

    private fun updateCache(summary: ReviewSummary) {
        val cacheKey = "$CACHE_KEY_PREFIX${summary.productId}"
        try {
            val response = ReviewSummaryResponse.from(summary)
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL)
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 갱신 실패: productId=${summary.productId}" }
        }
    }

    private fun evictCache(productId: Long) {
        val cacheKey = "$CACHE_KEY_PREFIX$productId"
        try {
            redisTemplate.delete(cacheKey)
        } catch (e: Exception) {
            logger.warn(e) { "Redis 캐시 삭제 실패: productId=$productId" }
        }
    }

    /**
     * review.summary.updated 이벤트 발행 (US-804).
     * ES 동기화용. 별점 분포, 사이즈핏 분포 포함.
     */
    private fun publishSummaryUpdatedEvent(summary: ReviewSummary) {
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "eventType" to "ReviewSummaryUpdated",
                "productId" to summary.productId,
                "reviewCount" to summary.totalCount,
                "avgRating" to summary.avgRating,
                "ratingDistribution" to mapOf(
                    1 to summary.rating1Count,
                    2 to summary.rating2Count,
                    3 to summary.rating3Count,
                    4 to summary.rating4Count,
                    5 to summary.rating5Count,
                ),
                "fitDistribution" to mapOf(
                    "SMALL" to summary.fitSmallCount,
                    "PERFECT" to summary.fitPerfectCount,
                    "LARGE" to summary.fitLargeCount,
                ),
                "photoReviewCount" to summary.photoReviewCount,
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
