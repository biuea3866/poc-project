package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.ResignEmploymentCommand

data class ResignRequest(
    val reason: String?,
) {
    fun toCommand(employmentId: Long, actorEmploymentId: Long?): ResignEmploymentCommand =
        ResignEmploymentCommand(
            employmentId = employmentId,
            reason = reason,
            actorEmploymentId = actorEmploymentId,
        )
}
