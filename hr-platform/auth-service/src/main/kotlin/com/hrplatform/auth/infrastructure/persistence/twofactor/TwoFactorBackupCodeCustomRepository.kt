package com.hrplatform.auth.infrastructure.persistence.twofactor

import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import java.time.ZonedDateTime

interface TwoFactorBackupCodeCustomRepository {
    fun findUnused(userAccountId: Long): List<TwoFactorBackupCode>
    fun deleteAllByUserAccountId(userAccountId: Long, deletedBy: Long?, now: ZonedDateTime)
}
