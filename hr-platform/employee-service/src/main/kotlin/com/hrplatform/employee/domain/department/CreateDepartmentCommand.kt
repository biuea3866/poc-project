package com.hrplatform.employee.domain.department

import java.time.LocalDate

data class CreateDepartmentCommand(
    val companyId: Long,
    val name: String,
    val code: String,
    val parentId: Long?,
    val orderNo: Int,
    val effectiveFrom: LocalDate,
    val actorEmploymentId: Long?,
)
