package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeSuspendedCancelledEvent(
    val employmentId: Long,
    val companyId: Long,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.suspended.cancelled"
}
