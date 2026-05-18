package com.hrplatform.auth.infrastructure.persistence.twofactor

import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCodeRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class TwoFactorBackupCodeRepositoryImpl(
    private val jpaRepository: TwoFactorBackupCodeJpaRepository,
) : TwoFactorBackupCodeRepository {

    override fun save(code: TwoFactorBackupCode): TwoFactorBackupCode =
        jpaRepository.save(code)

    override fun saveAll(codes: List<TwoFactorBackupCode>): List<TwoFactorBackupCode> =
        jpaRepository.saveAll(codes)

    override fun findUnused(userAccountId: Long): List<TwoFactorBackupCode> =
        jpaRepository.findUnused(userAccountId)

    override fun deleteAllByUserAccountId(userAccountId: Long, deletedBy: Long?, now: ZonedDateTime) =
        jpaRepository.deleteAllByUserAccountId(userAccountId, deletedBy, now)
}
