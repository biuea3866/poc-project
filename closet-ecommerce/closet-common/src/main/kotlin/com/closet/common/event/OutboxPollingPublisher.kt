package com.closet.common.event

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Outbox 폴링 발행기.
 * 주기적으로 PENDING 상태의 Outbox 이벤트를 Kafka로 발행한다.
 */
@Component
class OutboxPollingPublisher(
    private val outboxService: OutboxService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    @Scheduled(fixedDelay = 1000)
    fun publishPendingEvents() {
        val pendingEvents = outboxService.findPendingEvents()
        if (pendingEvents.isEmpty()) return

        pendingEvents.forEach { outboxEvent ->
            try {
                val topic = resolveTopicFor(outboxEvent.aggregateType)
                val domainEvent = DomainEvent(
                    eventType = outboxEvent.eventType,
                    aggregateType = outboxEvent.aggregateType,
                    aggregateId = outboxEvent.aggregateId,
                    payload = outboxEvent.payload,
                )
                val message = objectMapper.writeValueAsString(domainEvent)
                kafkaTemplate.send(topic, outboxEvent.aggregateId, message)
                outboxService.markPublished(outboxEvent)

                logger.info { "Outbox 이벤트 발행: topic=$topic, type=${outboxEvent.eventType}, aggregateId=${outboxEvent.aggregateId}" }
            } catch (e: Exception) {
                logger.error(e) { "Outbox 이벤트 발행 실패: id=${outboxEvent.id}, type=${outboxEvent.eventType}" }
                outboxService.incrementRetry(outboxEvent)
            }
        }
    }

    private fun resolveTopicFor(aggregateType: String): String {
        return when (aggregateType) {
            "Order" -> KafkaTopics.ORDER_EVENTS
            "Inventory" -> KafkaTopics.INVENTORY_EVENTS
            "Payment" -> KafkaTopics.PAYMENT_EVENTS
            else -> throw IllegalArgumentException("Unknown aggregate type: $aggregateType")
        }
    }
}
