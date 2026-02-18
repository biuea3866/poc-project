package com.biuea.kafkaretry.common.repository

import com.biuea.kafkaretry.common.entity.FailedMessage
import com.biuea.kafkaretry.common.entity.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FailedMessageRepository : JpaRepository<FailedMessage, Long> {
    fun findByStatusAndRetryCountLessThan(status: OutboxStatus, maxRetries: Int): List<FailedMessage>
    fun findByStatusIn(statuses: List<OutboxStatus>): List<FailedMessage>
    fun findByTopicAndStatus(topic: String, status: OutboxStatus): List<FailedMessage>
}
