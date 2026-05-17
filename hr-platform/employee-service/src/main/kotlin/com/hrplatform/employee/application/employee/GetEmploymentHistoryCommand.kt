package com.hrplatform.employee.application.employee

data class GetEmploymentHistoryCommand(
    val viewerEmploymentId: Long,
    val employmentId: Long,
)
