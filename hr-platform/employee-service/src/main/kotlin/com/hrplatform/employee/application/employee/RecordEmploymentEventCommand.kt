package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.RecordEmploymentEventCommand as DomainRecordEmploymentEventCommand
import com.hrplatform.employee.domain.employment.RecordableEventType
import java.time.LocalDate

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
) {
    fun toDomainCommand(): DomainRecordEmploymentEventCommand = DomainRecordEmploymentEventCommand(
        employmentId = employmentId,
        eventType = eventType,
        newDepartmentId = newDepartmentId,
        newPositionId = newPositionId,
        newBaseSalary = newBaseSalary,
        newCurrency = newCurrency,
        effectiveDate = effectiveDate,
        actorEmploymentId = actorEmploymentId,
        note = note,
    )
}
