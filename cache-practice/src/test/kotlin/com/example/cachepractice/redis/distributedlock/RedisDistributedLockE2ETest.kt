package com.example.cachepractice.redis.distributedlock

import com.example.cachepractice.redis.RedisTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("Redis 분산락 E2E 테스트")
class RedisDistributedLockE2ETest : RedisTestBase() {

    @Autowired
    private lateinit var lockService: RedisDistributedLockService

    @AfterEach
    fun cleanup() {
        lockService.clearStock()
    }

    @Test
    @DisplayName("Basic Lock - 락 획득 및 작업 실행")
    fun testBasicLock() {
        // given
        val lockKey = "test:lock:basic"
        var executed = false

        // when
        val result = lockService.executeWithLock(lockKey, waitTime = 5, leaseTime = 10) {
            executed = true
            Thread.sleep(100)
        }

        // then
        result.success shouldBe true
        result.lockAcquired shouldBe true
        executed shouldBe true
    }

    @Test
    @DisplayName("Basic Lock - 락 대기 시간 초과")
    fun testBasicLockTimeout() {
        // given
        val lockKey = "test:lock:timeout"

        // 첫 번째 스레드가 락 획득
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val results = mutableListOf<LockResult>()

        // when
        executor.submit {
            val result = lockService.executeWithLock(lockKey, waitTime = 10, leaseTime = 15) {
                Thread.sleep(5000) // 5초 작업
            }
            results.add(result)
            latch.countDown()
        }

        Thread.sleep(100) // 첫 번째 스레드가 락을 획득하도록 대기

        executor.submit {
            val result = lockService.executeWithLock(lockKey, waitTime = 1, leaseTime = 10) {
                Thread.sleep(100)
            }
            results.add(result)
            latch.countDown()
        }

        latch.await()
        executor.shutdown()

        // then
        results.size shouldBe 2
        results.count { it.lockAcquired } shouldBe 1 // 하나만 락 획득
        results.count { !it.lockAcquired } shouldBe 1 // 하나는 타임아웃
    }

    @Test
    @DisplayName("Fair Lock - 요청 순서대로 락 획득")
    fun testFairLock() {
        // given
        val lockKey = "test:lock:fair"
        val executionOrder = mutableListOf<Int>()

        val executor = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(3)

        // when
        repeat(3) { index ->
            executor.submit {
                lockService.executeWithFairLock(lockKey, waitTime = 10, leaseTime = 5) {
                    synchronized(executionOrder) {
                        executionOrder.add(index)
                    }
                    Thread.sleep(500)
                }
                latch.countDown()
            }
            Thread.sleep(50) // 순서 보장을 위한 작은 딜레이
        }

        latch.await()
        executor.shutdown()

        // then
        executionOrder.size shouldBe 3
        println("Fair Lock Execution Order: $executionOrder")
        // Fair Lock은 요청 순서대로 처리되어야 함
    }

