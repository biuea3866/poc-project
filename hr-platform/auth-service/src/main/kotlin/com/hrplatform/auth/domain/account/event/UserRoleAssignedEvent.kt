package com.hrplatform.auth.domain.account.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.ZonedDateTime
import java.util.UUID

data class UserRoleAssignedEvent(
    val userAccountId: Long,
    val companyIdValue: Long,
    val employmentId: Long,
    val email: String,
    val twoFactorEnabled: Boolean,
    val lockedUntil: ZonedDateTime?,
    val roleId: Long,
    val roleCode: String,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "UserRoleAssigned"
    override val eventVersion: Int = 1
    override val aggregateType: String = "UserAccount"
    override val aggregateId: Long get() = userAccountId
    override val companyId: Long get() = companyIdValue

    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "ASSIGN_ROLE"
        override val details: Map<String, Any?> = mapOf(
            "roleId" to roleId,
            "roleCode" to roleCode,
            "actor" to actorEmploymentId,
        )
    }

    override val state: DomainEventState = object : DomainEventState {
        override val status: String = "ACTIVE"
        override val snapshot: Map<String, Any?> = mapOf(
            "employmentId" to employmentId,
            "email" to email,
            "twoFactorEnabled" to twoFactorEnabled,
            "lockedUntil" to lockedUntil?.toString(),
        )
    }
}
