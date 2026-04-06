package com.closet.shipping.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.shipping.application.cs.CreateAnswerRequest
import com.closet.shipping.application.cs.CreateInquiryRequest
import com.closet.shipping.application.cs.InquiryAnswerResponse
import com.closet.shipping.application.cs.InquiryFacade
import com.closet.shipping.application.cs.InquiryListResponse
import com.closet.shipping.application.cs.InquiryResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/v1/inquiries")
class InquiryController(
    private val inquiryFacade: InquiryFacade,
) {
    /**
     * 문의 작성.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.BUYER)
    fun createInquiry(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody @Valid request: CreateInquiryRequest,
    ): ApiResponse<InquiryResponse> {
        val response = inquiryFacade.createInquiry(memberId, request)
        return ApiResponse.created(response)
    }

    /**
     * 내 문의 목록 조회.
     */
    @GetMapping
    fun getInquiries(
        @RequestParam memberId: Long,
    ): ApiResponse<List<InquiryListResponse>> {
        val response = inquiryFacade.findByMemberId(memberId)
        return ApiResponse.ok(response)
    }

    /**
     * 문의 상세 조회.
     */
    @GetMapping("/{id}")
    fun getInquiry(
        @PathVariable id: Long,
    ): ApiResponse<InquiryResponse> {
        val response = inquiryFacade.findById(id)
        return ApiResponse.ok(response)
    }

    /**
     * 답변 작성 (관리자).
     */
    @PostMapping("/{id}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.ADMIN)
    fun createAnswer(
        @PathVariable id: Long,
        @RequestHeader("X-Admin-Id") adminId: Long,
        @RequestBody @Valid request: CreateAnswerRequest,
    ): ApiResponse<InquiryAnswerResponse> {
        val response = inquiryFacade.createAnswer(id, adminId, request)
        return ApiResponse.created(response)
    }

    /**
     * 문의 닫기.
     */
    @PutMapping("/{id}/close")
    fun closeInquiry(
        @PathVariable id: Long,
    ): ApiResponse<Unit> {
        inquiryFacade.closeInquiry(id)
        return ApiResponse.ok(Unit)
    }
}
