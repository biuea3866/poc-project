package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "review-service", url = "\${service.review.url}")
interface ReviewServiceClient {

    @PostMapping("/reviews")
    fun createReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ): Any

    @PutMapping("/reviews/{id}")
    fun updateReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ): Any

    @DeleteMapping("/reviews/{id}")
    fun deleteReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long,
    )

    @GetMapping("/reviews/products/{productId}")
    fun getReviewsByProductId(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
    ): Any

    @GetMapping("/reviews/products/{productId}/summary")
    fun getReviewSummary(@PathVariable productId: Long): Any

    @PostMapping("/reviews/{id}/helpful")
    fun markHelpful(@PathVariable id: Long): Any
}
