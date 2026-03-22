package com.closet.common.kafka.idempotency

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "processed_event",
    uniqueConstraints = [
        UniqueConstraint(
            name = "idx_processed_event_unique",
            columnNames = ["eventId", "consumerGroup"],
        ),
    ],
)
class ProcessedEvent(

    @Column(nullable = false, length = 100)
    val eventId: String,

    @Column(nullable = false, length = 100)
    val consumerGroup: String,

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    val processedAt: LocalDateTime = LocalDateTime.now(),

) : BaseEntity()
