package com.hrplatform.auth.domain.twofactor

interface TwoFactorBackupCodeRepository {
    fun save(code: TwoFactorBackupCode): TwoFactorBackupCode
    fun saveAll(codes: List<TwoFactorBackupCode>): List<TwoFactorBackupCode>
    fun findUnused(userAccountId: Long): List<TwoFactorBackupCode>
    fun deleteAllByUserAccountId(userAccountId: Long, deletedBy: Long?, now: java.time.ZonedDateTime)
}
