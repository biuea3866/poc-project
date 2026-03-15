package com.biuea.wiki.domain.outbox.entity

import com.biuea.wiki.domain.common.BaseTimeEntity
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
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_event_status", columnList = "status"),
        Index(name = "idx_outbox_event_status_created_at", columnList = "status, created_at"),
    ]
)
class OutboxEvent(
    @Column(name = "aggregate_type", nullable = false, length = 100)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    @Column(name = "topic", nullable = false, length = 200)
    val topic: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "max_retries", nullable = false)
    val maxRetries: Int = 5,

    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {

    fun markSuccess() {
        this.status = OutboxStatus.SUCCESS
        this.processedAt = LocalDateTime.now()
        this.errorMessage = null
    }

    fun markFailed(error: String) {
        this.retryCount++
        this.errorMessage = error
        if (this.retryCount >= this.maxRetries) {
            this.status = OutboxStatus.DEAD_LETTER
        } else {
            this.status = OutboxStatus.FAILED
        }
    }

    fun resetForRetry() {
        this.status = OutboxStatus.PENDING
        this.errorMessage = null
    }

    companion object {
        fun create(
            aggregateType: String,
            aggregateId: String,
            topic: String,
            payload: String,
        ): OutboxEvent {
            return OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                topic = topic,
                payload = payload,
            )
        }
    }
}
