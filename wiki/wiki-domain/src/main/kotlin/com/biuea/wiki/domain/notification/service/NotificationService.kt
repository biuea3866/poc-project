package com.biuea.wiki.domain.notification.service

import com.biuea.wiki.domain.notification.entity.Notification
import com.biuea.wiki.domain.notification.entity.NotificationType
import com.biuea.wiki.infrastructure.notification.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional(readOnly = true)
    fun getNotifications(userId: Long, page: Int = 0, size: Int = 20, unreadOnly: Boolean = false): Page<Notification> {
        val pageable = PageRequest.of(page, size)
        return if (unreadOnly) {
            notificationRepository.findByTargetUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
        } else {
            notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long {
        return notificationRepository.countByTargetUserIdAndIsReadFalse(userId)
    }

    @Transactional
    fun markAsRead(notificationId: Long, userId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notification not found: $notificationId") }
        require(notification.targetUserId == userId) {
            "Not authorized to read notification: $notificationId"
        }
        notification.markRead()
        notificationRepository.save(notification)
    }

    @Transactional
    fun markAllAsRead(userId: Long) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    fun createNotification(
        type: NotificationType,
        targetUserId: Long,
        refId: Long,
        message: String,
    ): Notification {
        val notification = Notification(
            type = type,
            targetUserId = targetUserId,
            refId = refId,
            message = message,
        )
        return notificationRepository.save(notification)
    }
}
