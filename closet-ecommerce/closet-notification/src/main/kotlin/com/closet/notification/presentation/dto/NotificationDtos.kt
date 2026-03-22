package com.closet.notification.presentation.dto

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/** 알림 발송 요청 (관리자용) */
data class SendNotificationRequest(
    @field:NotNull(message = "회원 ID는 필수입니다")
    val memberId: Long,

    @field:NotNull(message = "알림 유형은 필수입니다")
    val type: NotificationType,

    @field:NotNull(message = "알림 채널은 필수입니다")
    val channel: NotificationChannel,

    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,
)

/** 알림 응답 */
data class NotificationResponse(
    val id: Long,
    val memberId: Long,
    val channel: NotificationChannel,
    val type: NotificationType,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val sentAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = notification.id,
            memberId = notification.memberId,
            channel = notification.channel,
            type = notification.type,
            title = notification.title,
            content = notification.content,
            isRead = notification.isRead,
            sentAt = notification.sentAt,
            readAt = notification.readAt,
            createdAt = notification.createdAt,
        )
    }
}

/** 미읽음 카운트 응답 */
data class UnreadCountResponse(
    val count: Long,
)
