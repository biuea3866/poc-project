package com.closet.review.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.review.application.CreateReviewRequest
import com.closet.review.application.PresignedUploadUrlRequest
import com.closet.review.application.PresignedUploadUrlResponse
import com.closet.review.application.ReviewEditHistoryResponse
import com.closet.review.application.ReviewImageService
import com.closet.review.application.ReviewResponse
import com.closet.review.application.ReviewService
import com.closet.review.application.ReviewSummaryResponse
import com.closet.review.application.ReviewSummaryService
import com.closet.review.application.UpdateReviewRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 리뷰 API 컨트롤러 (BUYER).
 *
 * POST   /api/v1/reviews                  - 리뷰 작성 (CP-24)
 * PATCH  /api/v1/reviews/{id}             - 리뷰 수정 (CP-26)
 * DELETE /api/v1/reviews/{id}             - 리뷰 삭제 (CP-24)
 * GET    /api/v1/reviews/{id}             - 리뷰 상세 조회
 * GET    /api/v1/reviews?productId=       - 상품별 리뷰 목록
 * GET    /api/v1/reviews/my               - 내 리뷰 목록
 * GET    /api/v1/reviews/{id}/history     - 수정 이력 (CP-26)
 * GET    /api/v1/reviews/summary/{productId} - 리뷰 집계 (CP-25)
 * POST   /api/v1/reviews/images/presigned-url - 이미지 Presigned Upload URL 발급
 */
@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val reviewSummaryService: ReviewSummaryService,
    private val reviewImageService: ReviewImageService,
) {

    /**
     * 리뷰 작성 (CP-24).
     * multipart/form-data로 이미지와 함께 업로드한다.
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.BUYER)
    fun createReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestPart("review") @Valid request: CreateReviewRequest,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
    ): ApiResponse<ReviewResponse> {
        val response = reviewService.createReview(memberId, request, images)
        return ApiResponse.created(response)
    }

    /**
     * 리뷰 수정 (CP-26, PD-32).
     * 별점 수정 불가, 최대 3회, 7일 이내.
     */
    @PatchMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RoleRequired(MemberRole.BUYER)
    fun updateReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
        @RequestPart("review") @Valid request: UpdateReviewRequest,
        @RequestPart("images", required = false) images: List<MultipartFile>?,
    ): ApiResponse<ReviewResponse> {
        val response = reviewService.updateReview(memberId, id, request, images)
        return ApiResponse.ok(response)
    }

    /**
     * 리뷰 삭제.
     */
    @DeleteMapping("/{id}")
    @RoleRequired(MemberRole.BUYER)
    fun deleteReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        reviewService.deleteReview(memberId, id)
        return ApiResponse.ok(Unit)
    }

    /**
     * 리뷰 상세 조회.
     */
    @GetMapping("/{id}")
    fun getReview(@PathVariable id: Long): ApiResponse<ReviewResponse> {
        val response = reviewService.findById(id)
        return ApiResponse.ok(response)
    }

    /**
     * 상품별 리뷰 목록 조회.
     */
    @GetMapping
    fun getReviewsByProductId(
        @RequestParam productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<ReviewResponse>> {
        val response = reviewService.findByProductId(productId, PageRequest.of(page, size))
        return ApiResponse.ok(response)
    }

    /**
     * 내 리뷰 목록 조회.
     */
    @GetMapping("/my")
    @RoleRequired(MemberRole.BUYER)
    fun getMyReviews(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ApiResponse<Page<ReviewResponse>> {
        val response = reviewService.findByMemberId(memberId, PageRequest.of(page, size))
        return ApiResponse.ok(response)
    }

    /**
     * 리뷰 수정 이력 조회 (CP-26).
     */
    @GetMapping("/{id}/history")
    fun getEditHistory(@PathVariable id: Long): ApiResponse<List<ReviewEditHistoryResponse>> {
        val response = reviewService.getEditHistory(id)
        return ApiResponse.ok(response)
    }

    /**
     * 상품별 리뷰 집계 조회 (CP-25).
     */
    @GetMapping("/summary/{productId}")
    fun getReviewSummary(@PathVariable productId: Long): ApiResponse<ReviewSummaryResponse?> {
        val response = reviewSummaryService.getSummary(productId)
        return ApiResponse.ok(response)
    }

    /**
     * 리뷰 이미지 Presigned Upload URL 발급.
     * 클라이언트가 이 URL로 MinIO/S3에 직접 업로드한다.
     */
    @PostMapping("/images/presigned-url")
    @RoleRequired(MemberRole.BUYER)
    fun getPresignedUploadUrl(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody @Valid request: PresignedUploadUrlRequest,
    ): ApiResponse<PresignedUploadUrlResponse> {
        val response = reviewImageService.generatePresignedUploadUrl(memberId, request)
        return ApiResponse.ok(response)
    }
}
