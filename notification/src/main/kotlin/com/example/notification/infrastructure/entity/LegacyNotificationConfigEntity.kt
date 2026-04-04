package com.example.notification.infrastructure.entity

import com.example.notification.legacy.LegacyNotificationConfig
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "legacy_notification_configs")
class LegacyNotificationConfigEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true)
    val userId: Long,

    @Column(nullable = false)
    val emailEnabled: Boolean = true,

    @Column(nullable = false)
    val pushEnabled: Boolean = true,

    @Column(nullable = false)
    val smsEnabled: Boolean = true,

    @Column(nullable = false)
    val inAppEnabled: Boolean = true,
) {
    fun toDomain(): LegacyNotificationConfig = LegacyNotificationConfig(
        userId = userId,
        emailEnabled = emailEnabled,
        pushEnabled = pushEnabled,
        smsEnabled = smsEnabled,
        inAppEnabled = inAppEnabled,
    )
}
