package com.closet.common.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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

    @Column(name = "aggregate_type", nullable = false, length = 100)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 100)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 100)
    val eventType: String,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(name = "status", nullable = false, length = 30)
    var status: String = "PENDING",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    var publishedAt: LocalDateTime? = null

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    fun markPublished() {
        this.status = "PUBLISHED"
        this.publishedAt = LocalDateTime.now()
    }

    fun incrementRetry() {
        this.retryCount++
    }
}
