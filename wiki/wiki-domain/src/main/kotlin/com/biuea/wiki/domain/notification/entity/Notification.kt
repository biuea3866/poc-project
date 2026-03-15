package com.biuea.wiki.domain.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notification_user", columnList = "target_user_id"),
        Index(name = "idx_notification_read", columnList = "target_user_id, is_read"),
    ]
)
class Notification(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    @Column(name = "target_user_id", nullable = false)
    val targetUserId: Long,

    @Column(name = "ref_id", nullable = false)
    val refId: Long,

    @Column(length = 500)
    val message: String = "",

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) {
    fun markRead() {
        this.isRead = true
        this.readAt = LocalDateTime.now()
    }
}
