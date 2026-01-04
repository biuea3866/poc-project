package com.example.cachepractice.redis.distributedlock

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Redis 분산락 예제
 * Redisson을 이용한 다양한 분산락 패턴
 * - 기본 Lock
 * - Fair Lock (공정한 락)
 * - ReadWrite Lock (읽기/쓰기 락)
 * - RedLock (다중 노드 분산락)
 */
@Service
@Profile("redis")
class RedisDistributedLockService(
    private val redissonClient: RedissonClient
) {

    /**
     * 기본 분산락
     * tryLock을 이용한 락 획득 시도
     */
    fun executeWithLock(
        lockKey: String,
        waitTime: Long = 5,
        leaseTime: Long = 10,
        task: () -> Unit
    ): LockResult {
        val lock = redissonClient.getLock(lockKey)
        val startTime = System.currentTimeMillis()

        return try {
            // tryLock(waitTime, leaseTime, timeUnit)
            // waitTime: 락 획득 대기 시간
            // leaseTime: 락 자동 해제 시간
            val acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)

            if (acquired) {
                try {
                    task()
                    val duration = System.currentTimeMillis() - startTime
                    LockResult(
                        success = true,
                        lockAcquired = true,
                        durationMs = duration,
                        message = "작업 완료"
                    )
                } finally {
                    if (lock.isHeldByCurrentThread) {
                        lock.unlock()
                    }
                }
            } else {
                val duration = System.currentTimeMillis() - startTime
                LockResult(
                    success = false,
                    lockAcquired = false,
                    durationMs = duration,
                    message = "락 획득 실패 (대기 시간 초과)"
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LockResult(
                success = false,
                lockAcquired = false,
                durationMs = duration,
                message = "에러: ${e.message}"
            )
        }
    }

    /**
     * Fair Lock (공정한 락)
     * 요청 순서대로 락을 획득
     */
    fun executeWithFairLock(
        lockKey: String,
        waitTime: Long = 5,
        leaseTime: Long = 10,
        task: () -> Unit
    ): LockResult {
        val fairLock = redissonClient.getFairLock(lockKey)
        val startTime = System.currentTimeMillis()

        return try {
            val acquired = fairLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)

            if (acquired) {
                try {
                    task()
                    val duration = System.currentTimeMillis() - startTime
                    LockResult(
                        success = true,
                        lockAcquired = true,
                        durationMs = duration,
                        message = "Fair Lock으로 작업 완료"
                    )
                } finally {
                    if (fairLock.isHeldByCurrentThread) {
                        fairLock.unlock()
                    }
                }
            } else {
                val duration = System.currentTimeMillis() - startTime
                LockResult(
                    success = false,
                    lockAcquired = false,
                    durationMs = duration,
                    message = "Fair Lock 획득 실패"
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LockResult(
                success = false,
                lockAcquired = false,
                durationMs = duration,
                message = "에러: ${e.message}"
            )
        }
    }

    /**
     * ReadWrite Lock
     * 읽기와 쓰기를 구분하여 락 획득
     * 읽기는 여러 스레드가 동시 접근 가능, 쓰기는 배타적 접근
     */
    fun executeWithReadLock(lockKey: String, task: () -> Unit): LockResult {
        val rwLock = redissonClient.getReadWriteLock(lockKey)
        val readLock = rwLock.readLock()
        val startTime = System.currentTimeMillis()

        return try {
            val acquired = readLock.tryLock(5, 10, TimeUnit.SECONDS)

            if (acquired) {
                try {
                    task()
                    val duration = System.currentTimeMillis() - startTime
                    LockResult(
                        success = true,
                        lockAcquired = true,
                        durationMs = duration,
                        message = "Read Lock으로 작업 완료 (읽기 전용)"
                    )
                } finally {
                    if (readLock.isHeldByCurrentThread) {
                        readLock.unlock()
                    }
                }
            } else {
                val duration = System.currentTimeMillis() - startTime
                LockResult(
                    success = false,
                    lockAcquired = false,
                    durationMs = duration,
                    message = "Read Lock 획득 실패"
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LockResult(
                success = false,
                lockAcquired = false,
                durationMs = duration,
                message = "에러: ${e.message}"
            )
        }
    }

    fun executeWithWriteLock(lockKey: String, task: () -> Unit): LockResult {
        val rwLock = redissonClient.getReadWriteLock(lockKey)
        val writeLock = rwLock.writeLock()
        val startTime = System.currentTimeMillis()

        return try {
            val acquired = writeLock.tryLock(5, 10, TimeUnit.SECONDS)

            if (acquired) {
                try {
                    task()
                    val duration = System.currentTimeMillis() - startTime
                    LockResult(
                        success = true,
                        lockAcquired = true,
                        durationMs = duration,
                        message = "Write Lock으로 작업 완료 (쓰기 전용)"
                    )
                } finally {
                    if (writeLock.isHeldByCurrentThread) {
                        writeLock.unlock()
                    }
                }
            } else {
                val duration = System.currentTimeMillis() - startTime
                LockResult(
                    success = false,
                    lockAcquired = false,
                    durationMs = duration,
                    message = "Write Lock 획득 실패"
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LockResult(
                success = false,
                lockAcquired = false,
                durationMs = duration,
                message = "에러: ${e.message}"
            )
        }
    }

    /**
     * 재고 차감 시뮬레이션
     * 분산락을 이용한 동시성 제어
     */
    private val stockMap = mutableMapOf<Long, Int>()

    fun initializeStock(productId: Long, stock: Int) {
        stockMap[productId] = stock
    }

    fun decreaseStock(productId: Long, quantity: Int): StockResult {
        val lockKey = "stock:$productId"

        var result: StockResult? = null
        val lockResult = executeWithLock(lockKey, waitTime = 3, leaseTime = 5) {
            val currentStock = stockMap[productId] ?: 0

            if (currentStock >= quantity) {
                // 재고 차감
                Thread.sleep(100) // 작업 시뮬레이션
                stockMap[productId] = currentStock - quantity
                result = StockResult(
                    success = true,
                    productId = productId,
                    requestedQuantity = quantity,
                    remainingStock = stockMap[productId] ?: 0,
                    message = "재고 차감 성공"
                )
            } else {
                result = StockResult(
                    success = false,
                    productId = productId,
                    requestedQuantity = quantity,
                    remainingStock = currentStock,
                    message = "재고 부족"
                )
            }
        }

        return result ?: StockResult(
            success = false,
            productId = productId,
            requestedQuantity = quantity,
            remainingStock = stockMap[productId] ?: 0,
            message = "락 획득 실패: ${lockResult.message}"
        )
    }

    fun getStock(productId: Long): Int {
        return stockMap[productId] ?: 0
    }

    fun clearStock() {
        stockMap.clear()
    }
}

data class LockResult(
    val success: Boolean,
    val lockAcquired: Boolean,
    val durationMs: Long,
    val message: String
)

data class StockResult(
    val success: Boolean,
    val productId: Long,
    val requestedQuantity: Int,
    val remainingStock: Int,
    val message: String
)
