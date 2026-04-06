package com.closet.shipping.application.cs

import org.springframework.stereotype.Component

/**
 * 문의 Facade.
 * Controller에서 호출하여 Service로 위임한다.
 */
@Component
class InquiryFacade(
    private val inquiryService: InquiryService,
) {
    fun createInquiry(
        memberId: Long,
        request: CreateInquiryRequest,
    ): InquiryResponse {
        return inquiryService.createInquiry(memberId, request)
    }

    fun findById(id: Long): InquiryResponse {
        return inquiryService.findById(id)
    }

    fun findByMemberId(memberId: Long): List<InquiryListResponse> {
        return inquiryService.findByMemberId(memberId)
    }

    fun createAnswer(
        inquiryId: Long,
        adminId: Long,
        request: CreateAnswerRequest,
    ): InquiryAnswerResponse {
        return inquiryService.createAnswer(inquiryId, adminId, request)
    }

    fun closeInquiry(id: Long) {
        inquiryService.closeInquiry(id)
    }
}
