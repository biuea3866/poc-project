package com.biuea.wiki.infrastructure.kafka

import com.biuea.wiki.domain.outbox.OutboxService
import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Kafka 발행 래퍼 — 발행 실패 시 outbox 테이블에 적재
 */
@Component
class OutboxKafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val outboxService: OutboxService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Kafka 발행을 시도하고, 실패 시 outbox에 적재한다.
     * 이 메서드는 호출자의 트랜잭션 안에서 실행되어야 한다.
     */
    @Transactional
    fun publish(
        topic: String,
        key: String,
        payload: Any,
        aggregateType: String,
        aggregateId: String,
    ) {
        val jsonPayload = objectMapper.writeValueAsString(payload)

        try {
            val future = kafkaTemplate.send(topic, key, payload)
            future.get() // 동기 대기 — 발행 성공 확인
            log.info("[OUTBOX-PUBLISHER] Kafka 발행 성공 topic={} key={}", topic, key)
        } catch (e: Exception) {
            log.warn("[OUTBOX-PUBLISHER] Kafka 발행 실패, outbox 적재 topic={} key={} error={}", topic, key, e.message)
            outboxService.save(
                OutboxEvent.create(
                    aggregateType = aggregateType,
                    aggregateId = aggregateId,
                    topic = topic,
                    payload = jsonPayload,
                )
            )
        }
    }
}
