package com.example.notification.infrastructure.entity

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationLogStatus
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationLog
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification_logs")
class NotificationLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val storeId: Long,

    @Column(nullable = false)
    val recipientUserId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val triggerType: NotificationTriggerType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: NotificationChannel,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: NotificationLogStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val priority: NotificationPriority,

    @Column(nullable = false, unique = true)
    val idempotencyKey: String,

    val failureReason: String? = null,
) {
    companion object {
        fun from(log: NotificationLog): NotificationLogEntity = NotificationLogEntity(
            storeId = log.storeId,
            recipientUserId = log.recipientUserId,
            triggerType = log.triggerType,
            channel = log.channel,
            status = log.status,
            priority = log.priority,
            idempotencyKey = log.idempotencyKey,
            failureReason = log.failureReason,
        )
    }
}
