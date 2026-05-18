package com.hrplatform.employee.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 트랜잭션 커밋 후 DomainEvent를 Kafka로 전송하는 리스너.
 * SpringApplicationEventDomainEventPublisher가 발행한 DomainEventApplicationEvent를
 * @TransactionalEventListener(AFTER_COMMIT)로 수신해 Kafka로 전송한다.
 * 트랜잭션이 롤백되면 이 리스너는 호출되지 않으므로 DB 롤백 시 이벤트 누출이 없다.
 */
@Component
@ConditionalOnBean(KafkaTemplate::class)
class KafkaDomainEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${hrplatform.kafka.topics.employee:event.hr.employee.v1}")
    private val employeeTopic: String,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDomainEvent(applicationEvent: DomainEventApplicationEvent) {
        publish(applicationEvent.domainEvent)
    }

    fun publish(event: DomainEvent) {
        val envelope = DomainEventEnvelope.from(event)
        val payload = objectMapper.writeValueAsString(envelope)
        val partitionKey = event.aggregateId.toString()
        kafkaTemplate.send(employeeTopic, partitionKey, payload)
        logger.info("Published {} for aggregate {} to {}", event.eventType, event.aggregateId, employeeTopic)
    }

    fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
