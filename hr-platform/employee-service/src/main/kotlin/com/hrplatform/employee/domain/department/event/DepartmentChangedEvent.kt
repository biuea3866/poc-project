package com.hrplatform.employee.domain.department.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class DepartmentChangedEvent(
    val departmentId: Long,
    val companyId: Long,
    val oldParentId: Long?,
    val newParentId: Long?,
    val oldPath: String,
    val newPath: String,
    override val occurredAt: ZonedDateTime,
    override val eventType: String = "department.changed",
) : DomainEvent
