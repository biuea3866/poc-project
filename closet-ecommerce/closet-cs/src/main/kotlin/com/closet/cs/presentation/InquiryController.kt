package com.closet.cs.presentation

import com.closet.common.response.ApiResponse
import com.closet.cs.application.InquiryService
import com.closet.cs.domain.InquiryStatus
import com.closet.cs.presentation.dto.CreateInquiryRequest
import com.closet.cs.presentation.dto.CreateReplyRequest
import com.closet.cs.presentation.dto.InquiryDetailResponse
import com.closet.cs.presentation.dto.InquiryReplyResponse
import com.closet.cs.presentation.dto.InquiryResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
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

@RestController
@RequestMapping("/api/v1/cs")
class InquiryController(
    private val inquiryService: InquiryService,
) {
    /** 문의 등록 */
    @PostMapping("/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    fun createInquiry(
        @RequestHeader("X-Member-Id") memberId: Long,
        @Valid @RequestBody request: CreateInquiryRequest,
    ): ApiResponse<InquiryResponse> {
        return ApiResponse.created(inquiryService.create(memberId, request))
    }

    /** 내 문의 목록 */
    @GetMapping("/inquiries/my")
    fun getMyInquiries(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PageableDefault(size = 10) pageable: Pageable,
    ): ApiResponse<Page<InquiryResponse>> {
        return ApiResponse.ok(inquiryService.findByMemberId(memberId, pageable))
    }

    /** 문의 상세 */
    @GetMapping("/inquiries/{id}")
    fun getInquiry(@PathVariable id: Long): ApiResponse<InquiryDetailResponse> {
        return ApiResponse.ok(inquiryService.findById(id))
    }

    /** 문의 답변 등록 */
    @PostMapping("/inquiries/{id}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    fun addReply(
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateReplyRequest,
    ): ApiResponse<InquiryReplyResponse> {
        return ApiResponse.created(inquiryService.addReply(id, request))
    }

    /** 문의 닫기 */
    @PatchMapping("/inquiries/{id}/close")
    fun closeInquiry(@PathVariable id: Long): ApiResponse<Unit> {
        inquiryService.close(id)
        return ApiResponse.ok(Unit)
    }

    /** 상태별 문의 목록 (관리자용) */
    @GetMapping("/inquiries")
    fun getInquiriesByStatus(
        @RequestParam status: InquiryStatus,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<InquiryResponse>> {
        return ApiResponse.ok(inquiryService.findByStatus(status, pageable))
    }
}
