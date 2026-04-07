package com.closet.common.outbox

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByStatusOrderByCreatedAtAsc(
        status: OutboxEventStatus,
        pageable: Pageable,
    ): List<OutboxEvent>
}
