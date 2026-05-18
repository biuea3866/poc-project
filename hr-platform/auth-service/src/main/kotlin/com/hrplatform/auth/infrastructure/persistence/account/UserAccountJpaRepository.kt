package com.hrplatform.auth.infrastructure.persistence.account

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountStatus
import org.springframework.data.jpa.repository.JpaRepository

interface UserAccountJpaRepository :
    JpaRepository<UserAccount, Long>,
    UserAccountCustomRepository {

    fun findByEmail(email: String): UserAccount?

    fun findByEmailAndDeletedAtIsNull(email: String): UserAccount?

    fun findByCompanyIdAndStatusAndDeletedAtIsNull(
        companyId: Long,
        status: UserAccountStatus,
    ): List<UserAccount>
}
