package com.closet.shipping.application.cs

import com.closet.shipping.domain.cs.inquiry.Inquiry
import com.closet.shipping.domain.cs.inquiry.InquiryAnswer
import com.closet.shipping.domain.cs.inquiry.InquiryAttachment
import java.time.ZonedDateTime

/**
 * 문의 상세 응답.
 */
data class InquiryResponse(
    val id: Long,
    val memberId: Long,
    val orderId: Long?,
    val category: String,
    val title: String,
    val content: String,
    val status: String,
    val answers: List<InquiryAnswerResponse>,
    val attachments: List<InquiryAttachmentResponse>,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(
            inquiry: Inquiry,
            answers: List<InquiryAnswer> = emptyList(),
            attachments: List<InquiryAttachment> = emptyList(),
        ): InquiryResponse {
            return InquiryResponse(
                id = inquiry.id,
                memberId = inquiry.memberId,
                orderId = inquiry.orderId,
                category = inquiry.category.name,
                title = inquiry.title,
                content = inquiry.content,
                status = inquiry.status.name,
                answers = answers.map { InquiryAnswerResponse.from(it) },
                attachments = attachments.map { InquiryAttachmentResponse.from(it) },
                createdAt = if (inquiry.id != 0L) inquiry.createdAt else null,
                updatedAt = if (inquiry.id != 0L) inquiry.updatedAt else null,
            )
        }
    }
}

/**
 * 문의 목록 응답 (간략).
 */
data class InquiryListResponse(
    val id: Long,
    val category: String,
    val title: String,
    val status: String,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryListResponse {
            return InquiryListResponse(
                id = inquiry.id,
                category = inquiry.category.name,
                title = inquiry.title,
                status = inquiry.status.name,
                createdAt = if (inquiry.id != 0L) inquiry.createdAt else null,
            )
        }
    }
}

/**
 * 답변 응답.
 */
data class InquiryAnswerResponse(
    val id: Long,
    val inquiryId: Long,
    val adminId: Long,
    val content: String,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(answer: InquiryAnswer): InquiryAnswerResponse {
            return InquiryAnswerResponse(
                id = answer.id,
                inquiryId = answer.inquiryId,
                adminId = answer.adminId,
                content = answer.content,
                createdAt = if (answer.id != 0L) answer.createdAt else null,
                updatedAt = if (answer.id != 0L) answer.updatedAt else null,
            )
        }
    }
}

/**
 * 첨부파일 응답.
 */
data class InquiryAttachmentResponse(
    val id: Long,
    val inquiryId: Long,
    val fileUrl: String,
    val fileName: String,
    val fileSize: Long,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(attachment: InquiryAttachment): InquiryAttachmentResponse {
            return InquiryAttachmentResponse(
                id = attachment.id,
                inquiryId = attachment.inquiryId,
                fileUrl = attachment.fileUrl,
                fileName = attachment.fileName,
                fileSize = attachment.fileSize,
                createdAt = if (attachment.id != 0L) attachment.createdAt else null,
            )
        }
    }
}
