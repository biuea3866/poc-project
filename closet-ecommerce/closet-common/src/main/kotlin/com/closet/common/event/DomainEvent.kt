package com.closet.common.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 도메인 이벤트 기본 래퍼.
 * Kafka 메시지로 직렬화/역직렬화되는 표준 포맷.
 */
data class DomainEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val payload: String,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
