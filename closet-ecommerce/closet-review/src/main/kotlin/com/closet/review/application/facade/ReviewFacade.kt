package com.closet.review.application.facade

import com.closet.review.application.CreateReviewRequest
import com.closet.review.application.PresignedUploadUrlRequest
import com.closet.review.application.PresignedUploadUrlResponse
import com.closet.review.application.ReviewEditHistoryResponse
import com.closet.review.application.ReviewImageService
import com.closet.review.application.ReviewListQuery
import com.closet.review.application.ReviewResponse
import com.closet.review.application.ReviewService
import com.closet.review.application.ReviewSummaryResponse
import com.closet.review.application.ReviewSummaryService
import com.closet.review.application.UpdateReviewRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * 리뷰 Facade (Controller -> Facade -> Service).
 *
 * Controller에서 직접 Service를 호출하지 않고 Facade를 경유한다.
 * 여러 Service의 조합이 필요한 경우 이 레이어에서 오케스트레이션한다.
 */
@Component
class ReviewFacade(
    private val reviewService: ReviewService,
    private val reviewSummaryService: ReviewSummaryService,
    private val reviewImageService: ReviewImageService,
) {
    fun createReview(
        memberId: Long,
        request: CreateReviewRequest,
    ): ReviewResponse {
        return reviewService.createReview(memberId, request)
    }

    fun updateReview(
        memberId: Long,
        reviewId: Long,
        request: UpdateReviewRequest,
    ): ReviewResponse {
        return reviewService.updateReview(memberId, reviewId, request)
    }

    fun deleteReview(
        memberId: Long,
        reviewId: Long,
    ) {
        reviewService.deleteReview(memberId, reviewId)
    }

    fun getReview(reviewId: Long): ReviewResponse {
        return reviewService.findById(reviewId)
    }

    fun getReviews(query: ReviewListQuery): Page<ReviewResponse> {
        return reviewService.findByProductId(query)
    }

    fun getMyReviews(
        memberId: Long,
        pageable: Pageable,
    ): Page<ReviewResponse> {
        return reviewService.findByMemberId(memberId, pageable)
    }

    fun getEditHistory(reviewId: Long): List<ReviewEditHistoryResponse> {
        return reviewService.getEditHistory(reviewId)
    }

    fun getSummary(productId: Long): ReviewSummaryResponse? {
        return reviewSummaryService.getSummary(productId)
    }

    fun markHelpful(
        memberId: Long,
        reviewId: Long,
    ) {
        reviewService.markHelpful(memberId, reviewId)
    }

    fun getPresignedUploadUrl(
        memberId: Long,
        request: PresignedUploadUrlRequest,
    ): PresignedUploadUrlResponse {
        return reviewImageService.generatePresignedUploadUrl(memberId, request)
    }

    fun hideReview(reviewId: Long) {
        reviewService.hideReview(reviewId)
    }

    fun unhideReview(reviewId: Long) {
        reviewService.unhideReview(reviewId)
    }
}
