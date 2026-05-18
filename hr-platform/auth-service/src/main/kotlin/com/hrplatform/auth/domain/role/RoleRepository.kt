package com.hrplatform.auth.domain.role

interface RoleRepository {
    fun save(role: Role): Role
    fun findById(id: Long): Role?
    fun findByCompanyIdAndCode(companyId: Long?, code: String): Role?
    fun findAllByCompanyId(companyId: Long?): List<Role>
}
