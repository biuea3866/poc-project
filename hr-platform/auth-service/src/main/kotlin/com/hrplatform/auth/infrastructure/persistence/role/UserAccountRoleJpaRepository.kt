package com.hrplatform.auth.infrastructure.persistence.role

import com.hrplatform.auth.domain.role.UserAccountRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserAccountRoleJpaRepository : JpaRepository<UserAccountRole, Long> {
    fun findByUserAccountIdAndDeletedAtIsNull(userAccountId: Long): List<UserAccountRole>
    fun findByUserAccountIdAndRoleIdAndDeletedAtIsNull(userAccountId: Long, roleId: Long): UserAccountRole?
}
