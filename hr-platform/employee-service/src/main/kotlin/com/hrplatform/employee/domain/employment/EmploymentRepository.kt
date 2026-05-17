package com.hrplatform.employee.domain.employment

interface EmploymentRepository {
    fun save(employment: Employment): Employment
    fun findById(id: Long): Employment?
    fun findByCompanyIdAndEmployeeNumber(companyId: Long, employeeNumber: String): Employment?
}
