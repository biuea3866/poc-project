package com.closet.common.kafka.idempotency

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class IdempotencyService(
    private val processedEventRepository: ProcessedEventRepository,
) {

    @Transactional(readOnly = true)
    fun isProcessed(eventId: String, consumerGroup: String): Boolean {
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)
    }

    @Transactional
    fun markProcessed(eventId: String, consumerGroup: String) {
        if (isProcessed(eventId, consumerGroup)) {
            logger.debug { "Event already processed: eventId=$eventId, consumerGroup=$consumerGroup" }
            return
        }
        processedEventRepository.save(ProcessedEvent(eventId = eventId, consumerGroup = consumerGroup))
        logger.info { "Marked event as processed: eventId=$eventId, consumerGroup=$consumerGroup" }
    }
}
