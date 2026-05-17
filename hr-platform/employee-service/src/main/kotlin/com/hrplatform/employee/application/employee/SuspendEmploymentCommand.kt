package com.hrplatform.employee.application.employee

import java.time.LocalDate

data class SuspendEmploymentCommand(
    val employmentId: Long,
    val reason: String,
    val until: LocalDate?,
    val actorEmploymentId: Long?,
)
