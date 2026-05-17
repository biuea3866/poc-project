package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeeResignedEvent(
    val employmentId: Long,
    val companyId: Long,
    val reason: String?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.resigned"
}
