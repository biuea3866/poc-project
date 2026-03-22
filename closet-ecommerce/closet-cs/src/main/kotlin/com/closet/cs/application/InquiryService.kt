package com.closet.cs.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.cs.domain.Inquiry
import com.closet.cs.domain.InquiryReply
import com.closet.cs.domain.InquiryStatus
import com.closet.cs.domain.repository.InquiryReplyRepository
import com.closet.cs.domain.repository.InquiryRepository
import com.closet.cs.presentation.dto.CreateInquiryRequest
import com.closet.cs.presentation.dto.CreateReplyRequest
import com.closet.cs.presentation.dto.InquiryDetailResponse
import com.closet.cs.presentation.dto.InquiryReplyResponse
import com.closet.cs.presentation.dto.InquiryResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryReplyRepository: InquiryReplyRepository,
) {
    /** 문의 등록 */
    @Transactional
    fun create(memberId: Long, request: CreateInquiryRequest): InquiryResponse {
        val inquiry = Inquiry.create(
            memberId = memberId,
            orderId = request.orderId,
            category = request.category,
            title = request.title,
            content = request.content,
        )

        val saved = inquiryRepository.save(inquiry)
        return InquiryResponse.from(saved)
    }

    /** 내 문의 목록 (페이징) */
    fun findByMemberId(memberId: Long, pageable: Pageable): Page<InquiryResponse> {
        return inquiryRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
            .map { InquiryResponse.from(it) }
    }

    /** 문의 상세 (답변 포함) */
    fun findById(id: Long): InquiryDetailResponse {
        val inquiry = inquiryRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "문의를 찾을 수 없습니다") }

        val replies = inquiryReplyRepository.findByInquiryIdOrderByCreatedAtAsc(id)
        return InquiryDetailResponse.from(inquiry, replies)
    }

    /** 문의 답변 등록 */
    @Transactional
    fun addReply(inquiryId: Long, request: CreateReplyRequest): InquiryReplyResponse {
        val inquiry = inquiryRepository.findById(inquiryId)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "문의를 찾을 수 없습니다") }

        // 답변 등록 시 상태를 ANSWERED로 전이
        if (inquiry.status == InquiryStatus.OPEN) {
            inquiry.answer()
        }

        val reply = InquiryReply.create(
            inquiryId = inquiryId,
            replyType = request.replyType,
            content = request.content,
        )

        val saved = inquiryReplyRepository.save(reply)
        return InquiryReplyResponse.from(saved)
    }

    /** 문의 닫기 */
    @Transactional
    fun close(id: Long) {
        val inquiry = inquiryRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ENTITY_NOT_FOUND, "문의를 찾을 수 없습니다") }

        inquiry.close()
    }

    /** 상태별 문의 목록 (관리자용) */
    fun findByStatus(status: InquiryStatus, pageable: Pageable): Page<InquiryResponse> {
        return inquiryRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
            .map { InquiryResponse.from(it) }
    }
}
