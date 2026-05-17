package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.CreateDepartmentCommand
import java.time.LocalDate

data class CreateDepartmentUseCaseCommand(
    val companyId: Long,
    val name: String,
    val code: String,
    val parentId: Long?,
    val orderNo: Int,
    val effectiveFrom: LocalDate,
    val actorEmploymentId: Long?,
) {
    fun toDomainCommand(): CreateDepartmentCommand = CreateDepartmentCommand(
        companyId = companyId,
        name = name,
        code = code,
        parentId = parentId,
        orderNo = orderNo,
        effectiveFrom = effectiveFrom,
        actorEmploymentId = actorEmploymentId,
    )
}
