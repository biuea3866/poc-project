package com.closet.bff.facade

import com.closet.bff.client.ReviewServiceClient
import com.closet.bff.dto.ReviewBffResponse
import com.closet.bff.dto.ReviewSummaryBffResponse
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 BFF Facade (CP-30).
 *
 * review-service(8087)의 리뷰 데이터를 조합한다.
 */
@Service
class ReviewBffFacade(
    private val reviewClient: ReviewServiceClient,
) {
    data class ProductReviewBffResponse(
        val summary: ReviewSummaryBffResponse?,
        val reviews: Page<ReviewBffResponse>?,
    )

    /**
     * 상품별 리뷰 집계 + 목록 조합.
     */
    fun getProductReviews(
        productId: Long,
        page: Int = 0,
        size: Int = 10,
    ): ProductReviewBffResponse {
        val summary = runCatching { reviewClient.getReviewSummary(productId) }.getOrNull()?.data
        val reviews = runCatching { reviewClient.getReviewsByProductId(productId, page, size) }.getOrNull()?.data

        return ProductReviewBffResponse(
            summary = summary,
            reviews = reviews,
        )
    }
}
