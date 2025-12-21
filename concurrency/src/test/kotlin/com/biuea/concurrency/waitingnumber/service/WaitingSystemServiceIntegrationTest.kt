package com.biuea.concurrency.waitingnumber.service

import com.biuea.concurrency.support.TestContainersConfig
import com.biuea.concurrency.waitingnumber.repository.ProductWaitingRepository
import com.biuea.concurrency.waitingnumber.repository.WaitingNumberRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WaitingSystemServiceIntegrationTest : TestContainersConfig() {

    @Autowired
    private lateinit var service: WaitingSystemService

    @Autowired
    private lateinit var waitingNumberRepository: WaitingNumberRepository

    @Autowired
    private lateinit var productWaitingRepository: ProductWaitingRepository

    // redisTemplate and jdbcTemplate are now inherited from TestContainersConfig

    @Test
    fun `싱글 스레드 - 대기번호 발급 및 저장`() {
        val result = service.issueWaitingNumber(productId = 1L, userId = 101L)

        assertThat(result.productId).isEqualTo(1L)
        assertThat(result.userId).isEqualTo(101L)
        // initialize seeds 1, then INCR -> 2
        assertThat(result.waitingNumber).isEqualTo("1-2")
        assertThat(waitingNumberRepository.count()).isEqualTo(1)
    }

    @Test
    fun `멀티 스레드 - 높은 동시성에서도 고유하고 순차적인 번호 발급`() {
        val threads = 100
        val results = runConcurrent(threads) { idx ->
            service.issueWaitingNumber(productId = 1L, userId = idx.toLong())
        }

        assertThat(results).hasSize(threads)
        val numbers = results.map { it.waitingNumber }
        assertThat(numbers.toSet()).hasSize(threads)

        assertThat(waitingNumberRepository.count()).isEqualTo(threads.toLong())
    }

    @Test
    fun `Redis 키 누락 또는 만료 - 재초기화 후에도 배치 내 고유성 보장`() {
        val productId = 3L

        // first batch
        runConcurrent(20) { idx ->
            service.issueWaitingNumber(productId = productId, userId = idx.toLong())
        }

        // simulate TTL expiry by deleting the key
        redisTemplate!!.delete("waiting:number:$productId")

        val results = runConcurrent(25) { idx ->
            service.issueWaitingNumber(productId = productId, userId = (1000 + idx).toLong())
        }

        val numbers = results.map { it.waitingNumber }
        assertThat(numbers.toSet()).hasSize(25)
    }

    @Test
    fun `상품 단위 격리 - 각 상품의 락과 시퀀스는 서로 독립적`() {
        val exec = Executors.newFixedThreadPool(50)
        val start = CountDownLatch(1)
        val done = CountDownLatch(100)

        val productA = 10L
        val productB = 11L

        val futuresA = (0 until 50).map { idx ->
            exec.submit(Callable {
                start.await()
                try {
                    service.issueWaitingNumber(productA, idx.toLong())
                } finally {
                    done.countDown()
                }
            })
        }

        val futuresB = (0 until 50).map { idx ->
            exec.submit(Callable {
                start.await()
                try {
                    service.issueWaitingNumber(productB, (100 + idx).toLong())
                } finally {
                    done.countDown()
                }
            })
        }

        start.countDown()  // Start all threads
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue()
        exec.shutdown()

        val resA = futuresA.map { it.get() }
        val resB = futuresB.map { it.get() }

        assertThat(resA.map { it.productId }.toSet()).containsExactly(productA)
        assertThat(resB.map { it.productId }.toSet()).containsExactly(productB)
        assertThat(resA.map { it.waitingNumber }.toSet()).hasSize(50)
        assertThat(resB.map { it.waitingNumber }.toSet()).hasSize(50)
    }

    private fun <T> runConcurrent(n: Int, block: (Int) -> T): List<T> {
        val exec = Executors.newFixedThreadPool(minOf(n, 32))
        val start = CountDownLatch(1)
        val done = CountDownLatch(n)
        val futures = (0 until n).map { idx ->
            exec.submit(Callable {
                start.await()
                try {
                    block(idx)
                } finally {
                    done.countDown()
                }
            })
        }
        start.countDown()
        done.await(60, TimeUnit.SECONDS)
        exec.shutdown()
        return futures.map { it.get() }
    }
}
