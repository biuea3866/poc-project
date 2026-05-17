package com.hrplatform.employee.domain.department.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class DepartmentHeadChangedEvent(
    val departmentId: Long,
    val companyId: Long,
    val oldHead: Long?,
    val newHead: Long?,
    override val occurredAt: ZonedDateTime,
    override val eventType: String = "department.head_changed",
) : DomainEvent
