package com.closet.review.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ReviewImageRepository : JpaRepository<ReviewImage, Long> {

    fun findByReviewIdOrderByDisplayOrderAsc(reviewId: Long): List<ReviewImage>
}
