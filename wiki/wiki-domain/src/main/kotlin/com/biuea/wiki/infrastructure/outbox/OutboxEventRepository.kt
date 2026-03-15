package com.biuea.wiki.infrastructure.outbox

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {

    fun findByStatusIn(statuses: List<OutboxStatus>, pageable: Pageable): List<OutboxEvent>

    fun findByStatus(status: OutboxStatus, pageable: Pageable): Page<OutboxEvent>

    fun findAllByStatus(status: OutboxStatus, pageable: Pageable): Page<OutboxEvent>
}
