package com.hrplatform.auth.infrastructure.passwordreset

import com.hrplatform.auth.domain.account.PasswordResetToken
import com.hrplatform.auth.domain.account.PasswordResetTokenRepository
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 비밀번호 재설정 토큰 저장소 - in-memory 구현 (local/test 환경 전용).
 * 운영 환경에서는 Redis 기반 구현으로 교체 필요.
 */
@Repository
@Profile("local", "test", "test-integration")
class InMemoryPasswordResetTokenRepository : PasswordResetTokenRepository {

    private val tokens = ConcurrentHashMap<String, PasswordResetToken>()

    override fun save(tokenHash: String, userAccountId: Long, expiresAt: Instant) {
        cleanup()
        tokens[tokenHash] = PasswordResetToken(
            tokenHash = tokenHash,
            userAccountId = userAccountId,
            expiresAt = expiresAt,
            usedAt = null,
        )
    }

    override fun findByHash(tokenHash: String): PasswordResetToken? = tokens[tokenHash]

    override fun markUsed(tokenHash: String) {
        val token = tokens[tokenHash] ?: return
        tokens[tokenHash] = token.copy(usedAt = Instant.now())
    }

    @Scheduled(fixedDelay = 60_000)
    fun cleanup() {
        val now = Instant.now()
        tokens.entries.removeIf { now.isAfter(it.value.expiresAt) }
    }
}
