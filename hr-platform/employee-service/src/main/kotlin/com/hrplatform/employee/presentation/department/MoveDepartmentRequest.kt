package com.hrplatform.employee.presentation.department

import com.hrplatform.employee.application.department.MoveDepartmentCommand

data class MoveDepartmentRequest(
    val newParentId: Long?,
) {
    fun toCommand(departmentId: Long, actorEmploymentId: Long?): MoveDepartmentCommand = MoveDepartmentCommand(
        departmentId = departmentId,
        newParentId = newParentId,
        actorEmploymentId = actorEmploymentId,
    )
}
