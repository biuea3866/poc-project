package com.hrplatform.employee.domain.employment.event

import com.hrplatform.core.domain.DomainEvent
import java.time.ZonedDateTime

data class EmployeePromotedCancelledEvent(
    val employmentId: Long,
    val companyId: Long,
    val cancelledPositionId: Long,
    val restoredPositionId: Long?,
    override val occurredAt: ZonedDateTime,
) : DomainEvent {
    override val eventType: String = "employee.promoted.cancelled"
}
