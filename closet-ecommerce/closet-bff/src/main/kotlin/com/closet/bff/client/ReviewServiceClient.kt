package com.closet.bff.client

import com.closet.bff.dto.ReviewBffResponse
import com.closet.bff.dto.ReviewSummaryBffResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

/**
 * Review Service Feign Client (CP-30).
 * Port: 8087 (PD-05)
 */
@FeignClient(name = "review-service", url = "\${service.review.url}")
interface ReviewServiceClient {

    @GetMapping("/api/v1/reviews")
    fun getReviewsByProductId(
        @RequestParam productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<ReviewBffResponse>>

    @GetMapping("/api/v1/reviews/{id}")
    fun getReview(@PathVariable id: Long): ApiResponse<ReviewBffResponse>

    @GetMapping("/api/v1/reviews/summary/{productId}")
    fun getReviewSummary(@PathVariable productId: Long): ApiResponse<ReviewSummaryBffResponse?>
}
