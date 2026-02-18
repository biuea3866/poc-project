package com.biuea.kafkaretry.consumer.service

import com.biuea.kafkaretry.common.entity.Notification
import com.biuea.kafkaretry.common.entity.NotificationLevel
import com.biuea.kafkaretry.common.repository.NotificationRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun notifyFailure(record: ConsumerRecord<*, *>, ex: Exception) {
        val notification = Notification(
            topic = record.topic(),
            eventId = record.key()?.toString() ?: "unknown",
            level = NotificationLevel.ERROR,
            message = "Message processing failed after all retries: ${ex.message}".take(1000),
            detail = "Topic: ${record.topic()}, Partition: ${record.partition()}, Offset: ${record.offset()}"
        )
        notificationRepository.save(notification)
        log.info("Notification saved: topic={}, eventId={}, level={}", notification.topic, notification.eventId, notification.level)
    }
}
