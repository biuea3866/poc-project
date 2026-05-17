package com.hrplatform.employee.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventEnvelope
import com.hrplatform.core.event.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(KafkaTemplate::class)
class KafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${hrplatform.kafka.topics.employee:event.hr.employee.v1}")
    private val employeeTopic: String,
) : DomainEventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        val envelope = DomainEventEnvelope.from(event)
        val payload = objectMapper.writeValueAsString(envelope)
        val partitionKey = event.aggregateId.toString()
        kafkaTemplate.send(employeeTopic, partitionKey, payload)
        logger.info("Published {} for aggregate {} to {}", event.eventType, event.aggregateId, employeeTopic)
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
