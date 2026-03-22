package com.closet.cs.presentation.dto

import com.closet.cs.domain.Faq
import com.closet.cs.domain.FaqCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/** FAQ 등록 요청 */
data class CreateFaqRequest(
    @field:NotNull(message = "FAQ 카테고리는 필수입니다")
    val category: FaqCategory,

    @field:NotBlank(message = "질문은 필수입니다")
    val question: String,

    @field:NotBlank(message = "답변은 필수입니다")
    val answer: String,

    val sortOrder: Int = 0,
)

/** FAQ 수정 요청 */
data class UpdateFaqRequest(
    @field:NotBlank(message = "질문은 필수입니다")
    val question: String,

    @field:NotBlank(message = "답변은 필수입니다")
    val answer: String,

    val sortOrder: Int = 0,
)

/** FAQ 응답 */
data class FaqResponse(
    val id: Long,
    val category: FaqCategory,
    val question: String,
    val answer: String,
    val sortOrder: Int,
    val isVisible: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(faq: Faq): FaqResponse = FaqResponse(
            id = faq.id,
            category = faq.category,
            question = faq.question,
            answer = faq.answer,
            sortOrder = faq.sortOrder,
            isVisible = faq.isVisible,
            createdAt = faq.createdAt,
            updatedAt = faq.updatedAt,
        )
    }
}
