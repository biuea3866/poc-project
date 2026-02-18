package com.biuea.wiki.infrastructure.auth

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class RedisAuthTokenStore(
    private val redisTemplate: StringRedisTemplate,
) {
    fun saveRefreshToken(userId: Long, refreshToken: String, expiresAt: LocalDateTime) {
        val ttl = ttlUntil(expiresAt)
        redisTemplate.opsForValue().set(refreshTokenKey(refreshToken), userId.toString(), ttl)
        redisTemplate.opsForSet().add(refreshUserSetKey(userId), refreshToken)
        redisTemplate.expire(refreshUserSetKey(userId), ttl)
    }

    fun isAccessTokenBlacklisted(accessToken: String): Boolean {
        return redisTemplate.hasKey(accessBlacklistKey(accessToken))
    }

    fun blacklistAccessToken(accessToken: String, expiresAt: LocalDateTime) {
        val ttl = ttlUntil(expiresAt)
        redisTemplate.opsForValue().set(accessBlacklistKey(accessToken), "1", ttl)
    }

    fun validateRefreshToken(refreshToken: String): Boolean {
        return redisTemplate.hasKey(refreshTokenKey(refreshToken))
    }

    fun revokeRefreshToken(refreshToken: String) {
        val userId = redisTemplate.opsForValue().get(refreshTokenKey(refreshToken))?.toLongOrNull()
        redisTemplate.delete(refreshTokenKey(refreshToken))
        if (userId != null) {
            redisTemplate.opsForSet().remove(refreshUserSetKey(userId), refreshToken)
        }
    }

    fun revokeAllRefreshTokensByUserId(userId: Long) {
        val key = refreshUserSetKey(userId)
        val refreshTokens = redisTemplate.opsForSet().members(key).orEmpty()
        if (refreshTokens.isNotEmpty()) {
            val tokenKeys = refreshTokens.map { refreshTokenKey(it) }
            redisTemplate.delete(tokenKeys)
        }
        redisTemplate.delete(key)
    }

    private fun ttlUntil(expiresAt: LocalDateTime): Duration {
        val nowMillis = System.currentTimeMillis()
        val expiresAtMillis = expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ttlMillis = (expiresAtMillis - nowMillis).coerceAtLeast(1)
        return Duration.ofMillis(ttlMillis)
    }

    private fun refreshTokenKey(token: String): String = "auth:refresh:token:$token"
    private fun refreshUserSetKey(userId: Long): String = "auth:refresh:user:$userId"
    private fun accessBlacklistKey(token: String): String = "auth:blacklist:access:$token"
}
