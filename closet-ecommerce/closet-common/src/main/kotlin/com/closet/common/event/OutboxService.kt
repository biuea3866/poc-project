package com.closet.common.event

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Transactional Outbox 서비스.
 * 도메인 트랜잭션과 동일한 트랜잭션에서 이벤트를 outbox 테이블에 저장한다.
 */
@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun save(aggregateType: String, aggregateId: String, eventType: String, payload: Any): OutboxEvent {
        val payloadJson = objectMapper.writeValueAsString(payload)
        val event = OutboxEvent(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            payload = payloadJson,
        )
        val saved = outboxRepository.save(event)
        logger.debug { "Outbox 이벤트 저장: type=$eventType, aggregateId=$aggregateId" }
        return saved
    }

    fun findPendingEvents(): List<OutboxEvent> {
        return outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")
    }

    @Transactional
    fun markPublished(event: OutboxEvent) {
        event.markPublished()
        outboxRepository.save(event)
    }

    @Transactional
    fun incrementRetry(event: OutboxEvent) {
        event.incrementRetry()
        outboxRepository.save(event)
    }
}
