package com.closet.common.kafka
import org.springframework.context.annotation.Profile

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Profile("kafka")
@Component
class KafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : DomainEventPublisher {

    override fun publish(topic: String, event: DomainEvent) {
        val json = objectMapper.writeValueAsString(event)
        logger.info { "Publishing event to topic=$topic, eventId=${event.eventId}, aggregateId=${event.aggregateId}" }
        kafkaTemplate.send(topic, event.aggregateId, json)
    }
}
