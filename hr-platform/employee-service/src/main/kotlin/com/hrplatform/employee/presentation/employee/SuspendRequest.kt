package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.SuspendEmploymentCommand
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class SuspendRequest(
    @field:NotBlank
    val reason: String,

    val until: LocalDate?,
) {
    fun toCommand(employmentId: Long, actorEmploymentId: Long?): SuspendEmploymentCommand =
        SuspendEmploymentCommand(
            employmentId = employmentId,
            reason = reason,
            until = until,
            actorEmploymentId = actorEmploymentId,
        )
}
