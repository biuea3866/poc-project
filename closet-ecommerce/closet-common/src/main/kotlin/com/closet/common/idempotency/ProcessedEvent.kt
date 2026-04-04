package com.closet.common.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "processed_event")
class ProcessedEvent(

    @Column(name = "event_id", nullable = false, length = 100, unique = true)
    val eventId: String,

    @Column(name = "topic", nullable = false, length = 200)
    val topic: String,

    @Column(name = "consumer_group", nullable = false, length = 100)
    val consumerGroup: String,

    @Column(name = "processed_at", nullable = false, columnDefinition = "DATETIME(6)")
    val processedAt: LocalDateTime = LocalDateTime.now(),
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    companion object {
        fun create(eventId: String, topic: String, consumerGroup: String): ProcessedEvent {
            return ProcessedEvent(
                eventId = eventId,
                topic = topic,
                consumerGroup = consumerGroup,
                processedAt = LocalDateTime.now(),
            )
        }
    }
}
