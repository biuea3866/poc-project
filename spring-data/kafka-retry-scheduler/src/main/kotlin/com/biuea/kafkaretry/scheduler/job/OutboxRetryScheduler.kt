package com.biuea.kafkaretry.scheduler.job

import com.biuea.kafkaretry.common.entity.OutboxStatus
import com.biuea.kafkaretry.common.repository.FailedMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxRetryScheduler(
    private val failedMessageRepository: FailedMessageRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${retry.scheduler.interval-ms:30000}")
    @Transactional
    fun retryFailedMessages() {
        val pendingMessages = failedMessageRepository.findByStatusAndRetryCountLessThan(
            OutboxStatus.PENDING, 5
        )

        if (pendingMessages.isEmpty()) return

        log.info("[Outbox Scheduler] Found {} failed messages to retry", pendingMessages.size)

        pendingMessages.forEach { failedMessage ->
            try {
                failedMessage.status = OutboxStatus.RETRYING
                failedMessage.retryCount++
                failedMessage.lastRetriedAt = Instant.now()

                val targetTopic = failedMessage.originalTopic ?: failedMessage.topic
                val targetKey = failedMessage.messageKey ?: ""
                kafkaTemplate.send(targetTopic, targetKey, failedMessage.payload).get()

                failedMessage.status = OutboxStatus.SUCCESS
                failedMessage.resolvedAt = Instant.now()
                log.info("[Outbox Scheduler] Successfully retried message id={} to topic={} (original={})",
                    failedMessage.id, targetTopic, failedMessage.originalTopic)
            } catch (exception: Exception) {
                log.error("[Outbox Scheduler] Retry failed for message id={}: {}",
                    failedMessage.id, exception.message)
                if (failedMessage.retryCount >= failedMessage.maxRetries) {
                    failedMessage.status = OutboxStatus.EXHAUSTED
                    log.warn("[Outbox Scheduler] Message id={} exhausted all {} retries",
                        failedMessage.id, failedMessage.maxRetries)
                } else {
                    failedMessage.status = OutboxStatus.PENDING
                }
            }
            failedMessageRepository.save(failedMessage)
        }
    }
}
