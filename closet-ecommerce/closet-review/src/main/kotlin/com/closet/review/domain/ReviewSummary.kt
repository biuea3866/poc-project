package com.closet.review.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 상품별 리뷰 집계 엔티티 (US-804).
 *
 * 리뷰 생성/수정/삭제 시 갱신되며, 별점 분포 + 사이즈핏 분포를 관리한다.
 * Redis 캐시(review_summary:{productId})로 조회 성능을 확보하고,
 * review.summary.updated 이벤트로 search-service(ES)에 전파한다.
 */
@Entity
@Table(name = "review_summary")
@EntityListeners(AuditingEntityListener::class)
class ReviewSummary(
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,
    @Column(name = "total_count", nullable = false)
    var totalCount: Int = 0,
    @Column(name = "avg_rating", nullable = false)
    var avgRating: Double = 0.0,
    @Column(name = "total_rating_sum", nullable = false)
    var totalRatingSum: Long = 0,
    /** 별점 분포 (1~5점 각각의 개수) */
    @Column(name = "rating_1_count", nullable = false) var rating1Count: Int = 0,
    @Column(name = "rating_2_count", nullable = false) var rating2Count: Int = 0,
    @Column(name = "rating_3_count", nullable = false) var rating3Count: Int = 0,
    @Column(name = "rating_4_count", nullable = false) var rating4Count: Int = 0,
    @Column(name = "rating_5_count", nullable = false) var rating5Count: Int = 0,
    /** 사이즈핏 분포 (US-802: SMALL/PERFECT/LARGE) */
    @Column(name = "fit_small_count", nullable = false) var fitSmallCount: Int = 0,
    @Column(name = "fit_perfect_count", nullable = false) var fitPerfectCount: Int = 0,
    @Column(name = "fit_large_count", nullable = false) var fitLargeCount: Int = 0,
    @Column(name = "photo_review_count", nullable = false)
    var photoReviewCount: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    /**
     * 리뷰 추가 시 집계 갱신.
     */
    fun addReview(
        rating: Int,
        fitType: SizeFit?,
        isPhotoReview: Boolean,
    ) {
        totalCount++
        totalRatingSum += rating
        avgRating = totalRatingSum.toDouble() / totalCount
        incrementRatingCount(rating)
        if (fitType != null) incrementFitCount(fitType)
        if (isPhotoReview) photoReviewCount++
    }

    /**
     * 리뷰 삭제 시 집계 갱신.
     */
    fun removeReview(
        rating: Int,
        fitType: SizeFit?,
        isPhotoReview: Boolean,
    ) {
        if (totalCount <= 0) return
        totalCount--
        totalRatingSum -= rating
        avgRating = if (totalCount > 0) totalRatingSum.toDouble() / totalCount else 0.0
        decrementRatingCount(rating)
        if (fitType != null) decrementFitCount(fitType)
        if (isPhotoReview) photoReviewCount = (photoReviewCount - 1).coerceAtLeast(0)
    }

    private fun incrementRatingCount(rating: Int) {
        when (rating) {
            1 -> rating1Count++
            2 -> rating2Count++
            3 -> rating3Count++
            4 -> rating4Count++
            5 -> rating5Count++
        }
    }

    private fun decrementRatingCount(rating: Int) {
        when (rating) {
            1 -> rating1Count = (rating1Count - 1).coerceAtLeast(0)
            2 -> rating2Count = (rating2Count - 1).coerceAtLeast(0)
            3 -> rating3Count = (rating3Count - 1).coerceAtLeast(0)
            4 -> rating4Count = (rating4Count - 1).coerceAtLeast(0)
            5 -> rating5Count = (rating5Count - 1).coerceAtLeast(0)
        }
    }

    private fun incrementFitCount(fitType: SizeFit) {
        when (fitType) {
            SizeFit.SMALL -> fitSmallCount++
            SizeFit.PERFECT -> fitPerfectCount++
            SizeFit.LARGE -> fitLargeCount++
        }
    }

    private fun decrementFitCount(fitType: SizeFit) {
        when (fitType) {
            SizeFit.SMALL -> fitSmallCount = (fitSmallCount - 1).coerceAtLeast(0)
            SizeFit.PERFECT -> fitPerfectCount = (fitPerfectCount - 1).coerceAtLeast(0)
            SizeFit.LARGE -> fitLargeCount = (fitLargeCount - 1).coerceAtLeast(0)
        }
    }

    companion object {
        fun create(productId: Long): ReviewSummary {
            return ReviewSummary(productId = productId)
        }
    }
}
