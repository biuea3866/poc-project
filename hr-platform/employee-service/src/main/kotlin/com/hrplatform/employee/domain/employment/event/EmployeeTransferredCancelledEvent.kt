package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeTransferredCancelledEvent(
    val employmentId: Long,
    val companyId: Long,
    val cancelledDepartmentId: Long,
    val restoredDepartmentId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.transferred.cancelled"
}
