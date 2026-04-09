package com.closet.review.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewSummaryRepository : JpaRepository<ReviewSummary, Long> {
    fun findByProductId(productId: Long): ReviewSummary?
}
