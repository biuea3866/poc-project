package com.hrplatform.employee.application.employee

data class ResumeEmploymentCommand(
    val employmentId: Long,
    val actorEmploymentId: Long?,
)
