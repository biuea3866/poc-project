package com.biuea.delivery.infrastructure.lock

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * Redis 기반 분산락.
 *
 * 획득: `SET lock:order:{id} {token} NX PX {ttl}` 한 방으로 "없을 때만 쓰기 + 만료 설정" 을 원자적으로 처리한다.
 * SETNX 후 EXPIRE 를 따로 부르면 그 사이에 프로세스가 죽었을 때 만료 없는 락이 남아 주문이 영영 잠긴다.
 *
 * 토큰: 소유자를 구분하는 UUID. 락 이름만으로는 "지금 잡고 있는 게 나인지" 알 수 없다.
 */
@Component
class RedisDistributedLock(
    private val stringRedisTemplate: StringRedisTemplate,
) {
    /** 획득에 성공하면 해제에 필요한 토큰을, 실패하면 null 을 돌려준다. */
    fun tryAcquire(lockName: String, leaseTtl: Duration, waitPolicy: LockWaitPolicy): String? {
        val token = UUID.randomUUID().toString()
        val waitStartedAtNanos = System.nanoTime()
        while (true) {
            val acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockName, token, leaseTtl)
            if (acquired == true) {
                return token
            }
            if (waitPolicy.isExpired(waitStartedAtNanos)) {
                return null
            }
            waitPolicy.sleepBeforeRetry()
        }
    }

    /**
     * 내 토큰일 때만 해제한다. 해제했으면 true.
     *
     * GET 으로 토큰을 확인하고 DEL 하는 2단계 해제는 안전하지 않다.
     * GET 과 DEL 사이에 TTL 이 만료되고 다른 라이더가 같은 락을 새로 잡으면, 내가 남의 락을 지운다.
     * 그 순간 두 스레드가 동시에 임계 구역에 들어가 중복 배차가 난다.
     * Lua 스크립트는 Redis 에서 단일 명령처럼 원자적으로 실행되므로 비교와 삭제 사이에 아무도 끼어들 수 없다.
     */
    fun release(lockName: String, token: String): Boolean =
        stringRedisTemplate.execute(RELEASE_SCRIPT, listOf(lockName), token) == RELEASE_SUCCEEDED

    companion object {
        private const val RELEASE_SUCCEEDED = 1L

        private val RELEASE_SCRIPT: RedisScript<Long> = DefaultRedisScript<Long>().apply {
            setScriptText(
                """
                if redis.call('GET', KEYS[1]) == ARGV[1] then
                    return redis.call('DEL', KEYS[1])
                else
                    return 0
                end
                """.trimIndent(),
            )
            setResultType(Long::class.javaObjectType)
        }
    }
}
