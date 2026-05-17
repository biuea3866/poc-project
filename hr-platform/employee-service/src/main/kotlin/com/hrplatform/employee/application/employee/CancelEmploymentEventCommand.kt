package com.hrplatform.employee.application.employee

data class CancelEmploymentEventCommand(
    val employmentId: Long,
    val historyId: Long,
    val cancellationReason: String,
    val actorEmploymentId: Long?,
)
