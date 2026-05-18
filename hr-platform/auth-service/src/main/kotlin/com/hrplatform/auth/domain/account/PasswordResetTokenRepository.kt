package com.hrplatform.auth.domain.account

import java.time.Instant

data class PasswordResetToken(
    val tokenHash: String,
    val userAccountId: Long,
    val expiresAt: Instant,
    val usedAt: Instant?,
)

interface PasswordResetTokenRepository {
    fun save(tokenHash: String, userAccountId: Long, expiresAt: Instant)
    fun findByHash(tokenHash: String): PasswordResetToken?
    fun markUsed(tokenHash: String)
}
