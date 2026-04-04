package com.closet.review.application

import com.closet.review.domain.FitType
import com.closet.review.domain.Review
import com.closet.review.domain.ReviewEditHistory
import com.closet.review.domain.ReviewSummary
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// === Requests ===

data class CreateReviewRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val orderItemId: Long,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(max = 2000) val content: String,
    val height: Int? = null,
    val weight: Int? = null,
    val purchasedSize: String? = null,
    val fitType: FitType? = null,
)

data class UpdateReviewRequest(
    @field:NotBlank @field:Size(max = 2000) val content: String,
)

// === Responses ===

data class ReviewResponse(
    val id: Long,
    val productId: Long,
    val orderItemId: Long,
    val memberId: Long,
    val rating: Int,
    val content: String,
    val status: String,
    val editCount: Int,
    val hasImage: Boolean,
    val images: List<ReviewImageResponse>,
    val height: Int?,
    val weight: Int?,
    val purchasedSize: String?,
    val fitType: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(review: Review): ReviewResponse {
            return ReviewResponse(
                id = review.id,
                productId = review.productId,
                orderItemId = review.orderItemId,
                memberId = review.memberId,
                rating = review.rating,
                content = review.content,
                status = review.status.name,
                editCount = review.editCount,
                hasImage = review.hasImage,
                images = review.images.map { ReviewImageResponse(it.id, it.imageUrl, it.thumbnailUrl, it.displayOrder) },
                height = review.height,
                weight = review.weight,
                purchasedSize = review.purchasedSize,
                fitType = review.fitType?.name,
                createdAt = if (review.id != 0L) review.createdAt else null,
                updatedAt = if (review.id != 0L) review.updatedAt else null,
            )
        }
    }
}

data class ReviewImageResponse(
    val id: Long,
    val imageUrl: String,
    val thumbnailUrl: String,
    val displayOrder: Int,
)

data class ReviewSummaryResponse(
    val productId: Long,
    val totalCount: Int,
    val avgRating: Double,
    val ratingDistribution: Map<Int, Int>,
    val fitDistribution: Map<String, Int>,
    val photoReviewCount: Int,
) {
    companion object {
        fun from(summary: ReviewSummary): ReviewSummaryResponse {
            return ReviewSummaryResponse(
                productId = summary.productId,
                totalCount = summary.totalCount,
                avgRating = summary.avgRating,
                ratingDistribution = mapOf(
                    1 to summary.rating1Count,
                    2 to summary.rating2Count,
                    3 to summary.rating3Count,
                    4 to summary.rating4Count,
                    5 to summary.rating5Count,
                ),
                fitDistribution = mapOf(
                    "VERY_SMALL" to summary.fitVerySmallCount,
                    "SMALL" to summary.fitSmallCount,
                    "JUST_RIGHT" to summary.fitJustRightCount,
                    "LARGE" to summary.fitLargeCount,
                    "VERY_LARGE" to summary.fitVeryLargeCount,
                ),
                photoReviewCount = summary.photoReviewCount,
            )
        }
    }
}

data class ReviewEditHistoryResponse(
    val id: Long,
    val reviewId: Long,
    val previousContent: String,
    val newContent: String,
    val editCount: Int,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(history: ReviewEditHistory): ReviewEditHistoryResponse {
            return ReviewEditHistoryResponse(
                id = history.id,
                reviewId = history.reviewId,
                previousContent = history.previousContent,
                newContent = history.newContent,
                editCount = history.editCount,
                createdAt = if (history.id != 0L) history.createdAt else null,
            )
        }
    }
}
