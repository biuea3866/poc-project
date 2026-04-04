package com.closet.review.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewEditHistoryRepository : JpaRepository<ReviewEditHistory, Long> {

    fun findByReviewIdOrderByCreatedAtDesc(reviewId: Long): List<ReviewEditHistory>
}
