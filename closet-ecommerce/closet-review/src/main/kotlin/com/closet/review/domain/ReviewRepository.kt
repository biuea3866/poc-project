package com.closet.review.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long>, ReviewCustomRepository {
    fun findByProductIdAndStatusOrderByCreatedAtDesc(
        productId: Long,
        status: ReviewStatus,
        pageable: Pageable,
    ): Page<Review>

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
}
