package com.closet.review.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewHelpfulRepository : JpaRepository<ReviewHelpful, Long> {
    fun existsByReviewIdAndMemberId(
        reviewId: Long,
        memberId: Long,
    ): Boolean
}
