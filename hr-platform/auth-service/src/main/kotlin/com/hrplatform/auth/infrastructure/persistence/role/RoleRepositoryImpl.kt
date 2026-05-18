package com.hrplatform.auth.infrastructure.persistence.role

import com.hrplatform.auth.domain.role.Role
import com.hrplatform.auth.domain.role.RoleRepository
import org.springframework.stereotype.Repository

@Repository
class RoleRepositoryImpl(
    private val jpaRepository: RoleJpaRepository,
) : RoleRepository {

    override fun save(role: Role): Role =
        jpaRepository.save(role)

    override fun findById(id: Long): Role? =
        jpaRepository.findById(id).orElse(null)

    override fun findByCompanyIdAndCode(companyId: Long?, code: String): Role? =
        jpaRepository.findByCompanyIdAndCodeAndDeletedAtIsNull(companyId, code)

    override fun findAllByCompanyId(companyId: Long?): List<Role> =
        jpaRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
}
