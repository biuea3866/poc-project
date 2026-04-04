package com.closet.common.idempotency

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessedEventRepository : JpaRepository<ProcessedEvent, Long> {

    fun existsByEventId(eventId: String): Boolean
}
