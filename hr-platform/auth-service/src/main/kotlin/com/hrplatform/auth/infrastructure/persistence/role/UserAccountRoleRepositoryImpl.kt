package com.hrplatform.auth.infrastructure.persistence.role

import com.hrplatform.auth.domain.role.UserAccountRole
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class UserAccountRoleRepositoryImpl(
    private val jpaRepository: UserAccountRoleJpaRepository,
) : UserAccountRoleRepository {

    override fun save(userAccountRole: UserAccountRole): UserAccountRole =
        jpaRepository.save(userAccountRole)

    override fun findByUserAccountId(userAccountId: Long): List<UserAccountRole> =
        jpaRepository.findByUserAccountIdAndDeletedAtIsNull(userAccountId)

    override fun findByUserAccountIdAndRoleId(userAccountId: Long, roleId: Long): UserAccountRole? =
        jpaRepository.findByUserAccountIdAndRoleIdAndDeletedAtIsNull(userAccountId, roleId)

    override fun deleteByUserAccountIdAndRoleId(
        userAccountId: Long,
        roleId: Long,
        deletedBy: Long?,
        now: ZonedDateTime,
    ) {
        val existing = jpaRepository.findByUserAccountIdAndRoleIdAndDeletedAtIsNull(userAccountId, roleId)
        existing?.softDelete(now, deletedBy)
    }
}
