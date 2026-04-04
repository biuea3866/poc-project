package com.closet.bff.presentation

import com.closet.bff.facade.ReviewBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 리뷰 BFF 컨트롤러 (CP-30).
 */
@RestController
@RequestMapping("/api/v1/bff")
class BffReviewController(
    private val reviewFacade: ReviewBffFacade,
) {

    /**
     * 상품별 리뷰 집계 + 목록.
     */
    @GetMapping("/products/{productId}/reviews")
    fun getProductReviews(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ) = ApiResponse.ok(reviewFacade.getProductReviews(productId, page, size))
}
