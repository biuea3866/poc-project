package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import org.springframework.data.domain.Page

data class EmployeeSummaryResult(
    val employmentId: Long,
    val personId: Long,
    val companyId: Long,
    val employeeNumber: String,
    val employmentType: EmploymentType,
    val status: EmploymentStatus,
    val departmentId: Long?,
) {
    companion object {
        fun of(employment: Employment): EmployeeSummaryResult = EmployeeSummaryResult(
            employmentId = requireNotNull(employment.id) { "Employment에는 id가 있어야 합니다" },
            personId = employment.personId,
            companyId = employment.companyId,
            employeeNumber = employment.employeeNumber,
            employmentType = employment.employmentType,
            status = employment.status,
            departmentId = employment.departmentId,
        )

        fun pageOf(page: Page<Employment>): Page<EmployeeSummaryResult> = page.map { of(it) }
    }
}
