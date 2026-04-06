package com.closet.shipping.domain.cs.inquiry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 1:1 문의 엔티티.
 *
 * 상태 머신: PENDING -> IN_PROGRESS -> ANSWERED -> CLOSED
 * 비즈니스: answer() 답변 시 상태 전이, close() 문의 종료, isEditable() 수정 가능 여부
 */
@Entity
@Table(name = "inquiry")
@EntityListeners(AuditingEntityListener::class)
class Inquiry(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "order_id")
    val orderId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    val category: InquiryCategory,
    @Column(name = "title", nullable = false, length = 200)
    val title: String,
    @Column(name = "content", nullable = false, length = 2000)
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: InquiryStatus = InquiryStatus.PENDING,
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    /**
     * 답변 완료 처리.
     * 관리자가 답변을 작성하면 호출된다.
     */
    fun answer() {
        status.validateTransitionTo(InquiryStatus.ANSWERED)
        status = InquiryStatus.ANSWERED
    }

    /**
     * 문의 진행 중으로 전환.
     * 관리자가 확인하거나, 답변 후 추가 문의가 발생할 때 호출된다.
     */
    fun markInProgress() {
        status.validateTransitionTo(InquiryStatus.IN_PROGRESS)
        status = InquiryStatus.IN_PROGRESS
    }

    /**
     * 문의 닫기.
     * 사용자 또는 관리자가 문의를 종료할 때 호출된다.
     */
    fun close() {
        status.validateTransitionTo(InquiryStatus.CLOSED)
        status = InquiryStatus.CLOSED
    }

    /**
     * 수정 가능 여부.
     * PENDING 상태에서만 수정 가능하다.
     */
    fun isEditable(): Boolean = status == InquiryStatus.PENDING

    /**
     * 소프트 삭제.
     */
    fun delete() {
        check(deletedAt == null) {
            "이미 삭제된 문의입니다: id=$id"
        }
        deletedAt = ZonedDateTime.now()
    }

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
            )
        }
    }
}
