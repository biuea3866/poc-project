package com.hrplatform.employee.domain.department

interface DepartmentRepository {

    fun save(department: Department): Department

    fun findById(id: Long): Department?

    /**
     * path LIKE 'prefix%' 를 이용해 서브트리 전체를 조회한다.
     * 예: findByPathPrefix("/1/") → /1/ 로 시작하는 모든 부서 반환
     */
    fun findByPathPrefix(prefix: String): List<Department>

    fun findByParentId(parentId: Long): List<Department>
}
