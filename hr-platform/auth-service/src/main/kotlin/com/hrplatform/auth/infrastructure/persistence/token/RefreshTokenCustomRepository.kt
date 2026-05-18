package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.RefreshToken
import java.time.ZonedDateTime

interface RefreshTokenCustomRepository {
    fun findActiveByUserAccountId(userAccountId: Long): List<RefreshToken>
    fun revokeAllByUserAccountId(userAccountId: Long, reason: String, now: ZonedDateTime)
}
