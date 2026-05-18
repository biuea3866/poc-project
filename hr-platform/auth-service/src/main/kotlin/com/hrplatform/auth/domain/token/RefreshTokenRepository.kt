package com.hrplatform.auth.domain.token

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken): RefreshToken
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun findActiveByUserAccountId(userAccountId: Long): List<RefreshToken>
    fun revokeAllByUserAccountId(userAccountId: Long, reason: String, now: java.time.ZonedDateTime)
}
