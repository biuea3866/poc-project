package com.closet.review.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReviewCustomRepository {
    fun findByProductIdLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdPhotoOnlyLatest(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdPhotoOnlyRating(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findByProductIdPhotoOnlyHelpful(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

    fun findBySimilarBody(
        productId: Long,
        status: ReviewStatus,
        minHeight: Int,
        maxHeight: Int,
        minWeight: Int,
        maxWeight: Int,
        pageable: Pageable,
    ): Page<Review>
}
