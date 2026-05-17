package com.hrplatform.employee.application.employee

data class ResignEmploymentCommand(
    val employmentId: Long,
    val reason: String?,
    val actorEmploymentId: Long?,
)
