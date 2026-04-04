package com.closet.common.outbox

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC"
    )
    fun findByStatusForUpdate(status: OutboxEventStatus, pageable: Pageable): List<OutboxEvent>
}
