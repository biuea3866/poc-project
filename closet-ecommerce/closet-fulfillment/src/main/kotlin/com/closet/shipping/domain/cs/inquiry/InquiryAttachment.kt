package com.closet.shipping.domain.cs.inquiry

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

/**
 * 문의 첨부파일 엔티티.
 *
 * 문의 작성 시 이미지/파일을 첨부할 수 있다.
 */
@Entity
@Table(name = "inquiry_attachment")
@EntityListeners(AuditingEntityListener::class)
class InquiryAttachment(
    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,
    @Column(name = "file_url", nullable = false, length = 500)
    val fileUrl: String,
    @Column(name = "file_name", nullable = false, length = 200)
    val fileName: String,
    @Column(name = "file_size", nullable = false)
    val fileSize: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    companion object {
        fun create(
            inquiryId: Long,
            fileUrl: String,
            fileName: String,
            fileSize: Long,
        ): InquiryAttachment {
            return InquiryAttachment(
                inquiryId = inquiryId,
                fileUrl = fileUrl,
                fileName = fileName,
                fileSize = fileSize,
            )
        }
    }
}
