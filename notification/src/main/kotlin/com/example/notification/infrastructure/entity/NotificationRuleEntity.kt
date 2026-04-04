package com.example.notification.infrastructure.entity

import com.example.notification.domain.enums.Frequency
import com.example.notification.domain.enums.NotificationCategory
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.enums.ScopeType
import com.example.notification.domain.model.NotificationRule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification_rules")
class NotificationRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false)
    val storeId: Long,

    val userId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val scopeType: ScopeType,

    val scopeId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: NotificationCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val triggerType: NotificationTriggerType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val channel: NotificationChannel,

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val priority: NotificationPriority = NotificationPriority.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val frequency: Frequency = Frequency.IMMEDIATE,
) {
    fun toDomain(): NotificationRule = NotificationRule(
        id = id,
        storeId = storeId,
        userId = userId,
        scopeType = scopeType,
        scopeId = scopeId,
        category = category,
        triggerType = triggerType,
        channel = channel,
        enabled = enabled,
        priority = priority,
        frequency = frequency,
    )
}
