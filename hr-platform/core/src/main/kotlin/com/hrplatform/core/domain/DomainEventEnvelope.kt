package com.hrplatform.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.hrplatform.core.util.ZonedDateTimes

/**
 * JSON 직렬화 헬퍼. Kafka 발행 시 KafkaDomainEventPublisher가 이 envelope으로 변환합니다.
 */
data class DomainEventEnvelope(
    val eventId: String,
    val eventType: String,
    val eventVersion: Int,
    val occurredAt: String,                 // ISO-8601 마이크로초 문자열
    val aggregateType: String,
    val aggregateId: Long,
    val companyId: Long,
    val actorEmploymentId: Long?,
    val action: ActionEnvelope,
    val state: StateEnvelope,
) {
    data class ActionEnvelope(
        val type: String,
        val details: Map<String, Any?>,
    )

    data class StateEnvelope(
        val status: String,
        @JsonProperty("snapshot") val snapshot: Map<String, Any?>,
    )

    companion object {
        fun from(event: DomainEvent): DomainEventEnvelope = DomainEventEnvelope(
            eventId = event.eventId.toString(),
            eventType = event.eventType,
            eventVersion = event.eventVersion,
            occurredAt = ZonedDateTimes.toIso8601(event.occurredAt),
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            companyId = event.companyId,
            actorEmploymentId = event.actorEmploymentId,
            action = ActionEnvelope(event.action.type, event.action.details),
            state = StateEnvelope(event.state.status, event.state.snapshot),
        )
    }
}
