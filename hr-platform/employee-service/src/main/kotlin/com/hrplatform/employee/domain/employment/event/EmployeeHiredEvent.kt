package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeHiredEvent(
    val employmentId: Long,
    val companyId: Long,
    val personId: Long,
    val employeeNumber: String,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.hired"
}
