package com.closet.shipping.application.cs

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.shipping.domain.cs.inquiry.Inquiry
import com.closet.shipping.domain.cs.inquiry.InquiryAnswer
import com.closet.shipping.domain.cs.inquiry.InquiryAnswerRepository
import com.closet.shipping.domain.cs.inquiry.InquiryAttachment
import com.closet.shipping.domain.cs.inquiry.InquiryAttachmentRepository
import com.closet.shipping.domain.cs.inquiry.InquiryRepository
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val inquiryAnswerRepository: InquiryAnswerRepository,
    private val inquiryAttachmentRepository: InquiryAttachmentRepository,
) {
    /**
     * 문의 작성.
     */
    @Transactional
    fun createInquiry(
        memberId: Long,
        request: CreateInquiryRequest,
    ): InquiryResponse {
        val inquiry =
            Inquiry.create(
                memberId = memberId,
                orderId = request.orderId,
                category = request.category,
                title = request.title,
                content = request.content,
            )
        val saved = inquiryRepository.save(inquiry)

        // 첨부파일 저장
        val attachments =
            if (request.attachments.isNotEmpty()) {
                val attachmentEntities =
                    request.attachments.map {
                        InquiryAttachment.create(
                            inquiryId = saved.id,
                            fileUrl = it.fileUrl,
                            fileName = it.fileName,
                            fileSize = it.fileSize,
                        )
                    }
                inquiryAttachmentRepository.saveAll(attachmentEntities)
            } else {
                emptyList()
            }

        logger.info { "문의 작성 완료: id=${saved.id}, memberId=$memberId, category=${request.category}" }
        return InquiryResponse.from(saved, emptyList(), attachments)
    }

    /**
     * 문의 상세 조회.
     */
    fun findById(id: Long): InquiryResponse {
        val inquiry = getInquiryOrThrow(id)
        val answers = inquiryAnswerRepository.findByInquiryIdOrderByCreatedAtAsc(inquiry.id)
        val attachments = inquiryAttachmentRepository.findByInquiryId(inquiry.id)
        return InquiryResponse.from(inquiry, answers, attachments)
    }

    /**
     * 내 문의 목록 조회.
     */
    fun findByMemberId(memberId: Long): List<InquiryListResponse> {
        return inquiryRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId)
            .map { InquiryListResponse.from(it) }
    }

    /**
     * 답변 작성 (관리자).
     * 문의 상태를 ANSWERED로 전이한다.
     */
    @Transactional
    fun createAnswer(
        inquiryId: Long,
        adminId: Long,
        request: CreateAnswerRequest,
    ): InquiryAnswerResponse {
        val inquiry = getInquiryOrThrow(inquiryId)

        // 답변 시 상태 전이
        inquiry.answer()

        val answer =
            InquiryAnswer.create(
                inquiryId = inquiry.id,
                adminId = adminId,
                content = request.content,
            )
        val saved = inquiryAnswerRepository.save(answer)

        logger.info { "답변 작성 완료: inquiryId=$inquiryId, adminId=$adminId" }
        return InquiryAnswerResponse.from(saved)
    }

    /**
     * 문의 닫기.
     */
    @Transactional
    fun closeInquiry(id: Long) {
        val inquiry = getInquiryOrThrow(id)
        inquiry.close()
        logger.info { "문의 닫기 완료: id=$id" }
    }

    private fun getInquiryOrThrow(id: Long): Inquiry {
        return inquiryRepository.findByIdOrNull(id)
            ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "문의를 찾을 수 없습니다: id=$id")
    }
}
