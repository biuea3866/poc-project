package com.hrplatform.employee.domain.employment

import java.time.LocalDate
import java.time.ZonedDateTime

data class RecordEmploymentEventCommand(
    val employmentId: Long,
    val eventType: RecordableEventType,
    val newDepartmentId: Long? = null,
    val newPositionId: Long? = null,
    val newBaseSalary: Long? = null,
    val newCurrency: String? = null,
    val effectiveDate: LocalDate,
    val actorEmploymentId: Long?,
    val note: String? = null,
)

enum class RecordableEventType {
    DEPT_CHANGE,
    PROMOTION,
    SALARY_CHANGE,
    SUSPEND,
    ;
}
