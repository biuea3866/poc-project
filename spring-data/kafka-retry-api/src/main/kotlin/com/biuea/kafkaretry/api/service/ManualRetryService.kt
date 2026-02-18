package com.biuea.kafkaretry.api.service

import com.biuea.kafkaretry.common.entity.FailedMessage
import com.biuea.kafkaretry.common.entity.OutboxStatus
import com.biuea.kafkaretry.common.repository.FailedMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ManualRetryService(
    private val failedMessageRepository: FailedMessageRepository,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun retryById(messageId: Long): String {
        val failedMessage = failedMessageRepository.findById(messageId)
            .orElseThrow { IllegalArgumentException("Failed message not found: $messageId") }

        if (failedMessage.status == OutboxStatus.SUCCESS) {
            return "ALREADY_SUCCESS"
        }

        return try {
            failedMessage.retryCount++
            failedMessage.lastRetriedAt = Instant.now()
            failedMessage.status = OutboxStatus.RETRYING

            val targetTopic = failedMessage.originalTopic ?: failedMessage.topic
            val targetKey = failedMessage.messageKey ?: ""
            kafkaTemplate.send(targetTopic, targetKey, failedMessage.payload).get()

            failedMessage.status = OutboxStatus.SUCCESS
            failedMessage.resolvedAt = Instant.now()
            failedMessageRepository.save(failedMessage)
            log.info("[Manual Retry] Successfully retried message id={}", messageId)
            "SUCCESS"
        } catch (exception: Exception) {
            log.error("[Manual Retry] Failed to retry message id={}: {}", messageId, exception.message)
            if (failedMessage.retryCount >= failedMessage.maxRetries) {
                failedMessage.status = OutboxStatus.EXHAUSTED
            } else {
                failedMessage.status = OutboxStatus.PENDING
            }
            failedMessageRepository.save(failedMessage)
            "FAILED: ${exception.message}"
        }
    }

    fun listFailedMessages(status: OutboxStatus?): List<FailedMessage> {
        return if (status != null) {
            failedMessageRepository.findByStatusIn(listOf(status))
        } else {
            failedMessageRepository.findByStatusIn(listOf(OutboxStatus.PENDING, OutboxStatus.EXHAUSTED))
        }
    }

    @Transactional
    fun retryAllPendingMessages(): Int {
        val pendingMessages = failedMessageRepository.findByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 5)
        var successCount = 0

        pendingMessages.forEach { failedMessage ->
            try {
                failedMessage.retryCount++
                failedMessage.lastRetriedAt = Instant.now()
                failedMessage.status = OutboxStatus.RETRYING

                val targetTopic = failedMessage.originalTopic ?: failedMessage.topic
                val targetKey = failedMessage.messageKey ?: ""
                kafkaTemplate.send(targetTopic, targetKey, failedMessage.payload).get()

                failedMessage.status = OutboxStatus.SUCCESS
                failedMessage.resolvedAt = Instant.now()
                successCount++
            } catch (exception: Exception) {
                log.error("[Manual Retry All] Failed for message id={}: {}", failedMessage.id, exception.message)
                if (failedMessage.retryCount >= failedMessage.maxRetries) {
                    failedMessage.status = OutboxStatus.EXHAUSTED
                } else {
                    failedMessage.status = OutboxStatus.PENDING
                }
            }
            failedMessageRepository.save(failedMessage)
        }

        log.info("[Manual Retry All] Retried {}/{} messages successfully", successCount, pendingMessages.size)
        return successCount
    }
}
