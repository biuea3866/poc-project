package com.closet.review.presentation

import com.closet.common.response.ApiResponse
import com.closet.review.application.dto.CreateReviewRequest
import com.closet.review.application.dto.ReviewResponse
import com.closet.review.application.dto.ReviewSummaryResponse
import com.closet.review.application.dto.UpdateReviewRequest
import com.closet.review.application.service.ReviewService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: CreateReviewRequest
    ): ApiResponse<ReviewResponse> {
        return ApiResponse.created(reviewService.createReview(memberId, request))
    }

    @PutMapping("/{id}")
    fun updateReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: UpdateReviewRequest
    ): ApiResponse<ReviewResponse> {
        return ApiResponse.ok(reviewService.updateReview(id, memberId, request))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long
    ) {
        reviewService.deleteReview(id, memberId)
    }

    @GetMapping("/products/{productId}")
    fun getReviewsByProductId(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "newest") sort: String
    ): ApiResponse<Page<ReviewResponse>> {
        return ApiResponse.ok(reviewService.getReviewsByProductId(productId, page, size, sort))
    }

    @GetMapping("/products/{productId}/summary")
    fun getReviewSummary(
        @PathVariable productId: Long
    ): ApiResponse<ReviewSummaryResponse> {
        return ApiResponse.ok(reviewService.getReviewSummary(productId))
    }

    @PostMapping("/{id}/helpful")
    fun markHelpful(@PathVariable id: Long): ApiResponse<Void> {
        reviewService.markHelpful(id)
        return ApiResponse.ok(null)
    }
}
