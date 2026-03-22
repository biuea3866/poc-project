package com.closet.common.kafka.idempotency

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessedEventRepository : JpaRepository<ProcessedEvent, Long> {
    fun existsByEventIdAndConsumerGroup(eventId: String, consumerGroup: String): Boolean
}
