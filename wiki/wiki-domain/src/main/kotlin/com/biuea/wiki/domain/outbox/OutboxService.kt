package com.biuea.wiki.domain.outbox

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import com.biuea.wiki.infrastructure.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

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
        if (event.status == OutboxStatus.DEAD_LETTER) {
            log.warn(
                "[OUTBOX] DEAD_LETTER 전이 — id={} aggregateType={} aggregateId={} topic={} retryCount={} error={}",
                event.id,
                event.aggregateType,
                event.aggregateId,
                event.topic,
                event.retryCount,
                error,
            )
        }
        outboxEventRepository.save(event)
    }

    /**
     * wiki-worker 컨슈머에서 이벤트 ID로 성공 처리.
     * outbox 이벤트가 존재하지 않으면 no-op (직접 발행된 메시지일 수 있음).
     */
    @Transactional
    fun markSuccessById(eventId: Long) {
        outboxEventRepository.findById(eventId).ifPresent { event ->
            event.markSuccess()
            outboxEventRepository.save(event)
            log.info("[OUTBOX] 컨슈머 처리 성공 id={} topic={}", event.id, event.topic)
        }
    }

    /**
     * wiki-worker 컨슈머에서 이벤트 ID로 실패 처리.
     * outbox 이벤트가 존재하지 않으면 no-op (직접 발행된 메시지일 수 있음).
     */
    @Transactional
    fun markFailedById(eventId: Long, error: String) {
        outboxEventRepository.findById(eventId).ifPresent { event ->
            markFailed(event, error)
        }
    }

    @Transactional
    fun resetForRetry(id: Long): OutboxEvent {
        val event = findById(id)
        event.resetForRetry()
        return outboxEventRepository.save(event)
    }
}
