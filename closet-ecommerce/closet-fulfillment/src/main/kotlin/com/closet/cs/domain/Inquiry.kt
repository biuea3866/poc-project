package com.closet.cs.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * 1:1 문의 Aggregate Root
 */
@Entity
@Table(name = "inquiry")
class Inquiry(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "order_id")
    val orderId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val category: InquiryCategory,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: InquiryStatus = InquiryStatus.OPEN,
) : BaseEntity() {
    companion object {
        fun create(
            memberId: Long,
            orderId: Long?,
            category: InquiryCategory,
            title: String,
            content: String,
        ): Inquiry {
            return Inquiry(
                memberId = memberId,
                orderId = orderId,
                category = category,
                title = title,
                content = content,
                status = InquiryStatus.OPEN,
            )
        }
    }

    /** 문의에 답변 처리 */
    fun answer() {
        status.validateTransitionTo(InquiryStatus.ANSWERED)
        this.status = InquiryStatus.ANSWERED
    }

    /** 문의 닫기 */
    fun close() {
        status.validateTransitionTo(InquiryStatus.CLOSED)
        this.status = InquiryStatus.CLOSED
    }
}
