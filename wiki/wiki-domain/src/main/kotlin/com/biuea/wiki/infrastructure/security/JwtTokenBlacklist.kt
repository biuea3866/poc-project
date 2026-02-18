package com.biuea.wiki.infrastructure.security

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class JwtTokenBlacklist {
    private val blacklistedTokens: MutableMap<String, Long> = ConcurrentHashMap()

    fun blacklist(token: String, expiresAtMillis: Long) {
        clearExpired()
        blacklistedTokens[token] = expiresAtMillis
    }

    fun isBlacklisted(token: String): Boolean {
        clearExpired()
        return blacklistedTokens.containsKey(token)
    }

    private fun clearExpired() {
        val now = System.currentTimeMillis()
        blacklistedTokens.entries.removeIf { (_, expiresAtMillis) -> expiresAtMillis <= now }
    }
}
