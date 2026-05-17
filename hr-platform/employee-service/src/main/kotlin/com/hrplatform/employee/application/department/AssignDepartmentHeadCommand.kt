package com.hrplatform.employee.application.department

data class AssignDepartmentHeadCommand(
    val departmentId: Long,
    val employmentId: Long,
    val actorEmploymentId: Long?,
)
