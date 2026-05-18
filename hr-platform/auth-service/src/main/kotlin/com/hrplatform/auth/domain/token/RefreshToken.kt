package com.hrplatform.auth.domain.token

import com.hrplatform.core.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(name = "user_account_id", nullable = false)
    val userAccountId: Long,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "access_jti", length = 36, columnDefinition = "CHAR(36)")
    var accessJti: String?,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: ZonedDateTime,

    @Column(name = "device_info", length = 500)
    val deviceInfo: String?,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String?,

    @Column(name = "revoked_at")
    var revokedAt: ZonedDateTime?,

    @Column(name = "revoked_reason")
    var revokedReason: String?,
) : BaseEntity() {

    fun revoke(reason: String, now: ZonedDateTime) {
        check(revokedAt == null) { "이미 폐기된 RefreshToken입니다" }
        revokedAt = now
        revokedReason = reason
    }

    fun rotate(newHash: String, newAccessJti: String) {
        check(revokedAt == null) { "폐기된 RefreshToken은 rotate할 수 없습니다" }
        tokenHash = newHash
        accessJti = newAccessJti
    }

    fun isExpired(now: ZonedDateTime): Boolean = expiresAt.isBefore(now)

    fun isValid(now: ZonedDateTime): Boolean = !isExpired(now) && revokedAt == null && !isDeleted
}
