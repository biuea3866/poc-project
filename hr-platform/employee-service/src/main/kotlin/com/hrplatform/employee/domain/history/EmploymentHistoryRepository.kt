package com.hrplatform.employee.domain.history

interface EmploymentHistoryRepository {
    fun save(history: EmploymentHistory): EmploymentHistory
    fun findById(id: Long): EmploymentHistory?
    fun findByEmploymentId(employmentId: Long): List<EmploymentHistory>
    fun findLastByEmploymentId(employmentId: Long): EmploymentHistory?
}
