package com.closet.inventory.infrastructure

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class InventoryLockService(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val LOCK_PREFIX = "lock:inventory:"
        private val LOCK_TTL = Duration.ofSeconds(10)
        private const val MAX_RETRY = 3
        private const val RETRY_DELAY_MS = 100L

        // Lua script: atomic check-and-delete (값이 같을 때만 삭제)
        private val UNLOCK_SCRIPT = DefaultRedisScript<Long>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """.trimIndent(),
            Long::class.java
        )
    }

    /**
     * Redis SETNX 기반 분산 락을 획득하고, action을 실행한 뒤, 락을 해제한다.
     *
     * @param productOptionId 락 대상 SKU ID
     * @param action 락 내에서 실행할 비즈니스 로직
     * @return action의 반환값
     * @throws BusinessException 락 획득 실패 시
     */
    fun <T> withLock(productOptionId: Long, action: () -> T): T {
        val lockKey = "$LOCK_PREFIX$productOptionId"
        val lockValue = UUID.randomUUID().toString()

        val acquired = tryAcquireLock(lockKey, lockValue)
        if (!acquired) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "재고 락 획득에 실패했습니다. productOptionId=$productOptionId"
            )
        }

        try {
            logger.debug { "분산 락 획득: key=$lockKey, value=$lockValue" }
            return action()
        } finally {
            releaseLock(lockKey, lockValue)
            logger.debug { "분산 락 해제: key=$lockKey" }
        }
    }

    private fun tryAcquireLock(lockKey: String, lockValue: String): Boolean {
        repeat(MAX_RETRY) { attempt ->
            val result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL)

            if (result == true) {
                return true
            }

            logger.debug { "분산 락 획득 실패 (시도 ${attempt + 1}/$MAX_RETRY): key=$lockKey" }

            if (attempt < MAX_RETRY - 1) {
                Thread.sleep(RETRY_DELAY_MS)
            }
        }
        return false
    }

    private fun releaseLock(lockKey: String, lockValue: String) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, listOf(lockKey), lockValue)
        } catch (e: Exception) {
            logger.warn(e) { "분산 락 해제 중 오류 발생: key=$lockKey" }
        }
    }
}
