package com.closet.common.kafka

import java.time.LocalDateTime
import java.util.UUID

data class DomainEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val payload: String,
)
