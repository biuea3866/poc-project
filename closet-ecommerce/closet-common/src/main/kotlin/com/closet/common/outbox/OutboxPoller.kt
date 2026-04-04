package com.closet.common.outbox

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(
    name = ["outbox.polling.enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class OutboxPoller(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    companion object {
        private const val BATCH_SIZE = 100
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun poll() {
        val pendingEvents = outboxEventRepository.findByStatusForUpdate(
            status = OutboxEventStatus.PENDING,
            pageable = PageRequest.of(0, BATCH_SIZE),
        )

        if (pendingEvents.isEmpty()) return

        logger.info { "Outbox 폴링: ${pendingEvents.size}건 처리 시작" }

        for (event in pendingEvents) {
            try {
                kafkaTemplate.send(event.topic, event.partitionKey, event.payload).get()
                event.markPublished()
                logger.debug { "Outbox 이벤트 발행 성공: id=${event.id}, topic=${event.topic}, eventType=${event.eventType}" }
            } catch (e: Exception) {
                event.markFailed()
                logger.error(e) { "Outbox 이벤트 발행 실패: id=${event.id}, topic=${event.topic}, eventType=${event.eventType}" }
            }
        }

        outboxEventRepository.saveAll(pendingEvents)
        logger.info { "Outbox 폴링: ${pendingEvents.size}건 처리 완료" }
    }
}
