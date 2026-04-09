package com.closet.review.application

import com.closet.review.domain.Review
import com.closet.review.domain.ReviewEditHistory
import com.closet.review.domain.ReviewSortType
import com.closet.review.domain.ReviewSummary
import com.closet.review.domain.SizeFit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.ZonedDateTime

// === Requests ===

/**
 * 리뷰 작성 요청 (US-801, US-802).
 */
data class CreateReviewRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val orderItemId: Long,
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(min = 20, max = 1000) val content: String,
    /** 이미지 URL 목록 (Presigned URL로 업로드한 이미지, 최대 5장) */
    val imageUrls: List<String> = emptyList(),
    // 사이즈 후기 (US-802)
    val height: Int? = null,
    val weight: Int? = null,
    val normalSize: String? = null,
    val purchasedSize: String? = null,
    val fitType: SizeFit? = null,
)

/**
 * 리뷰 수정 요청 (US-801).
 */
data class UpdateReviewRequest(
    @field:NotBlank @field:Size(min = 20, max = 1000) val content: String,
    /** 이미지 URL 목록 (교체). null이면 이미지 변경 없음. */
    val imageUrls: List<String>? = null,
)

/**
 * 리뷰 목록 조회 쿼리 파라미터.
 */
data class ReviewListQuery(
    val productId: Long,
    val sort: ReviewSortType = ReviewSortType.LATEST,
    val photoOnly: Boolean = false,
    val page: Int = 0,
    val size: Int = 10,
    // "비슷한 체형" 필터 (US-802)
    val myHeight: Int? = null,
    val myWeight: Int? = null,
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
    val normalSize: String?,
    val purchasedSize: String?,
    val fitType: String?,
    val helpfulCount: Int,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
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
                normalSize = review.normalSize,
                purchasedSize = review.purchasedSize,
                fitType = review.fitType?.name,
                helpfulCount = review.helpfulCount,
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
                ratingDistribution =
                    mapOf(
                        1 to summary.rating1Count,
                        2 to summary.rating2Count,
                        3 to summary.rating3Count,
                        4 to summary.rating4Count,
                        5 to summary.rating5Count,
                    ),
                fitDistribution =
                    mapOf(
                        "SMALL" to summary.fitSmallCount,
                        "PERFECT" to summary.fitPerfectCount,
                        "LARGE" to summary.fitLargeCount,
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
    val createdAt: ZonedDateTime?,
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
