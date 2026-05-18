package com.hrplatform.auth.infrastructure.kafka

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
class AuthKafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${hrplatform.kafka.topics.auth:event.hr.auth.v1}")
    private val authTopic: String,
) : DomainEventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        require(event.aggregateType == "UserAccount") {
            "AuthKafkaDomainEventPublisher는 UserAccount 이벤트만 발행합니다. 실제 aggregateType: ${event.aggregateType}"
        }
        val envelope = DomainEventEnvelope.from(event)
        val payload = objectMapper.writeValueAsString(envelope)
        val partitionKey = event.aggregateId.toString()
        kafkaTemplate.send(authTopic, partitionKey, payload)
        logger.info("Published {} for aggregate {} to {}", event.eventType, event.aggregateId, authTopic)
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
