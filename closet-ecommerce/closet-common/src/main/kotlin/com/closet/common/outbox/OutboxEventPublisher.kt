package com.closet.common.outbox

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
) {
    /**
     * Outbox 이벤트를 저장한다.
     * 호출하는 서비스의 비즈니스 트랜잭션 내에서 호출되어야 하며,
     * 동일 트랜잭션으로 묶여 DB-Kafka 원자성을 보장한다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun publish(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        topic: String,
        partitionKey: String,
        payload: String,
    ): OutboxEvent {
        val event =
            OutboxEvent.create(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                topic = topic,
                partitionKey = partitionKey,
                payload = payload,
            )
        return outboxEventRepository.save(event)
    }
}
