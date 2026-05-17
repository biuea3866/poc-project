package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeePromotedEvent(
    val employmentId: Long,
    val companyId: Long,
    val previousPositionId: Long?,
    val newPositionId: Long,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.promoted"
}
