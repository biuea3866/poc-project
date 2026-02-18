package com.biuea.kafkaretry.consumer.service

import com.biuea.kafkaretry.common.entity.FailedMessage
import com.biuea.kafkaretry.common.entity.OutboxStatus
import com.biuea.kafkaretry.common.repository.FailedMessageRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.PrintWriter
import java.io.StringWriter

@Service
class OutboxService(
    private val failedMessageRepository: FailedMessageRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun saveFailedMessage(record: ConsumerRecord<*, *>, ex: Exception) {
        val sw = StringWriter()
        ex.printStackTrace(PrintWriter(sw))

        val failedMessage = FailedMessage(
            topic = record.topic(),
            messageKey = record.key()?.toString(),
            payload = record.value()?.toString() ?: "",
            originalTopic = deriveOriginalTopic(record.topic()),
            errorMessage = (ex.message ?: "Unknown error").take(1000),
            stackTrace = sw.toString().take(4000),
            status = OutboxStatus.PENDING
        )
        failedMessageRepository.save(failedMessage)
        log.info("Saved failed message to outbox: topic={}, key={}, id={}", record.topic(), record.key(), failedMessage.id)
    }

    private fun deriveOriginalTopic(topic: String): String {
        return topic
            .replace(Regex("-dlt$"), "")
            .replace(Regex("-retry-\\d+$"), "")
            .replace(Regex("\\.DLT$"), "")
    }
}
