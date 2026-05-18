package com.hrplatform.auth.domain.role

interface UserAccountRoleRepository {
    fun save(userAccountRole: UserAccountRole): UserAccountRole
    fun findByUserAccountId(userAccountId: Long): List<UserAccountRole>
    fun findByUserAccountIdAndRoleId(userAccountId: Long, roleId: Long): UserAccountRole?
    fun deleteByUserAccountIdAndRoleId(userAccountId: Long, roleId: Long, deletedBy: Long?, now: java.time.ZonedDateTime)
}
