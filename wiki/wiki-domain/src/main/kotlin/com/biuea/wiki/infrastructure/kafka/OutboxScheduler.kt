package com.biuea.wiki.infrastructure.kafka

import com.biuea.wiki.domain.outbox.OutboxService
import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 미처리 outbox 이벤트를 주기적으로 Kafka에 재발행하는 스케줄러
 *
 * 재발행 시 X-Outbox-Event-Id 헤더를 포함하여 컨슈머가
 * 처리 결과를 outbox 테이블에 반영할 수 있도록 한다.
 */
@Component
class OutboxScheduler(
    private val outboxService: OutboxService,
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 50
        const val HEADER_OUTBOX_EVENT_ID = "X-Outbox-Event-Id"
    }

    @Scheduled(fixedDelay = 60_000) // 1분 간격
    fun publishPendingOutboxEvents() {
        val events = outboxService.findPendingAndFailed(BATCH_SIZE)
        if (events.isEmpty()) return

        log.info("[OUTBOX-SCHEDULER] 미처리 이벤트 {}건 재발행 시작", events.size)
        var successCount = 0

        for (event in events) {
            try {
                republish(event)
                outboxService.markSuccess(event)
                successCount++
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                log.error("[OUTBOX-SCHEDULER] 재발행 실패 id={} error={}", event.id, errorMsg)
                outboxService.markFailed(event, errorMsg)
            }
        }

        log.info("[OUTBOX-SCHEDULER] 재발행 완료 success={}/{}", successCount, events.size)
    }

    private fun republish(event: OutboxEvent) {
        val payload = objectMapper.readValue(event.payload, Map::class.java)
        val record = ProducerRecord<Any, Any>(event.topic, null, event.aggregateId, payload).apply {
            headers().add(RecordHeader(HEADER_OUTBOX_EVENT_ID, event.id.toString().toByteArray()))
        }
        kafkaTemplate.send(record).get() // 동기 대기
    }
}
