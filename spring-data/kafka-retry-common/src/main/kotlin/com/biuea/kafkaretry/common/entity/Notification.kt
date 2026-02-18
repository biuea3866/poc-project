package com.biuea.kafkaretry.common.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val topic: String,

    @Column(nullable = false)
    val eventId: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val level: NotificationLevel,

    @Column(nullable = false, length = 1000)
    val message: String,

    @Column(columnDefinition = "TEXT")
    val detail: String? = null,

    @Column(nullable = false)
    var acknowledged: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class NotificationLevel {
    INFO, WARN, ERROR, CRITICAL
}
