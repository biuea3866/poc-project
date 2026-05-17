package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeSalaryChangedEvent(
    val employmentId: Long,
    val companyId: Long,
    val previousBaseSalary: Long?,
    val newBaseSalary: Long,
    val currency: String,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.salary_changed"
}
