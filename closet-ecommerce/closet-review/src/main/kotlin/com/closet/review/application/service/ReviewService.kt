package com.closet.review.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.review.application.dto.CreateReviewRequest
import com.closet.review.application.dto.ReviewResponse
import com.closet.review.application.dto.ReviewSummaryResponse
import com.closet.review.application.dto.UpdateReviewRequest
import com.closet.review.domain.entity.Review
import com.closet.review.domain.enums.ReviewStatus
import com.closet.review.domain.enums.SizeFeeling
import com.closet.review.domain.repository.ReviewRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository
) {

    @Transactional
    fun createReview(memberId: Long, request: CreateReviewRequest): ReviewResponse {
        if (reviewRepository.existsByOrderItemId(request.orderItemId)) {
            throw BusinessException(ErrorCode.DUPLICATE_ENTITY, "이미 해당 주문 상품에 대한 리뷰가 존재합니다")
        }

        val review = Review.create(
            productId = request.productId,
            orderItemId = request.orderItemId,
            memberId = memberId,
            rating = request.rating,
            content = request.content,
            height = request.height,
            weight = request.weight,
            sizeFeeling = request.sizeFeeling,
            imageUrls = request.imageUrls
        )

        val saved = reviewRepository.save(review)
        logger.info { "리뷰 생성 완료: id=${saved.id}, productId=${saved.productId}, memberId=$memberId" }
        return ReviewResponse.from(saved)
    }

    @Transactional
    fun updateReview(reviewId: Long, memberId: Long, request: UpdateReviewRequest): ReviewResponse {
        val review = findReviewById(reviewId)
        review.validateOwner(memberId)
        review.update(content = request.content, rating = request.rating)
        logger.info { "리뷰 수정 완료: id=$reviewId, memberId=$memberId" }
        return ReviewResponse.from(review)
    }

    @Transactional
    fun deleteReview(reviewId: Long, memberId: Long) {
        val review = findReviewById(reviewId)
        review.validateOwner(memberId)
        review.delete()
        logger.info { "리뷰 삭제 완료: id=$reviewId, memberId=$memberId" }
    }

    fun getReviewsByProductId(productId: Long, page: Int, size: Int, sort: String): Page<ReviewResponse> {
        val sortOrder = when (sort) {
            "helpful" -> Sort.by(Sort.Direction.DESC, "helpfulCount")
            "rating_high" -> Sort.by(Sort.Direction.DESC, "rating")
            "rating_low" -> Sort.by(Sort.Direction.ASC, "rating")
            else -> Sort.by(Sort.Direction.DESC, "createdAt") // newest
        }
        val pageable = PageRequest.of(page, size, sortOrder)
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.ACTIVE, pageable)
            .map { ReviewResponse.from(it) }
    }

    fun getReviewSummary(productId: Long): ReviewSummaryResponse {
        val averageRating = reviewRepository.findAverageRatingByProductId(productId) ?: 0.0
        val totalCount = reviewRepository.countByProductIdAndStatus(productId)

        val ratingDistribution = reviewRepository.findRatingDistribution(productId)
            .associate { (it[0] as Int) to (it[1] as Long) }

        val sizeFeelingDistribution = reviewRepository.findSizeFeelingDistribution(productId)
            .associate { (it[0] as SizeFeeling) to (it[1] as Long) }

        return ReviewSummaryResponse(
            averageRating = averageRating,
            totalCount = totalCount,
            ratingDistribution = ratingDistribution,
            sizeFeelingDistribution = sizeFeelingDistribution
        )
    }

    @Transactional
    fun markHelpful(reviewId: Long) {
        val review = findReviewById(reviewId)
        review.markHelpful()
        logger.info { "리뷰 도움돼요: id=$reviewId, helpfulCount=${review.helpfulCount}" }
    }

    private fun findReviewById(id: Long): Review {
        return reviewRepository.findById(id)
            .filter { !it.isDeleted() }
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰를 찾을 수 없습니다: $id") }
    }
}
