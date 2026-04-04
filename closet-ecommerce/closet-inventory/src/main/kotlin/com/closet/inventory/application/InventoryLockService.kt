package com.closet.inventory.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import mu.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Redisson 분산 락을 이용한 재고 조작 래퍼.
 *
 * SKU 단위 세밀 잠금(inventory:lock:{productOptionId})으로 동시성을 제어하고,
 * 엔티티의 @Version 낙관적 락과 이중 잠금으로 데이터 정합성을 보장한다.
 */
@Component
class InventoryLockService(
    private val redissonClient: RedissonClient,
) {
    companion object {
        private const val LOCK_KEY_PREFIX = "inventory:lock:"
        private const val WAIT_TIME = 3L
        private const val LEASE_TIME = -1L // Watch Dog 자동 갱신
    }

    /**
     * 분산 락을 획득한 후 block()을 실행한다.
     *
     * @param productOptionId 재고 잠금 대상 (SKU 단위)
     * @param block 락 내에서 실행할 비즈니스 로직
     * @throws BusinessException 락 획득 실패 시
     */
    fun <T> withLock(productOptionId: Long, block: () -> T): T {
        val lockKey = "$LOCK_KEY_PREFIX$productOptionId"
        val lock = redissonClient.getLock(lockKey)

        val acquired = try {
            lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw BusinessException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "재고 락 획득 중 인터럽트가 발생했습니다. productOptionId=$productOptionId"
            )
        }

        if (!acquired) {
            logger.warn { "재고 락 획득 실패: productOptionId=$productOptionId" }
            throw BusinessException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "재고 락 획득에 실패했습니다. 잠시 후 다시 시도해주세요. productOptionId=$productOptionId"
            )
        }

        return try {
            block()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
