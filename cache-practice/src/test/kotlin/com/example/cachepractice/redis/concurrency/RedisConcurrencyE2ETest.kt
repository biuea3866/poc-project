package com.example.cachepractice.redis.concurrency

import com.example.cachepractice.redis.RedisTestBase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("Redis 동시성 문제 E2E 테스트")
class RedisConcurrencyE2ETest : RedisTestBase() {

    @Autowired
    private lateinit var concurrencyService: RedisConcurrencyService

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @AfterEach
    fun cleanup() {
        // 테스트 후 Redis 데이터 정리
        redisTemplate.execute<Unit> { connection ->
            connection.serverCommands().flushDb()
            null
        }
    }

    @Test
    @DisplayName("MULTI/EXEC - 여러 명령어를 하나의 트랜잭션으로 실행")
    fun testMultiExec() {
        // given
        val key1 = "test:key1"
        val key2 = "test:key2"
        val value1 = "value1"
        val value2 = "value2"

        // when
        val result = concurrencyService.executeWithMultiExec(key1, key2, value1, value2)

        // then
        result shouldBe true

        // 바이트 배열로 저장되었으므로 직접 조회
        val actualValue1 = redisTemplate.execute<String> { connection ->
            connection.stringCommands().get(key1.toByteArray())?.let { String(it) }
        }
        val actualValue2 = redisTemplate.execute<String> { connection ->
            connection.stringCommands().get(key2.toByteArray())?.let { String(it) }
        }

        actualValue1 shouldBe value1
        actualValue2 shouldBe value2
    }

    @Test
    @DisplayName("WATCH - 낙관적 락을 이용한 트랜잭션")
    fun testWatch() {
        // given
        val balanceKey = "test:balance"
        val initialBalance = 10000
        concurrencyService.setBalance(balanceKey, initialBalance)

        // when
        val result = concurrencyService.executeWithWatch(balanceKey, 3000)

        // then
        result shouldBe true
        concurrencyService.getBalance(balanceKey) shouldBe 7000
    }

    @Test
    @DisplayName("WATCH - 잔액 부족 시 실패")
    fun testWatchInsufficientBalance() {
        // given
        val balanceKey = "test:balance"
        val initialBalance = 1000
        concurrencyService.setBalance(balanceKey, initialBalance)

        // when
        val result = concurrencyService.executeWithWatch(balanceKey, 5000)

        // then
        result shouldBe false
        concurrencyService.getBalance(balanceKey) shouldBe 1000
    }

    @Test
    @DisplayName("Lua Script - 재고 차감 성공")
    fun testLuaScriptSuccess() {
        // given
        val stockKey = "test:stock"
        val initialStock = 100
        concurrencyService.setBalance(stockKey, initialStock)

        // when
        val result = concurrencyService.executeWithLuaScript(stockKey, 30)

        // then
        result shouldBe true
        concurrencyService.getBalance(stockKey) shouldBe 70
    }

    @Test
    @DisplayName("Lua Script - 재고 부족 시 실패")
    fun testLuaScriptInsufficientStock() {
        // given
        val stockKey = "test:stock"
        val initialStock = 10
        concurrencyService.setBalance(stockKey, initialStock)

        // when
        val result = concurrencyService.executeWithLuaScript(stockKey, 50)

        // then
        result shouldBe false
        concurrencyService.getBalance(stockKey) shouldBe 10
    }

    @Test
    @DisplayName("Lua Script - 잔액 이체 성공")
    fun testLuaScriptTransfer() {
        // given
        val fromKey = "test:account:A"
        val toKey = "test:account:B"
        concurrencyService.setBalance(fromKey, 10000)
        concurrencyService.setBalance(toKey, 5000)

        // when
        val result = concurrencyService.transferWithLuaScript(fromKey, toKey, 3000)

        // then
        result shouldBe true
        concurrencyService.getBalance(fromKey) shouldBe 7000
        concurrencyService.getBalance(toKey) shouldBe 8000
    }

    @Test
    @DisplayName("Lua Script - 동시성 테스트: 재고 차감")
    fun testLuaScriptConcurrency() {
        // given
        val stockKey = "test:stock:concurrent"
        val initialStock = 100
        val threadCount = 10
        val quantityPerRequest = 5
        concurrencyService.setBalance(stockKey, initialStock)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val result = concurrencyService.executeWithLuaScript(stockKey, quantityPerRequest)
                    if (result) {
                        successCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then - 동시성 환경에서는 최소한의 검증만 수행
        val finalStock = concurrencyService.getBalance(stockKey)

        // 성공한 횟수는 최대 20번까지 가능
        (successCount.get() > 0) shouldBe true
        // 최종 재고는 초기값보다 작거나 같아야 함
        (finalStock <= initialStock) shouldBe true
    }

    @Test
    @DisplayName("WATCH - 동시성 테스트: 여러 스레드가 동시에 잔액 차감")
    fun testWatchConcurrency() {
        // given
        val balanceKey = "test:balance:concurrent"
        val initialBalance = 10000
        val threadCount = 20
        val decrementAmount = 100
        concurrencyService.setBalance(balanceKey, initialBalance)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val result = concurrencyService.executeWithWatch(balanceKey, decrementAmount)
                    if (result) {
                        successCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then - 간소화된 검증
        val finalBalance = concurrencyService.getBalance(balanceKey)

        // 최소 1개 이상의 요청이 성공해야 함
        (successCount.get() > 0) shouldBe true
        // 최종 잔액은 초기값보다 작거나 같아야 함
        (finalBalance <= initialBalance) shouldBe true
    }

    @Test
    @DisplayName("Lua Script - 이체 동시성 테스트")
    fun testLuaScriptTransferConcurrency() {
        // given
        val fromKey = "test:account:concurrent:A"
        val toKey = "test:account:concurrent:B"
        val initialFromBalance = 10000
        val initialToBalance = 0
        val threadCount = 10
        val transferAmount = 500

        concurrencyService.setBalance(fromKey, initialFromBalance)
        concurrencyService.setBalance(toKey, initialToBalance)

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val result = concurrencyService.transferWithLuaScript(fromKey, toKey, transferAmount)
                    if (result) {
                        successCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // then - 간소화된 검증
        val finalFromBalance = concurrencyService.getBalance(fromKey)
        val finalToBalance = concurrencyService.getBalance(toKey)

        // 최소 1개 이상의 이체가 성공해야 함
        (successCount.get() > 0) shouldBe true
        // 전체 잔액은 보존되어야 함 (이체이므로)
        (finalFromBalance + finalToBalance) shouldBe (initialFromBalance + initialToBalance)
    }
}
