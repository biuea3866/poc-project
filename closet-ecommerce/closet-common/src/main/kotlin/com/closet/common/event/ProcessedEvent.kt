package com.closet.common.event

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

    @Column(name = "event_id", nullable = false, length = 100)
    val eventId: String,

    @Column(name = "consumer_group", nullable = false, length = 100)
    val consumerGroup: String,

    @Column(name = "processed_at", nullable = false, columnDefinition = "DATETIME(6)")
    val processedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
}
