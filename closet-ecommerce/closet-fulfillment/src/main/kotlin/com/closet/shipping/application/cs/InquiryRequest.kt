package com.closet.shipping.application.cs

import com.closet.shipping.domain.cs.inquiry.InquiryCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 문의 작성 요청.
 */
data class CreateInquiryRequest(
    val orderId: Long? = null,
    @field:NotNull val category: InquiryCategory,
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank @field:Size(max = 2000) val content: String,
    val attachments: List<AttachmentRequest> = emptyList(),
)

/**
 * 첨부파일 요청.
 */
data class AttachmentRequest(
    @field:NotBlank val fileUrl: String,
    @field:NotBlank val fileName: String,
    val fileSize: Long,
)

/**
 * 답변 작성 요청.
 */
data class CreateAnswerRequest(
    @field:NotBlank @field:Size(max = 2000) val content: String,
)
