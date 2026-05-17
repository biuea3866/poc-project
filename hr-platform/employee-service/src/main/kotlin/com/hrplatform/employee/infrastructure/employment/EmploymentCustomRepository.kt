package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EmploymentCustomRepository {
    fun findByCompanyIdAndEmployeeNumber(companyId: Long, employeeNumber: String): Employment?
    fun findByPersonId(personId: Long): List<Employment>
    fun findByDepartmentTreePath(pathPrefix: String): List<Employment>
    fun findManagedBy(managerEmploymentId: Long): List<Employment>
    fun findByCompanyIdAndStatus(companyId: Long, status: EmploymentStatus): List<Employment>
    fun findByCompanyIdWithPage(
        companyId: Long,
        departmentPathPrefix: String?,
        employmentId: Long?,
        pageable: Pageable,
    ): Page<Employment>
}
