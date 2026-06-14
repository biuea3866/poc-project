package com.biuea.batch.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "reserved_notification")
class ReservedNotificationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "notification_type", nullable = false, length = 32)
    val notificationType: String,

    @Column(name = "group_key", nullable = false, length = 128)
    val groupKey: String,

    @Column(name = "payload", nullable = false, length = 512)
    val payload: String,

    @Column(name = "sent", nullable = false)
    val sent: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,
)
