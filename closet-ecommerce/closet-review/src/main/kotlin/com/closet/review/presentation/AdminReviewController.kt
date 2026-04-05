package com.closet.review.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.review.application.facade.ReviewFacade
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 관리자 리뷰 API 컨트롤러 (CP-26, PD-35).
 *
 * Controller -> Facade -> Service 패턴.
 *
 * PATCH /api/v1/admin/reviews/{id}/hide   - 리뷰 블라인드
 * PATCH /api/v1/admin/reviews/{id}/unhide - 리뷰 블라인드 해제
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
class AdminReviewController(
    private val reviewFacade: ReviewFacade,
) {

    /**
     * 리뷰 블라인드 (PD-35).
     * HIDDEN 상태로 변경하여 구매자에게 노출되지 않도록 한다.
     */
    @PatchMapping("/{id}/hide")
    @RoleRequired(MemberRole.ADMIN)
    fun hideReview(@PathVariable id: Long): ApiResponse<Unit> {
        reviewFacade.hideReview(id)
        return ApiResponse.ok(Unit)
    }

    /**
     * 리뷰 블라인드 해제.
     */
    @PatchMapping("/{id}/unhide")
    @RoleRequired(MemberRole.ADMIN)
    fun unhideReview(@PathVariable id: Long): ApiResponse<Unit> {
        reviewFacade.unhideReview(id)
        return ApiResponse.ok(Unit)
    }
}
