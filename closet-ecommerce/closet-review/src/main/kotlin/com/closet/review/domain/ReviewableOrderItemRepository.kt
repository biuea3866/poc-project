package com.closet.review.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewableOrderItemRepository : JpaRepository<ReviewableOrderItem, Long> {
    fun existsByOrderItemIdAndMemberId(
        orderItemId: Long,
        memberId: Long,
    ): Boolean

    fun findByOrderItemId(orderItemId: Long): ReviewableOrderItem?
}
