package com.closet.review.domain.repository

import com.closet.review.domain.entity.Review
import com.closet.review.domain.enums.ReviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository : JpaRepository<Review, Long> {

    fun existsByOrderItemId(orderItemId: Long): Boolean

    fun findByProductIdAndStatus(productId: Long, status: ReviewStatus, pageable: Pageable): Page<Review>

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = :status")
    fun findAverageRatingByProductId(productId: Long, status: ReviewStatus = ReviewStatus.ACTIVE): Double?

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = :status")
    fun countByProductIdAndStatus(productId: Long, status: ReviewStatus = ReviewStatus.ACTIVE): Long

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = :status GROUP BY r.rating")
    fun findRatingDistribution(productId: Long, status: ReviewStatus = ReviewStatus.ACTIVE): List<Array<Any>>

    @Query("SELECT r.sizeFeeling, COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = :status AND r.sizeFeeling IS NOT NULL GROUP BY r.sizeFeeling")
    fun findSizeFeelingDistribution(productId: Long, status: ReviewStatus = ReviewStatus.ACTIVE): List<Array<Any>>
}
