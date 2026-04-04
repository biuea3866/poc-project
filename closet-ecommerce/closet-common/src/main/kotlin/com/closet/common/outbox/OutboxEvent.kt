package com.closet.common.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "outbox_event")
@EntityListeners(AuditingEntityListener::class)
class OutboxEvent(
    @Column(name = "aggregate_type", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    val eventType: String,

    @Column(name = "topic", nullable = false, length = 200, columnDefinition = "VARCHAR(200)")
    val topic: String,

    @Column(name = "partition_key", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    val partitionKey: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    var status: OutboxEventStatus = OutboxEventStatus.PENDING,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    var publishedAt: LocalDateTime? = null

    fun markPublished() {
        status.validateTransitionTo(OutboxEventStatus.PUBLISHED)
        this.status = OutboxEventStatus.PUBLISHED
        this.publishedAt = LocalDateTime.now()
    }

    fun markFailed() {
        status.validateTransitionTo(OutboxEventStatus.FAILED)
        this.status = OutboxEventStatus.FAILED
    }

    companion object {
        fun create(
            aggregateType: String,
            aggregateId: String,
            eventType: String,
            topic: String,
            partitionKey: String,
            payload: String,
        ): OutboxEvent {
            return OutboxEvent(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                topic = topic,
                partitionKey = partitionKey,
                payload = payload,
            )
        }
    }
}
