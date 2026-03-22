package com.closet.review.application.dto

import com.closet.review.domain.entity.Review
import com.closet.review.domain.enums.SizeFeeling
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

// ===== Request =====

data class CreateReviewRequest(
    @field:NotNull(message = "상품 ID는 필수입니다")
    val productId: Long,
    @field:NotNull(message = "주문 상품 ID는 필수입니다")
    val orderItemId: Long,
    @field:Min(1, message = "별점은 1 이상이어야 합니다")
    @field:Max(5, message = "별점은 5 이하여야 합니다")
    val rating: Int,
    @field:NotBlank(message = "리뷰 내용은 필수입니다")
    val content: String,
    val height: Int? = null,
    val weight: Int? = null,
    val sizeFeeling: SizeFeeling? = null,
    val imageUrls: List<String> = emptyList()
)

data class UpdateReviewRequest(
    @field:Min(1, message = "별점은 1 이상이어야 합니다")
    @field:Max(5, message = "별점은 5 이하여야 합니다")
    val rating: Int,
    @field:NotBlank(message = "리뷰 내용은 필수입니다")
    val content: String
)

// ===== Response =====

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val memberId: Long,
    val rating: Int,
    val content: String,
    val height: Int?,
    val weight: Int?,
    val sizeFeeling: SizeFeeling?,
    val helpfulCount: Int,
    val images: List<ReviewImageResponse>,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(review: Review): ReviewResponse = ReviewResponse(
            id = review.id,
            productId = review.productId,
            memberId = review.memberId,
            rating = review.rating,
            content = review.content,
            height = review.height,
            weight = review.weight,
            sizeFeeling = review.sizeFeeling,
            helpfulCount = review.helpfulCount,
            images = review.images.map { ReviewImageResponse(it.id, it.imageUrl, it.sortOrder) },
            createdAt = review.createdAt
        )
    }
}

data class ReviewImageResponse(
    val id: Long,
    val imageUrl: String,
    val sortOrder: Int
)

data class ReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Long,
    val ratingDistribution: Map<Int, Long>,
    val sizeFeelingDistribution: Map<SizeFeeling, Long>
)
