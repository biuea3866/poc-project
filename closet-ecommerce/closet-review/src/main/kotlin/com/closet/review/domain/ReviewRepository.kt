package com.closet.review.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByProductIdAndStatusInOrderByCreatedAtDesc(
        productId: Long,
        statuses: Collection<ReviewStatus>,
        pageable: Pageable,
    ): Page<Review>

    fun findByMemberIdAndStatusNotOrderByCreatedAtDesc(
        memberId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun existsByOrderItemIdAndMemberIdAndStatusNot(
        orderItemId: Long,
        memberId: Long,
        status: ReviewStatus,
    ): Boolean

    fun findByIdAndMemberId(
        id: Long,
        memberId: Long,
    ): Review?

    fun countByProductIdAndStatus(
        productId: Long,
        status: ReviewStatus,
    ): Long

    fun findByProductIdLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusOrderByCreatedAtDesc(productId, status, pageable)

    fun findByProductIdAndStatusOrderByCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusOrderByRatingDescCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusOrderByHelpfulCountDescCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusAndHasImageTrueOrderByCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusAndHasImageTrueOrderByRatingDescCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusAndHasImageTrueOrderByHelpfulCountDescCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdAndStatusAndHeightIsNotNullAndWeightIsNotNullAndHeightBetweenAndWeightBetweenOrderByCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        minHeight: Int,
        maxHeight: Int,
        minWeight: Int,
        maxWeight: Int,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusOrderByRatingDescCreatedAtDesc(productId, status, pageable)

    fun findByProductIdHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusOrderByHelpfulCountDescCreatedAtDesc(productId, status, pageable)

    fun findByProductIdPhotoOnlyLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusAndHasImageTrueOrderByCreatedAtDesc(productId, status, pageable)

    fun findByProductIdPhotoOnlyRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusAndHasImageTrueOrderByRatingDescCreatedAtDesc(productId, status, pageable)

    fun findByProductIdPhotoOnlyHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review> = findByProductIdAndStatusAndHasImageTrueOrderByHelpfulCountDescCreatedAtDesc(productId, status, pageable)

    fun findBySimilarBody(
        productId: Long,
        status: ReviewStatus,
        minHeight: Int,
        maxHeight: Int,
        minWeight: Int,
        maxWeight: Int,
        pageable: Pageable,
    ): Page<Review> =
        findByProductIdAndStatusAndHeightIsNotNullAndWeightIsNotNullAndHeightBetweenAndWeightBetweenOrderByCreatedAtDesc(
            productId = productId,
            status = status,
            minHeight = minHeight,
            maxHeight = maxHeight,
            minWeight = minWeight,
            maxWeight = maxWeight,
            pageable = pageable,
        )
}
