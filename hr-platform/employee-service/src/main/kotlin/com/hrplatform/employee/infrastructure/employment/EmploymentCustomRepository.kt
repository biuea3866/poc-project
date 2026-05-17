package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus

interface EmploymentCustomRepository {
    fun findByCompanyIdAndEmployeeNumber(companyId: Long, employeeNumber: String): Employment?
    fun findByPersonId(personId: Long): List<Employment>
    fun findByDepartmentTreePath(pathPrefix: String): List<Employment>
    fun findManagedBy(managerEmploymentId: Long): List<Employment>
    fun findByCompanyIdAndStatus(companyId: Long, status: EmploymentStatus): List<Employment>
}
