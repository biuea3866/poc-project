package com.hrplatform.employee.application.employee

data class GetEmployeeCommand(
    val viewerEmploymentId: Long,
    val targetEmploymentId: Long,
)
