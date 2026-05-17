package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus

data class RecordEmploymentEventResult(
    val employmentId: Long,
    val status: EmploymentStatus,
    val departmentId: Long?,
    val positionId: Long?,
    val baseSalary: Long?,
) {
    companion object {
        fun of(employment: Employment): RecordEmploymentEventResult = RecordEmploymentEventResult(
            employmentId = requireNotNull(employment.id) { "저장된 Employment에는 id가 있어야 합니다" },
            status = employment.status,
            departmentId = employment.departmentId,
            positionId = employment.positionId,
            baseSalary = employment.baseSalary,
        )
    }
}
