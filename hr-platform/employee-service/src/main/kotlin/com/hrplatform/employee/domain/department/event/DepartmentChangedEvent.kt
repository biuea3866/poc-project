package com.hrplatform.employee.domain.department.event

import com.hrplatform.core.domain.DomainEvent
import com.hrplatform.core.domain.DomainEventAction
import com.hrplatform.core.domain.DomainEventState
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class DepartmentChangedEvent(
    val departmentId: Long,
    val companyIdValue: Long,
    val oldParentId: Long?,
    val newParentId: Long?,
    val oldPath: String,
    val newPath: String,
    val statusValue: String,
    val snapshotHeadEmploymentId: Long?,
    val effectiveFromValue: LocalDate,
    val effectiveToValue: LocalDate?,
    override val actorEmploymentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val eventType: String = "DepartmentChanged"
    override val eventVersion: Int = 1
    override val aggregateType: String = "Department"
    override val aggregateId: Long get() = departmentId
    override val companyId: Long get() = companyIdValue
    override val action: DomainEventAction = object : DomainEventAction {
        override val type: String = "MOVE"
        override val details: Map<String, Any?> = mapOf(
            "oldParentId" to oldParentId,
            "newParentId" to newParentId,
            "oldPath" to oldPath,
            "newPath" to newPath,
        )
    }
    override val state: DomainEventState = object : DomainEventState {
        override val status: String = statusValue
        override val snapshot: Map<String, Any?> = mapOf(
            "parentId" to newParentId,
            "path" to newPath,
            "headEmploymentId" to snapshotHeadEmploymentId,
            "effectiveFrom" to effectiveFromValue.toString(),
            "effectiveTo" to effectiveToValue?.toString(),
        )
    }
}
