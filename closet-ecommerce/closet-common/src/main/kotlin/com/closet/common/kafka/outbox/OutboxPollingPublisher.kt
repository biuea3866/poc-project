package com.closet.common.kafka.outbox
import org.springframework.context.annotation.Profile

import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Profile("kafka")
@Component
class OutboxPollingPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_RETRY_COUNT = 3
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun publishPendingEvents() {
        val events = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING,
            PageRequest.of(0, BATCH_SIZE),
        )

        if (events.isEmpty()) return

        logger.info { "Publishing ${events.size} pending outbox events" }

        for (event in events) {
            try {
                val topic = "${event.aggregateType.lowercase()}.events"
                kafkaTemplate.send(topic, event.aggregateId, event.payload).get()
                event.status = OutboxStatus.PUBLISHED
                event.publishedAt = LocalDateTime.now()
                logger.debug { "Published outbox event id=${event.id}, topic=$topic" }
            } catch (e: Exception) {
                event.retryCount++
                if (event.retryCount >= MAX_RETRY_COUNT) {
                    event.status = OutboxStatus.FAILED
                    logger.error(e) { "Outbox event permanently failed after $MAX_RETRY_COUNT retries: id=${event.id}" }
                } else {
                    logger.warn(e) { "Outbox event publish failed (retry=${event.retryCount}): id=${event.id}" }
                }
            }
        }

        outboxRepository.saveAll(events)
    }
}
