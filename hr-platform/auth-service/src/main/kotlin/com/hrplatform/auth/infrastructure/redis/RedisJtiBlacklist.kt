package com.hrplatform.auth.infrastructure.redis

import com.hrplatform.auth.domain.token.JtiBlacklist
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisJtiBlacklist(
    private val redisTemplate: StringRedisTemplate,
) : JtiBlacklist {

    companion object {
        private const val KEY_PREFIX = "auth:jti:blacklist:"
    }

    override fun add(jti: String, ttl: Duration) {
        redisTemplate.opsForValue().set("$KEY_PREFIX$jti", "1", ttl)
    }

    override fun addAll(jtis: List<String>, ttl: Duration) {
        jtis.forEach { add(it, ttl) }
    }

    override fun contains(jti: String): Boolean =
        redisTemplate.hasKey("$KEY_PREFIX$jti") == true
}
