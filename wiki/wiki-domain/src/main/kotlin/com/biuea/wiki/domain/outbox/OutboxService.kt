package com.biuea.wiki.domain.outbox

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import com.biuea.wiki.infrastructure.outbox.OutboxEventRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun save(event: OutboxEvent): OutboxEvent {
        return outboxEventRepository.save(event)
    }

    @Transactional(readOnly = true)
    fun findPendingAndFailed(batchSize: Int): List<OutboxEvent> {
        val pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending())
        return outboxEventRepository.findByStatusIn(
            listOf(OutboxStatus.PENDING, OutboxStatus.FAILED),
            pageable,
        )
    }

    @Transactional(readOnly = true)
    fun findByStatus(status: OutboxStatus, pageable: Pageable): Page<OutboxEvent> {
        return outboxEventRepository.findAllByStatus(status, pageable)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<OutboxEvent> {
        return outboxEventRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): OutboxEvent {
        return outboxEventRepository.findById(id)
            .orElseThrow { IllegalArgumentException("OutboxEvent not found: $id") }
    }

    @Transactional
    fun markSuccess(event: OutboxEvent) {
        event.markSuccess()
        outboxEventRepository.save(event)
    }

    @Transactional
    fun markFailed(event: OutboxEvent, error: String) {
        event.markFailed(error)
        outboxEventRepository.save(event)
    }

    @Transactional
    fun resetForRetry(id: Long): OutboxEvent {
        val event = findById(id)
        event.resetForRetry()
        return outboxEventRepository.save(event)
    }
}
