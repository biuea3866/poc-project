package com.hrplatform.employee.domain.employment

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EmploymentRepository {
    fun save(employment: Employment): Employment
    fun findById(id: Long): Employment?
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
