package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.LocalDate
import java.time.ZonedDateTime

data class EmployeeSuspendedEvent(
    val employmentId: Long,
    val companyId: Long,
    val reason: String,
    val until: LocalDate?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.suspended"
}
