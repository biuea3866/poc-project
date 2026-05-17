package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeTransferredEvent(
    val employmentId: Long,
    val companyId: Long,
    val previousDepartmentId: Long?,
    val newDepartmentId: Long,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.transferred"
}