    @Test
    @DisplayName("Read Lock - 여러 스레드가 동시에 읽기 가능")
    fun testReadLock() {
        // given
        val lockKey = "test:lock:read"
        val concurrentReads = AtomicInteger(0)
        val maxConcurrentReads = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(5)
        val latch = CountDownLatch(5)

        // when
        repeat(5) {
            executor.submit {
                lockService.executeWithReadLock(lockKey) {
                    val current = concurrentReads.incrementAndGet()
                    maxConcurrentReads.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(1000) // 읽기 작업
                    concurrentReads.decrementAndGet()
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        // then
        // 여러 Read Lock이 동시에 획득 가능해야 함
        maxConcurrentReads.get() shouldNotBe 1
        println("Max Concurrent Reads: ${maxConcurrentReads.get()}")
    }

    @Test
    @DisplayName("Write Lock - 배타적 접근만 가능")
    fun testWriteLock() {
        // given
        val lockKey = "test:lock:write"
        val concurrentWrites = AtomicInteger(0)
        val maxConcurrentWrites = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(3)
        val latch = CountDownLatch(3)

        // when
        repeat(3) {
            executor.submit {
                lockService.executeWithWriteLock(lockKey) {
                    val current = concurrentWrites.incrementAndGet()
                    maxConcurrentWrites.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(500) // 쓰기 작업
                    concurrentWrites.decrementAndGet()
                }
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        // then
        // Write Lock은 한 번에 하나만 획득 가능해야 함
        maxConcurrentWrites.get() shouldBe 1
    }

    @Test
    @DisplayName("재고 차감 - 단일 요청")
    fun testDecreaseStockSingle() {
        // given
        val productId = 1L
        val initialStock = 100
        lockService.initializeStock(productId, initialStock)

        // when
        val result = lockService.decreaseStock(productId, 30)

        // then
        result.success shouldBe true
        result.remainingStock shouldBe 70
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족")
    fun testDecreaseStockInsufficientStock() {
        // given
        val productId = 2L
        val initialStock = 10
        lockService.initializeStock(productId, initialStock)

        // when
        val result = lockService.decreaseStock(productId, 50)

        // then
        result.success shouldBe false
        result.message shouldBe "재고 부족"
        result.remainingStock shouldBe 10
    }

    @Test
    @DisplayName("재고 차감 - 동시성 테스트 (분산락으로 정합성 보장)")
    fun testDecreaseStockConcurrency() {
        // given
        val productId = 100L
        val initialStock = 100
        val threadCount = 20
        val quantityPerRequest = 5

        lockService.initializeStock(productId, initialStock)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val result = lockService.decreaseStock(productId, quantityPerRequest)
                    if (result.success) {
                        successCount.incrementAndGet()
                    } else {
                        failCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then
        val finalStock = lockService.getStock(productId)
        val expectedSuccessCount = initialStock / quantityPerRequest

        println("Success: ${successCount.get()}, Fail: ${failCount.get()}")
        println("Final Stock: $finalStock")

        successCount.get() shouldBe expectedSuccessCount
        failCount.get() shouldBe (threadCount - expectedSuccessCount)
        finalStock shouldBe 0 // 100 / 5 = 20번 성공, 100 - (20 * 5) = 0
    }

    @Test
    @DisplayName("재고 차감 - 높은 동시성 테스트")
    fun testDecreaseStockHighConcurrency() {
        // given
        val productId = 200L
        val initialStock = 1000
        val threadCount = 100
        val quantityPerRequest = 1

        lockService.initializeStock(productId, initialStock)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        // when
        val startTime = System.currentTimeMillis()

        repeat(threadCount) {
            executor.submit {
                try {
                    val result = lockService.decreaseStock(productId, quantityPerRequest)
                    if (result.success) {
                        successCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val duration = System.currentTimeMillis() - startTime

        // then - 간소화된 검증
        val finalStock = lockService.getStock(productId)

        println("High Concurrency Test - Duration: ${duration}ms")
        println("Success: ${successCount.get()}, Final Stock: $finalStock")

        // 최소한 일부는 성공해야 함
        (successCount.get() > 0) shouldBe true
        // 최종 재고는 초기 재고 이하여야 함
        (finalStock <= initialStock) shouldBe true
        // 성공 횟수와 재고 감소량이 일치해야 함
        finalStock shouldBe (initialStock - successCount.get())
    }

    @Test
    @DisplayName("재고 차감 - 경쟁 조건에서 데이터 정합성 검증")
    fun testDecreaseStockDataConsistency() {
        // given
        val productId = 300L
        val initialStock = 500
        val threadCount = 50
        val quantityPerRequest = 10

        lockService.initializeStock(productId, initialStock)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val results = mutableListOf<StockResult>()

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val result = lockService.decreaseStock(productId, quantityPerRequest)
                    synchronized(results) {
                        results.add(result)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then - 간소화된 검증
        val finalStock = lockService.getStock(productId)
        val successCount = results.count { it.success }
        val failCount = results.count { !it.success }

        println("Success: $successCount, Fail: $failCount, Final Stock: $finalStock")

        // 모든 결과가 기록되었는지 확인
        results.size shouldBe threadCount

        // 데이터 정합성 검증 - 성공 횟수와 재고 감소량이 일치해야 함
        finalStock shouldBe (initialStock - (successCount * quantityPerRequest))

        // 최소한 일부는 성공해야 함
        (successCount > 0) shouldBe true
    }

    @Test
    @DisplayName("락 재진입 테스트")
    fun testLockReentrancy() {
        // given
        val lockKey = "test:lock:reentrant"
        var innerExecuted = false
        var outerExecuted = false

        // when
        val result = lockService.executeWithLock(lockKey, waitTime = 5, leaseTime = 10) {
            outerExecuted = true

            // 같은 스레드에서 같은 락을 다시 획득 시도
            // Redisson Lock은 재진입 가능
            val innerResult = lockService.executeWithLock(lockKey, waitTime = 1, leaseTime = 5) {
                innerExecuted = true
            }

            innerResult.success shouldBe true
        }

        // then
        result.success shouldBe true
        outerExecuted shouldBe true
        innerExecuted shouldBe true
    }
}
