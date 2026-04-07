package com.closet.review.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.review.application.CreateReviewRequest
import com.closet.review.application.PresignedUploadUrlRequest
import com.closet.review.application.PresignedUploadUrlResponse
import com.closet.review.application.ReviewEditHistoryResponse
import com.closet.review.application.ReviewListQuery
import com.closet.review.application.ReviewResponse
import com.closet.review.application.ReviewSummaryResponse
import com.closet.review.application.UpdateReviewRequest
import com.closet.review.application.facade.ReviewFacade
import com.closet.review.domain.ReviewSortType
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 리뷰 API 컨트롤러 (BUYER).
 *
 * Controller -> Facade -> Service 패턴.
 * Controller는 HTTP 요청/응답 매핑만 담당하고,
 * 비즈니스 오케스트레이션은 Facade에 위임한다.
 *
 * POST   /api/v1/reviews                       - 리뷰 작성 (US-801)
 * PATCH  /api/v1/reviews/{id}                  - 리뷰 수정 (US-801)
 * DELETE /api/v1/reviews/{id}                  - 리뷰 삭제 (US-801)
 * GET    /api/v1/reviews/{id}                  - 리뷰 상세 조회
 * GET    /api/v1/reviews?productId=&sort=&photoOnly=&page=&size= - 리뷰 목록 조회
 * GET    /api/v1/reviews/my                    - 내 리뷰 목록
 * GET    /api/v1/reviews/{id}/history          - 수정 이력
 * GET    /api/v1/reviews/summary/{productId}   - 리뷰 집계 (US-804)
 * POST   /api/v1/reviews/{id}/helpful          - 도움이 됐어요
 * POST   /api/v1/reviews/images/presigned-url  - 이미지 Presigned URL 발급
 */
@RestController
@RequestMapping("/api/v1/reviews")
class ReviewController(
    private val reviewFacade: ReviewFacade,
) {
    /**
     * 리뷰 작성 (US-801).
     * JSON 요청 (이미지는 Presigned URL로 사전 업로드).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.BUYER)
    fun createReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody @Valid request: CreateReviewRequest,
    ): ApiResponse<ReviewResponse> {
        val response = reviewFacade.createReview(memberId, request)
        return ApiResponse.created(response)
    }

    /**
     * 리뷰 수정 (US-801).
     * 별점 수정 불가, 최대 3회, 7일 이내.
     */
    @PatchMapping("/{id}")
    @RoleRequired(MemberRole.BUYER)
    fun updateReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateReviewRequest,
    ): ApiResponse<ReviewResponse> {
        val response = reviewFacade.updateReview(memberId, id, request)
        return ApiResponse.ok(response)
    }

    /**
     * 리뷰 삭제 (본인만, US-801).
     */
    @DeleteMapping("/{id}")
    @RoleRequired(MemberRole.BUYER)
    fun deleteReview(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        reviewFacade.deleteReview(memberId, id)
        return ApiResponse.ok(Unit)
    }

    /**
     * 리뷰 상세 조회.
     */
    @GetMapping("/{id}")
    fun getReview(
        @PathVariable id: Long,
    ): ApiResponse<ReviewResponse> {
        val response = reviewFacade.getReview(id)
        return ApiResponse.ok(response)
    }

    /**
     * 상품별 리뷰 목록 조회 (정렬/필터 지원).
     */
    @GetMapping
    fun getReviews(
        @RequestParam productId: Long,
        @RequestParam(defaultValue = "LATEST") sort: ReviewSortType,
        @RequestParam(defaultValue = "false") photoOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) myHeight: Int?,
        @RequestParam(required = false) myWeight: Int?,
    ): ApiResponse<Page<ReviewResponse>> {
        val query =
            ReviewListQuery(
                productId = productId,
                sort = sort,
                photoOnly = photoOnly,
                page = page,
                size = size,
                myHeight = myHeight,
                myWeight = myWeight,
            )
        val response = reviewFacade.getReviews(query)
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
        val response = reviewFacade.getMyReviews(memberId, PageRequest.of(page, size))
        return ApiResponse.ok(response)
    }

    /**
     * 리뷰 수정 이력 조회.
     */
    @GetMapping("/{id}/history")
    fun getEditHistory(
        @PathVariable id: Long,
    ): ApiResponse<List<ReviewEditHistoryResponse>> {
        val response = reviewFacade.getEditHistory(id)
        return ApiResponse.ok(response)
    }

    /**
     * 상품별 리뷰 집계 조회 (US-804).
     */
    @GetMapping("/summary/{productId}")
    fun getReviewSummary(
        @PathVariable productId: Long,
    ): ApiResponse<ReviewSummaryResponse?> {
        val response = reviewFacade.getSummary(productId)
        return ApiResponse.ok(response)
    }

    /**
     * "도움이 됐어요" (US-801).
     */
    @PostMapping("/{id}/helpful")
    @RoleRequired(MemberRole.BUYER)
    fun markHelpful(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        reviewFacade.markHelpful(memberId, id)
        return ApiResponse.ok(Unit)
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
        val response = reviewFacade.getPresignedUploadUrl(memberId, request)
        return ApiResponse.ok(response)
    }
}
