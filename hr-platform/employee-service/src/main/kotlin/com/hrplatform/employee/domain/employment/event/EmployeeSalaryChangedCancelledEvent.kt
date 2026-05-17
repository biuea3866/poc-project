package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeSalaryChangedCancelledEvent(
    val employmentId: Long,
    val companyId: Long,
    val cancelledBaseSalary: Long,
    val restoredBaseSalary: Long?,
    val currency: String,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.salary_changed.cancelled"
}
