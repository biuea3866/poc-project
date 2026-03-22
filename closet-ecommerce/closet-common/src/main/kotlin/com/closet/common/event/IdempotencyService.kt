package com.closet.common.event

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * 멱등성 보장 서비스.
 * 동일한 이벤트가 중복 처리되지 않도록 보장한다.
 */
@Service
class IdempotencyService(
    private val processedEventRepository: ProcessedEventRepository,
) {

    fun isProcessed(eventId: String, consumerGroup: String): Boolean {
        return processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)
    }

    @Transactional
    fun markProcessed(eventId: String, consumerGroup: String) {
        if (isProcessed(eventId, consumerGroup)) {
            logger.debug { "이미 처리된 이벤트: eventId=$eventId, consumerGroup=$consumerGroup" }
            return
        }
        processedEventRepository.save(ProcessedEvent(eventId = eventId, consumerGroup = consumerGroup))
        logger.debug { "이벤트 처리 완료 기록: eventId=$eventId, consumerGroup=$consumerGroup" }
    }
}
