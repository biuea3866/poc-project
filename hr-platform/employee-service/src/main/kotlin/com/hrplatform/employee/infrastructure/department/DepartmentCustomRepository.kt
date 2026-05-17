package com.hrplatform.employee.infrastructure.department

interface DepartmentCustomRepository {
    fun findByPathPrefixQuery(prefix: String): List<DepartmentEntity>
    fun findByParentIdQuery(parentId: Long): List<DepartmentEntity>
}
