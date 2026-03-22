package com.closet.notification.domain.repository

import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationTemplate
import com.closet.notification.domain.NotificationType
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, Long> {
    fun findByTypeAndChannelAndIsActiveTrueAndDeletedAtIsNull(
        type: NotificationType,
        channel: NotificationChannel,
    ): NotificationTemplate?

    fun findByTypeAndChannelAndDeletedAtIsNull(
        type: NotificationType,
        channel: NotificationChannel,
    ): List<NotificationTemplate>

    fun findByIdAndDeletedAtIsNull(id: Long): NotificationTemplate?
}
