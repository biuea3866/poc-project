package com.closet.shipping.domain.cs.inquiry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 문의 답변 엔티티.
 *
 * 관리자가 1:1 문의에 답변을 작성한다.
 * 하나의 문의에 여러 답변이 달릴 수 있다 (추가 문의 대응).
 */
@Entity
@Table(name = "inquiry_answer")
@EntityListeners(AuditingEntityListener::class)
class InquiryAnswer(
    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,
    @Column(name = "admin_id", nullable = false)
    val adminId: Long,
    @Column(name = "content", nullable = false, length = 2000)
    var content: String,
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
     * 답변 내용 수정.
     */
    fun updateContent(newContent: String) {
        this.content = newContent
    }

    companion object {
        fun create(
            inquiryId: Long,
            adminId: Long,
            content: String,
        ): InquiryAnswer {
            return InquiryAnswer(
                inquiryId = inquiryId,
                adminId = adminId,
                content = content,
            )
        }
    }
}
