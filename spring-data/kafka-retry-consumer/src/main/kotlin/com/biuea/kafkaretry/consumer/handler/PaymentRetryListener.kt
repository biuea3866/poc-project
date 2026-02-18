package com.biuea.kafkaretry.consumer.handler

import com.biuea.kafkaretry.consumer.service.NotificationService
import com.biuea.kafkaretry.consumer.service.OutboxService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.RetryListener
import org.springframework.stereotype.Component

@Component
class PaymentRetryListener(
    private val outboxService: OutboxService,
    private val notificationService: NotificationService
) : RetryListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun failedDelivery(record: ConsumerRecord<*, *>, ex: Exception?, deliveryAttempt: Int) {
        log.warn("[ErrorHandler] Payment retry attempt {} for topic: {}, partition: {}, offset: {}: {}",
            deliveryAttempt, record.topic(), record.partition(), record.offset(), ex?.message)
    }

    override fun recovered(record: ConsumerRecord<*, *>, ex: Exception?) {
        log.error("[ErrorHandler] Payment retries exhausted for offset {}. Saving to outbox and sending notification.",
            record.offset())
        val exception = ex ?: RuntimeException("Unknown error after retries exhausted")
        outboxService.saveFailedMessage(record, exception)
        notificationService.notifyFailure(record, exception)
    }
}
