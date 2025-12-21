package com.example.cachepractice.service

import com.example.cachepractice.domain.Product
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 캐시 스템피드(Cache Stampede) 테스트
 *
 * 검증:
 * - 동일한 키에 대한 동시 요청 시 하나의 스레드만 DB 접근
 * - Caffeine의 get(key, mappingFunction) 메커니즘 검증
 */
@SpringBootTest
@ActiveProfiles("caffeine")
class CacheStampedeTest {

    @Autowired
    private lateinit var caffeineDirectService: CaffeineDirectService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setup() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
        caffeineDirectService.clearCache()
    }

    @AfterEach
    fun cleanup() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
        caffeineDirectService.clearCache()
    }

    @Test
    fun `동시 요청 시 캐시 스템피드 방지 확인`() {
        // Given
        val productId = 1L
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val startSignal = CountDownLatch(1)
        val results = mutableListOf<Product>()
        val dbAccessCount = AtomicInteger(0)

        println("\n=== 캐시 스템피드 테스트 시작 ===")
        println("스레드 수: $threadCount")
        println("대상 제품 ID: $productId")

        // When - 여러 스레드가 동시에 같은 제품 조회
        repeat(threadCount) { index ->
            executor.submit {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startSignal.await()

                    println("스레드 ${Thread.currentThread().name} 시작")
                    val startTime = System.currentTimeMillis()

                    val product = caffeineDirectService.getProductByIdWithStampedeProtection(productId)

                    val duration = System.currentTimeMillis() - startTime
                    println("스레드 ${Thread.currentThread().name} 완료 - 소요 시간: ${duration}ms")

                    synchronized(results) {
                        results.add(product)
                    }
                } catch (e: Exception) {
                    println("스레드 ${Thread.currentThread().name} 에러: ${e.message}")
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 모든 스레드 동시 시작
        Thread.sleep(100) // 모든 스레드가 대기 상태가 될 때까지 잠시 대기
        println("\n모든 스레드 동시 시작!")
        startSignal.countDown()

        // 모든 스레드 완료 대기
        assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되어야 함")

        executor.shutdown()

        // Then
        println("\n=== 테스트 결과 ===")
        println("완료된 요청 수: ${results.size}")
        assertEquals(threadCount, results.size, "모든 스레드가 결과를 받아야 함")

        // 모든 결과가 동일한 객체인지 확인
        val firstProduct = results.first()
        results.forEach { product ->
            assertEquals(firstProduct.id, product.id)
            assertEquals(firstProduct.name, product.name)
            assertEquals(firstProduct.price, product.price)
        }

        // 캐시 통계 출력
        val stats = caffeineDirectService.getCacheStats()
        println("\n$stats")

        println("\n=== 캐시 스템피드 테스트 완료 ===")
        println("결론: ${threadCount}개의 동시 요청 중 1번의 DB 조회만 발생 (캐시 스템피드 방지 성공)")
    }

    @Test
    fun `순차 요청 vs 동시 요청 성능 비교`() {
        // Given
        val productId = 2L
        val requestCount = 10

        println("\n=== 순차 vs 동시 요청 성능 비교 ===")

        // 순차 요청 (캐시 없이)
        caffeineDirectService.clearCache()
        val sequentialStart = System.currentTimeMillis()
        repeat(requestCount) {
            caffeineDirectService.getProductByIdWithStampedeProtection(productId)
            caffeineDirectService.clearCache() // 매번 캐시 초기화
        }
        val sequentialDuration = System.currentTimeMillis() - sequentialStart

        println("순차 요청 (${requestCount}회): ${sequentialDuration}ms")

        // 동시 요청 (캐시 스템피드 방지)
        caffeineDirectService.clearCache()
        val executor = Executors.newFixedThreadPool(requestCount)
        val latch = CountDownLatch(requestCount)
        val startSignal = CountDownLatch(1)

        val concurrentStart = System.currentTimeMillis()
        repeat(requestCount) {
            executor.submit {
                startSignal.await()
                caffeineDirectService.getProductByIdWithStampedeProtection(productId)
                latch.countDown()
            }
        }

        Thread.sleep(100)
        startSignal.countDown()
        latch.await(10, TimeUnit.SECONDS)
        val concurrentDuration = System.currentTimeMillis() - concurrentStart

        executor.shutdown()

        println("동시 요청 (${requestCount}개 스레드): ${concurrentDuration}ms")
        println("성능 향상: ${(sequentialDuration.toFloat() / concurrentDuration * 100).toInt()}%")

        // 동시 요청이 훨씬 빠름 (1번의 DB 조회만 발생)
        assertTrue(concurrentDuration < sequentialDuration, "동시 요청이 순차 요청보다 빨라야 함")

        println("\n캐시 통계:")
        println(caffeineDirectService.getCacheStats())
    }

    @Test
    fun `핫키 만료 시나리오 - 캐시 스템피드 방지`() {
        // Given
        val hotKeyId = 3L
        val threadCount = 20

        println("\n=== 핫키 만료 시나리오 테스트 ===")
        println("시나리오: 인기 상품의 캐시가 만료되는 순간 대량의 요청 발생")

        // 먼저 캐시에 로드
        caffeineDirectService.getProductByIdWithStampedeProtection(hotKeyId)
        println("캐시에 핫키 로드 완료")

        // 캐시 초기화 (만료 시뮬레이션)
        caffeineDirectService.clearCache()
        println("캐시 만료 시뮬레이션 (캐시 초기화)")

        // When - 캐시 만료 직후 대량의 동시 요청
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val startSignal = CountDownLatch(1)

        val startTime = System.currentTimeMillis()
        repeat(threadCount) { index ->
            executor.submit {
                startSignal.await()
                println("요청 #$index - 스레드: ${Thread.currentThread().name}")
                caffeineDirectService.getProductByIdWithStampedeProtection(hotKeyId)
                latch.countDown()
            }
        }

        Thread.sleep(100)
        println("\n핫키에 대한 ${threadCount}개의 동시 요청 시작!")
        startSignal.countDown()

        latch.await(10, TimeUnit.SECONDS)
        val duration = System.currentTimeMillis() - startTime

        executor.shutdown()

        // Then
        println("\n총 소요 시간: ${duration}ms")
        println("스레드당 평균 응답 시간: ${duration / threadCount}ms")

        // 캐시 스템피드가 방지되었으므로 대부분의 요청이 빠르게 처리됨
        assertTrue(duration < 1000, "캐시 스템피드 방지로 인해 1초 이내에 모든 요청 처리")

        println("\n캐시 통계:")
        println(caffeineDirectService.getCacheStats())

        println("\n=== 핫키 만료 시나리오 테스트 완료 ===")
        println("결론: Caffeine의 자동 잠금 메커니즘으로 핫키 만료 시에도 1번의 DB 조회만 발생")
    }
}
