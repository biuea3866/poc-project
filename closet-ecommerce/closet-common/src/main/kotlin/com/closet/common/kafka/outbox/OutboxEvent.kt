package com.closet.common.kafka.outbox

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_status_created", columnList = "status, createdAt"),
    ],
)
class OutboxEvent(

    @Column(nullable = false, length = 100)
    val aggregateType: String,

    @Column(nullable = false, length = 100)
    val aggregateId: String,

    @Column(nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OutboxStatus = OutboxStatus.PENDING,

    @Column(columnDefinition = "DATETIME(6)")
    var publishedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var retryCount: Int = 0,

) : BaseEntity()
