package com.closet.bff.presentation

import com.closet.bff.client.ReviewServiceClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reviews")
class ReviewProxyController(
    private val reviewClient: ReviewServiceClient,
) {

    @PostMapping
    fun createReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = reviewClient.createReview(memberId, request)

    @PutMapping("/{id}")
    fun updateReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = reviewClient.updateReview(id, memberId, request)

    @DeleteMapping("/{id}")
    fun deleteReview(
        @PathVariable id: Long,
        @RequestHeader("X-Member-Id") memberId: Long,
    ): ResponseEntity<Void> {
        reviewClient.deleteReview(id, memberId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/products/{productId}")
    fun getReviewsByProductId(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "newest") sort: String,
    ) = reviewClient.getReviewsByProductId(productId, page, size, sort)

    @GetMapping("/products/{productId}/summary")
    fun getReviewSummary(@PathVariable productId: Long) =
        reviewClient.getReviewSummary(productId)

    @PostMapping("/{id}/helpful")
    fun markHelpful(@PathVariable id: Long) =
        reviewClient.markHelpful(id)
}
