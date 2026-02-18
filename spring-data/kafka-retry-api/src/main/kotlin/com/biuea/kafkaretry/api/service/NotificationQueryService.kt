package com.biuea.kafkaretry.api.service

import com.biuea.kafkaretry.common.entity.Notification
import com.biuea.kafkaretry.common.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository
) {
    fun getUnacknowledged(): List<Notification> {
        return notificationRepository.findByAcknowledgedFalseOrderByCreatedAtDesc()
    }

    fun getByTopic(topic: String): List<Notification> {
        return notificationRepository.findByTopicOrderByCreatedAtDesc(topic)
    }

    @Transactional
    fun acknowledge(id: Long) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Notification not found: $id") }
        notification.acknowledged = true
        notificationRepository.save(notification)
    }
}
