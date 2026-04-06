package com.closet.cs.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * FAQ (자주 묻는 질문)
 */
@Entity
@Table(name = "faq")
class Faq(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val category: FaqCategory,
    @Column(nullable = false, length = 500)
    var question: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var answer: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "is_visible", nullable = false, columnDefinition = "TINYINT(1)")
    var isVisible: Boolean = true,
) : BaseEntity() {
    companion object {
        fun create(
            category: FaqCategory,
            question: String,
            answer: String,
            sortOrder: Int = 0,
        ): Faq {
            return Faq(
                category = category,
                question = question,
                answer = answer,
                sortOrder = sortOrder,
                isVisible = true,
            )
        }
    }

    /** FAQ 노출 */
    fun show() {
        this.isVisible = true
    }

    /** FAQ 숨김 */
    fun hide() {
        this.isVisible = false
    }

    /** FAQ 내용 수정 */
    fun updateContent(
        question: String,
        answer: String,
        sortOrder: Int,
    ) {
        this.question = question
        this.answer = answer
        this.sortOrder = sortOrder
    }
}
