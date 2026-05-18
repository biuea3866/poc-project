package com.hrplatform.auth.infrastructure.persistence.account

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import org.springframework.stereotype.Repository

@Repository
class UserAccountRepositoryImpl(
    private val jpaRepository: UserAccountJpaRepository,
) : UserAccountRepository {

    override fun save(userAccount: UserAccount): UserAccount =
        jpaRepository.save(userAccount)

    override fun findById(id: Long): UserAccount? =
        jpaRepository.findById(id).orElse(null)

    override fun findByEmailHash(emailHash: String): UserAccount? =
        jpaRepository.findByEmailHashAndDeletedAtIsNull(emailHash)

    override fun findByEmailHash(emailHash: String): UserAccount? =
        jpaRepository.findByEmailHash(emailHash)

    override fun findByEmploymentId(employmentId: Long): UserAccount? =
        jpaRepository.findByEmploymentId(employmentId)

    override fun findActiveByCompanyId(companyId: Long): List<UserAccount> =
        jpaRepository.findByCompanyIdAndStatusAndDeletedAtIsNull(companyId, UserAccountStatus.ACTIVE)
}
