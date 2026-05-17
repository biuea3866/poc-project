package com.hrplatform.employee.presentation.department

import com.hrplatform.employee.application.department.CreateDepartmentUseCaseCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateDepartmentRequest(
    @field:NotNull
    val companyId: Long,

    @field:NotBlank
    val name: String,

    @field:NotBlank
    val code: String,

    val parentId: Long?,

    @field:NotNull
    val orderNo: Int,

    @field:NotNull
    val effectiveFrom: LocalDate,
) {
    fun toCommand(actorEmploymentId: Long?): CreateDepartmentUseCaseCommand = CreateDepartmentUseCaseCommand(
        companyId = companyId,
        name = name,
        code = code,
        parentId = parentId,
        orderNo = orderNo,
        effectiveFrom = effectiveFrom,
        actorEmploymentId = actorEmploymentId,
    )
}
