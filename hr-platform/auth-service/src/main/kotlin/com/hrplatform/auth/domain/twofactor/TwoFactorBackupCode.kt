package com.hrplatform.auth.domain.twofactor

import com.hrplatform.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "two_factor_backup_codes")
class TwoFactorBackupCode(
    @Column(name = "user_account_id", nullable = false)
    val userAccountId: Long,

    @Column(name = "code_hash", nullable = false)
    val codeHash: String,

    @Column(name = "used_at")
    var usedAt: ZonedDateTime?,
) : BaseEntity() {

    fun use(now: ZonedDateTime) {
        if (usedAt != null) throw BackupCodeAlreadyUsedException()
        usedAt = now
    }

    fun isUsed(): Boolean = usedAt != null
}
