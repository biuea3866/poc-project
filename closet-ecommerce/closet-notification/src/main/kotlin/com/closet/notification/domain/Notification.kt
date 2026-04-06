package com.closet.notification.domain

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

/**
 * 알림 Aggregate Root
 */
@Entity
@Table(name = "notification")
class Notification(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val channel: NotificationChannel,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val type: NotificationType,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
    @Column(name = "sent_at", nullable = false, columnDefinition = "DATETIME(6)")
    val sentAt: ZonedDateTime = ZonedDateTime.now(),
    @Column(name = "read_at", columnDefinition = "DATETIME(6)")
    var readAt: ZonedDateTime? = null,
) : BaseEntity() {
    companion object {
        /** 알림 생성 팩토리 메서드 */
        fun create(
            memberId: Long,
            channel: NotificationChannel,
            type: NotificationType,
            title: String,
            content: String,
        ): Notification {
            return Notification(
                memberId = memberId,
                channel = channel,
                type = type,
                title = title,
                content = content,
                isRead = false,
                sentAt = ZonedDateTime.now(),
            )
        }
    }

    /** 읽음 처리 */
    fun markAsRead() {
        if (!this.isRead) {
            this.isRead = true
            this.readAt = ZonedDateTime.now()
        }
    }
}
