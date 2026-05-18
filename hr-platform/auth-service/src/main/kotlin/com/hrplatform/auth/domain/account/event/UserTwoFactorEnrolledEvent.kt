package com.hrplatform.auth.domain.account.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.ZonedDateTime
import java.util.UUID

data class UserTwoFactorEnrolledEvent(
    val userAccountId: Long,
    val companyIdValue: Long,
    val employmentId: Long,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "UserTwoFactorEnrolled"
    override val eventVersion: Int = 1
    override val aggregateType: String = "UserAccount"
    override val aggregateId: Long get() = userAccountId
    override val companyId: Long get() = companyIdValue

    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "ENROLL_2FA"
        override val details: Map<String, Any?> = mapOf(
            "method" to "TOTP",
        )
    }

    override val state: DomainEventState = object : DomainEventState {
        override val status: String = "ACTIVE"
        override val snapshot: Map<String, Any?> = mapOf(
            "employmentId" to employmentId,
            "twoFactorEnabled" to true,
            "lockedUntil" to null,
        )
    }
}
