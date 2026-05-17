package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.RecordEmploymentEventCommand
import com.hrplatform.employee.domain.employment.RecordableEventType
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class BulkRecordEventRequest(
    @field:NotNull
    val employmentId: Long,

    @field:NotNull
    val eventType: RecordableEventType,

    val newDepartmentId: Long?,
    val newPositionId: Long?,
    val newBaseSalary: Long?,
    val newCurrency: String?,

    @field:NotNull
    val effectiveDate: LocalDate,

    val note: String?,
) {
    fun toCommand(actorEmploymentId: Long?): RecordEmploymentEventCommand =
        RecordEmploymentEventCommand(
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
