package com.biuea.wiki.notification

import com.biuea.wiki.domain.notification.entity.Notification
import com.biuea.wiki.domain.notification.entity.NotificationType
import com.biuea.wiki.domain.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service

@Service
class NotificationAppService(
    private val notificationService: NotificationService,
    private val notificationSseManager: NotificationSseManager,
) {
    private val logger = LoggerFactory.getLogger(NotificationAppService::class.java)

    fun getNotifications(userId: Long, page: Int = 0, size: Int = 20, unreadOnly: Boolean = false): Page<Notification> {
        return notificationService.getNotifications(userId, page, size, unreadOnly)
    }

    fun getUnreadCount(userId: Long): Long {
        return notificationService.getUnreadCount(userId)
    }

    fun markAsRead(notificationId: Long, userId: Long) {
        notificationService.markAsRead(notificationId, userId)
    }

    fun markAllAsRead(userId: Long) {
        notificationService.markAllAsRead(userId)
    }

    fun createNotification(
        type: NotificationType,
        targetUserId: Long,
        refId: Long,
        message: String,
    ): Notification {
        val notification = notificationService.createNotification(type, targetUserId, refId, message)
        // Push via SSE in real-time
        try {
            notificationSseManager.sendToUser(targetUserId, notification)
        } catch (e: Exception) {
            logger.warn("Failed to push SSE notification to user $targetUserId: ${e.message}")
        }
        return notification
    }
}
