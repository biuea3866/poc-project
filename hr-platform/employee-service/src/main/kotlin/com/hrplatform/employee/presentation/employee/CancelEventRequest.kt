package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.CancelEmploymentEventCommand
import jakarta.validation.constraints.NotBlank

data class CancelEventRequest(
    @field:NotBlank
    val cancellationReason: String,
) {
    fun toCommand(employmentId: Long, historyId: Long, actorEmploymentId: Long?): CancelEmploymentEventCommand =
        CancelEmploymentEventCommand(
            employmentId = employmentId,
            historyId = historyId,
            cancellationReason = cancellationReason,
            actorEmploymentId = actorEmploymentId,
        )
}
