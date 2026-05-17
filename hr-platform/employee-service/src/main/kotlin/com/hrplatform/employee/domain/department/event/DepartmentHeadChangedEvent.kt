package com.hrplatform.employee.domain.department.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class DepartmentHeadChangedEvent(
    val departmentId: Long,
    val companyIdValue: Long,
    val oldHeadEmploymentId: Long?,
    val newHeadEmploymentId: Long?,
    val statusValue: String,
    val snapshotParentId: Long?,
    val snapshotPath: String,
    val effectiveFromValue: LocalDate,
    val effectiveToValue: LocalDate?,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "DepartmentHeadChanged"
    override val eventVersion: Int = 1
    override val aggregateType: String = "Department"
    override val aggregateId: Long get() = departmentId
    override val companyId: Long get() = companyIdValue
    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "CHANGE_HEAD"
        override val details: Map<String, Any?> = mapOf(
            "oldHeadEmploymentId" to oldHeadEmploymentId,
            "newHeadEmploymentId" to newHeadEmploymentId,
        )
    }
    override val state: DomainEventState = object : DomainEventState {
        override val status: String = statusValue
        override val snapshot: Map<String, Any?> = mapOf(
            "parentId" to snapshotParentId,
            "path" to snapshotPath,
            "headEmploymentId" to newHeadEmploymentId,
            "effectiveFrom" to effectiveFromValue.toString(),
            "effectiveTo" to effectiveToValue?.toString(),
        )
    }
}
