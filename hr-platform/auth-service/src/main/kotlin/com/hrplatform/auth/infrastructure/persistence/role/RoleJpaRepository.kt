package com.hrplatform.auth.infrastructure.persistence.role

import com.hrplatform.auth.domain.role.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleJpaRepository : JpaRepository<Role, Long> {
    fun findByCompanyIdAndCodeAndDeletedAtIsNull(companyId: Long?, code: String): Role?
    fun findByCompanyIdAndDeletedAtIsNull(companyId: Long?): List<Role>
}
