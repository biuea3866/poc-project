package com.biuea.chat.infrastructure.redis

import com.biuea.chat.domain.routing.SessionLocator
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 세션 레지스트리의 Redis 구현. userId → serverId 를 TTL 과 함께 저장해
 * 비정상 종료가 남긴 stale 엔트리를 자동 만료시킨다.
 */
@Component
class RedisSessionLocator(
    private val redisTemplate: StringRedisTemplate,
) : SessionLocator {
    override fun register(userId: Long, serverId: String) {
        redisTemplate.opsForValue().set(key(userId), serverId, TTL)
    }

    override fun locate(userId: Long): String? = redisTemplate.opsForValue().get(key(userId))

    override fun deregister(userId: Long) {
        redisTemplate.delete(key(userId))
    }

    private fun key(userId: Long) = "session:$userId"

    companion object {
        private val TTL: Duration = Duration.ofSeconds(30)
    }
}
