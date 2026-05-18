package com.hrplatform.auth.infrastructure.passwordreset

import com.hrplatform.core.exception.BusinessException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val TOKEN_TTL = Duration.ofMinutes(30)

/**
 * 비밀번호 재설정 토큰 저장소 (in-memory 임시 구현).
 * TODO: 향후 Redis 기반 구현으로 교체 예정.
 */
@Component
class PasswordResetTokenStore {

    private val tokens = ConcurrentHashMap<String, Instant>()

    fun store(token: String) {
        val expiry = Instant.now().plus(TOKEN_TTL)
        tokens[token] = expiry
    }

    fun validate(token: String) {
        val expiry = tokens[token]
        if (expiry == null || Instant.now().isAfter(expiry)) {
            throw BusinessException(
                errorCode = "INVALID_RESET_TOKEN",
                message = "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다",
            )
        }
    }

    fun consume(token: String) {
        tokens.remove(token)
    }
}
