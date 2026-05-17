package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.ZonedDateTime
import java.util.UUID

data class EmployeeTransferredEvent(
    val employmentId: Long,
    val companyIdValue: Long,
    val departmentId: Long?,
    val managerEmploymentId: Long?,
    val country: String,
    val currency: String,
    val timezone: String,
    val oldDepartmentId: Long?,
    val newDepartmentId: Long,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "EmployeeTransferred"
    override val eventVersion: Int = 1
    override val aggregateType: String = "Employment"
    override val aggregateId: Long get() = employmentId
    override val companyId: Long get() = companyIdValue
    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "TRANSFER"
        override val details: Map<String, Any?> = mapOf(
            "oldDepartmentId" to oldDepartmentId,
            "newDepartmentId" to newDepartmentId,
        )
    }
    override val state: DomainEventState = object : DomainEventState {
        override val status: String = "ACTIVE"
        override val snapshot: Map<String, Any?> = mapOf(
            "departmentId" to departmentId,
            "managerEmploymentId" to managerEmploymentId,
            "country" to country,
            "currency" to currency,
            "timezone" to timezone,
        )
    }
}
