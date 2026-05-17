package com.hrplatform.employee.domain.department

interface DepartmentRepository {
    fun save(department: Department): Department
    fun findById(id: Long): Department?
    fun findByPathPrefix(prefix: String): List<Department>
    fun findByParentId(parentId: Long?): List<Department>
}
