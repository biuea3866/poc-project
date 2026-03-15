package com.biuea.wiki.presentation.admin.outbox.response

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import java.time.LocalDateTime

data class OutboxEventResponse(
    val id: Long,
    val aggregateType: String,
    val aggregateId: String,
    val topic: String,
    val payload: String,
    val status: OutboxStatus,
    val retryCount: Int,
    val maxRetries: Int,
    val processedAt: LocalDateTime?,
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(event: OutboxEvent): OutboxEventResponse {
            return OutboxEventResponse(
                id = event.id,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                topic = event.topic,
                payload = event.payload,
                status = event.status,
                retryCount = event.retryCount,
                maxRetries = event.maxRetries,
                processedAt = event.processedAt,
                errorMessage = event.errorMessage,
                createdAt = event.createdAt,
                updatedAt = event.updatedAt,
            )
        }
    }
}
