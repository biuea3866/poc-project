package com.closet.common.kafka.outbox
import org.springframework.context.annotation.Profile

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Profile("kafka")
@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun save(aggregateType: String, aggregateId: String, eventType: String, payload: Any) {
        val jsonPayload = objectMapper.writeValueAsString(payload)
        val event = OutboxEvent(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            payload = jsonPayload,
        )
        outboxRepository.save(event)
        logger.info { "Outbox event saved: aggregateType=$aggregateType, aggregateId=$aggregateId, eventType=$eventType" }
    }
}
