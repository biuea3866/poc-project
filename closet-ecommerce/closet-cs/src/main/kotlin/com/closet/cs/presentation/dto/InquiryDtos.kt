package com.closet.cs.presentation.dto

import com.closet.cs.domain.Inquiry
import com.closet.cs.domain.InquiryCategory
import com.closet.cs.domain.InquiryReply
import com.closet.cs.domain.InquiryStatus
import com.closet.cs.domain.ReplyType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/** 문의 등록 요청 */
data class CreateInquiryRequest(
    val orderId: Long? = null,

    @field:NotNull(message = "문의 카테고리는 필수입니다")
    val category: InquiryCategory,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,
)

/** 문의 답변 요청 */
data class CreateReplyRequest(
    @field:NotNull(message = "답변 유형은 필수입니다")
    val replyType: ReplyType,

    @field:NotBlank(message = "답변 내용은 필수입니다")
    val content: String,
)

/** 문의 응답 */
data class InquiryResponse(
    val id: Long,
    val memberId: Long,
    val orderId: Long?,
    val category: InquiryCategory,
    val title: String,
    val content: String,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryResponse = InquiryResponse(
            id = inquiry.id,
            memberId = inquiry.memberId,
            orderId = inquiry.orderId,
            category = inquiry.category,
            title = inquiry.title,
            content = inquiry.content,
            status = inquiry.status,
            createdAt = inquiry.createdAt,
            updatedAt = inquiry.updatedAt,
        )
    }
}

/** 문의 상세 응답 (답변 포함) */
data class InquiryDetailResponse(
    val id: Long,
    val memberId: Long,
    val orderId: Long?,
    val category: InquiryCategory,
    val title: String,
    val content: String,
    val status: InquiryStatus,
    val replies: List<InquiryReplyResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(inquiry: Inquiry, replies: List<InquiryReply>): InquiryDetailResponse = InquiryDetailResponse(
            id = inquiry.id,
            memberId = inquiry.memberId,
            orderId = inquiry.orderId,
            category = inquiry.category,
            title = inquiry.title,
            content = inquiry.content,
            status = inquiry.status,
            replies = replies.map { InquiryReplyResponse.from(it) },
            createdAt = inquiry.createdAt,
            updatedAt = inquiry.updatedAt,
        )
    }
}

/** 답변 응답 */
data class InquiryReplyResponse(
    val id: Long,
    val inquiryId: Long,
    val replyType: ReplyType,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(reply: InquiryReply): InquiryReplyResponse = InquiryReplyResponse(
            id = reply.id,
            inquiryId = reply.inquiryId,
            replyType = reply.replyType,
            content = reply.content,
            createdAt = reply.createdAt,
        )
    }
}
