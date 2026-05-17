package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import java.time.LocalDate

data class GetEmployeeResult(
    val employmentId: Long,
    val personId: Long,
    val companyId: Long,
    val employeeNumber: String,
    val employmentType: EmploymentType,
    val status: EmploymentStatus,
    val startDate: LocalDate,
    val departmentId: Long?,
    val positionId: Long?,
    val baseSalary: Long?,
) {
    companion object {
        fun of(employment: Employment): GetEmployeeResult = GetEmployeeResult(
            employmentId = requireNotNull(employment.id) { "Employment에는 id가 있어야 합니다" },
            personId = employment.personId,
            companyId = employment.companyId,
            employeeNumber = employment.employeeNumber,
            employmentType = employment.employmentType,
            status = employment.status,
            startDate = employment.startDate,
            departmentId = employment.departmentId,
            positionId = employment.positionId,
            baseSalary = employment.baseSalary,
        )
    }
}
