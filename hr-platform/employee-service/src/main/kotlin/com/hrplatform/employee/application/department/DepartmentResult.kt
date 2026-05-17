package com.hrplatform.employee.application.department

import com.hrplatform.employee.domain.department.Department
import java.time.LocalDate

data class DepartmentResult(
    val departmentId: Long,
    val companyId: Long,
    val name: String,
    val code: String,
    val parentId: Long?,
    val path: String,
    val headEmploymentId: Long?,
    val effectiveFrom: LocalDate,
) {
    companion object {
        fun of(department: Department): DepartmentResult = DepartmentResult(
            departmentId = requireNotNull(department.id) { "저장된 Department에는 id가 있어야 합니다" },
            companyId = department.companyId,
            name = department.name,
            code = department.code,
            parentId = department.parentId,
            path = department.path,
            headEmploymentId = department.headEmploymentId,
            effectiveFrom = department.effectiveFrom,
        )
    }
}
