package com.example.order

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 기반 분산 RateLimiter (Sliding Window Counter).
 *
 * Resilience4j RateLimiter는 인스턴스 로컬이라 N대로 스케일아웃되면 허용량도 N배가 된다.
 * 이 컴포넌트는 Lua 스크립트로 INCR/EXPIRE를 원자적으로 수행하여
 * 모든 인스턴스가 같은 카운터를 공유하도록 한다.
 *
 * 사용 알고리즘: 고정 윈도우(fixed window) 카운터.
 *  - 1초 단위 버킷 키(rl:{name}:{epochSecond})에 INCR
 *  - 첫 INCR이면 (windowSeconds + 1)초 TTL 설정 (자동 정리)
 *  - count <= limit 인 동안 허용
 *
 * 단점: 윈도우 경계에서 burst 가능 (limit 2배까지 가능). Sliding window log/sliding counter로 강화 가능.
 */
@Component
class RedisRateLimiter(
    private val redis: StringRedisTemplate,
    @Value("\${ratelimit.redis.limit-per-second:20}") private val limit: Long,
    @Value("\${ratelimit.redis.window-seconds:1}") private val windowSeconds: Long,
) {

    private val luaScript = DefaultRedisScript<Long>().apply {
        setScriptText(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """.trimIndent()
        )
        resultType = Long::class.java
    }

    fun tryAcquire(name: String): Boolean {
        val bucket = System.currentTimeMillis() / (windowSeconds * 1000)
        val key = "rl:$name:$bucket"
        val count = redis.execute(luaScript, listOf(key), (windowSeconds + 1).toString())
        return count <= limit
    }

    fun config(): Map<String, Any> = mapOf(
        "limitPerWindow" to limit,
        "windowSeconds" to windowSeconds,
        "windowExpirationTtl" to Duration.ofSeconds(windowSeconds + 1).toString(),
    )
}
