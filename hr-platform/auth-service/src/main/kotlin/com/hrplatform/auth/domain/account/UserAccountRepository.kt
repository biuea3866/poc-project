package com.hrplatform.auth.domain.account

interface UserAccountRepository {
    fun save(userAccount: UserAccount): UserAccount
    fun findById(id: Long): UserAccount?
    fun findByEmailHash(emailHash: String): UserAccount?
    fun findByEmploymentId(employmentId: Long): UserAccount?
    fun findActiveByCompanyId(companyId: Long): List<UserAccount>
}
