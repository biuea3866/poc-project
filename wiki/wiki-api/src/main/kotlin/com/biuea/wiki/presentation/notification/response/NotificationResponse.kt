package com.biuea.wiki.presentation.notification.response

import com.biuea.wiki.domain.notification.entity.Notification
import com.biuea.wiki.domain.notification.entity.NotificationType

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val targetUserId: Long,
    val refId: Long,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
    val readAt: String?,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = notification.id,
            type = notification.type,
            targetUserId = notification.targetUserId,
            refId = notification.refId,
            message = notification.message,
            isRead = notification.isRead,
            createdAt = notification.createdAt.toString(),
            readAt = notification.readAt?.toString(),
        )
    }
}

data class NotificationPageResponse(
    val content: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class UnreadCountResponse(
    val count: Long,
)
