package com.hrplatform.auth.domain.account.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.ZonedDateTime
import java.util.UUID

data class UserDeactivatedEvent(
    val userAccountId: Long,
    val companyIdValue: Long,
    val employmentId: Long,
    val email: String,
    val twoFactorEnabled: Boolean,
    val reason: String,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "UserDeactivated"
    override val eventVersion: Int = 1
    override val aggregateType: String = "UserAccount"
    override val aggregateId: Long get() = userAccountId
    override val companyId: Long get() = companyIdValue

    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "DEACTIVATE"
        override val details: Map<String, Any?> = mapOf(
            "reason" to reason,
        )
    }

    override val state: DomainEventState = object : DomainEventState {
        override val status: String = "DEACTIVATED"
        override val snapshot: Map<String, Any?> = mapOf(
            "employmentId" to employmentId,
            "email" to email,
            "twoFactorEnabled" to twoFactorEnabled,
            "lockedUntil" to null,
        )
    }
}
