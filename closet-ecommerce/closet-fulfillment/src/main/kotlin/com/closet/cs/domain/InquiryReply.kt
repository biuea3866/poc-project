package com.closet.cs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.ZonedDateTime

/**
 * 문의 답변
 */
@Entity
@Table(name = "inquiry_reply")
class InquiryReply(
    @Column(name = "inquiry_id", nullable = false)
    val inquiryId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "reply_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val replyType: ReplyType,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    var createdAt: ZonedDateTime = ZonedDateTime.now()

    companion object {
        fun create(
            inquiryId: Long,
            replyType: ReplyType,
            content: String,
        ): InquiryReply {
            return InquiryReply(
                inquiryId = inquiryId,
                replyType = replyType,
                content = content,
            )
        }
    }
}
