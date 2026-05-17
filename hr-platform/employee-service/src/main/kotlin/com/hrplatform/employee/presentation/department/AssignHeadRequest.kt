package com.hrplatform.employee.presentation.department

import com.hrplatform.employee.application.department.AssignDepartmentHeadCommand
import jakarta.validation.constraints.NotNull

data class AssignHeadRequest(
    @field:NotNull
    val employmentId: Long,
) {
    fun toCommand(departmentId: Long, actorEmploymentId: Long?): AssignDepartmentHeadCommand =
        AssignDepartmentHeadCommand(
            departmentId = departmentId,
            employmentId = employmentId,
            actorEmploymentId = actorEmploymentId,
        )
}
