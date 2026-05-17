package com.hrplatform.employee.application.department

data class MoveDepartmentCommand(
    val departmentId: Long,
    val newParentId: Long?,
    val actorEmploymentId: Long?,
)
