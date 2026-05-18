package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.RefreshToken
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class RefreshTokenRepositoryImpl(
    private val jpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {

    override fun save(refreshToken: RefreshToken): RefreshToken =
        jpaRepository.save(refreshToken)

    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        jpaRepository.findByTokenHash(tokenHash)

    override fun findActiveByUserAccountId(userAccountId: Long): List<RefreshToken> =
        jpaRepository.findActiveByUserAccountId(userAccountId)

    override fun revokeAllByUserAccountId(userAccountId: Long, reason: String, now: ZonedDateTime) =
        jpaRepository.revokeAllByUserAccountId(userAccountId, reason, now)
}
