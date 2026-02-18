package com.biuea.kafkaretry.common.repository

import com.biuea.kafkaretry.common.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByAcknowledgedFalseOrderByCreatedAtDesc(): List<Notification>
    fun findByTopicOrderByCreatedAtDesc(topic: String): List<Notification>
}
