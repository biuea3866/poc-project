package com.biuea.springdata.kafka.dto

import java.time.Instant
import java.util.UUID

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val type: EventType,
    val payload: Map<String, Any> = emptyMap()
)

enum class EventType {
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ORDER_PLACED,
    ORDER_SHIPPED,
    PAYMENT_PROCESSED,
    NOTIFICATION_SENT
}
