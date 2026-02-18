package com.biuea.kafkaretry.common.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "failed_messages")
class FailedMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val topic: String,

    @Column(name = "message_key")
    val messageKey: String? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    val payload: String,

    @Column(nullable = false, length = 1000)
    val errorMessage: String,

    @Column(name = "original_topic")
    val originalTopic: String? = null,

    @Column(columnDefinition = "TEXT")
    val stackTrace: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(nullable = false)
    var retryCount: Int = 0,

    @Column(nullable = false)
    val maxRetries: Int = 5,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    var lastRetriedAt: Instant? = null,

    var resolvedAt: Instant? = null
)

enum class OutboxStatus {
    PENDING, RETRYING, SUCCESS, EXHAUSTED
}
