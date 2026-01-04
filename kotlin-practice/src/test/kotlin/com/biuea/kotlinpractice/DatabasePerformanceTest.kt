package com.biuea.kotlinpractice

import com.biuea.kotlinpractice.async.DatabasePerformanceExample
import com.biuea.kotlinpractice.repository.OrderRepository
import com.biuea.kotlinpractice.repository.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class DatabasePerformanceTest {

    @Autowired
    private lateinit var performanceExample: DatabasePerformanceExample

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @BeforeEach
    fun setup() {
        // 테스트 전 데이터 초기화
        performanceExample.initializeData()
    }

    @Test
    fun `데이터베이스 기본 동작 확인`() {
        println("\n=== 데이터베이스 기본 동작 확인 ===")

        val productCount = productRepository.count()
        val orderCount = orderRepository.count()

        println("상품 수: $productCount")
        println("주문 수: $orderCount")

        assert(productCount > 0)
    }

    @Test
    fun `플랫폼 스레드 DB 접근 테스트`() {
        val metrics = performanceExample.platformThreadExample(concurrency = 10)
        metrics.print("플랫폼 스레드")

        performanceExample.printStatistics()
        assert(metrics.recordsProcessed > 0)
    }

    @Test
    fun `코루틴 DB 접근 테스트`() {
        val metrics = performanceExample.coroutineExample(concurrency = 10)
        metrics.print("코루틴")

        performanceExample.printStatistics()
        assert(metrics.recordsProcessed > 0)
    }

    @Test
    fun `버추얼 스레드 DB 접근 테스트`() {
        val metrics = performanceExample.virtualThreadExample()
        metrics.print("버추얼 스레드")

        performanceExample.printStatistics()
        assert(metrics.recordsProcessed > 0)
    }

    @Test
    fun `대량 조회 성능 비교`() {
        println("\n=== 대량 조회 성능 비교 ===\n")

        val readCount = 500

        val platformMetrics = performanceExample.bulkReadPlatformThread(readCount)
        platformMetrics.print("플랫폼 스레드 대량 조회")

        val coroutineMetrics = performanceExample.bulkReadCoroutine(readCount)
        coroutineMetrics.print("코루틴 대량 조회")

        val virtualMetrics = performanceExample.bulkReadVirtualThread(readCount)
        virtualMetrics.print("버추얼 스레드 대량 조회")

        printComparison(platformMetrics, coroutineMetrics, virtualMetrics)
    }

    @Test
    fun `주문 처리 성능 비교`() {
        println("\n=== 주문 처리 성능 비교 ===\n")

        // 실제 상품 ID 조회
        val productIds = productRepository.findAll().map { it.id!! }
        val orderCount = 100
        val orders = (1..orderCount).map { i ->
            DatabasePerformanceExample.OrderRequest(
                customerName = "Customer-$i",
                productId = productIds[i % productIds.size],
                quantity = 1
            )
        }

        val platformMetrics = performanceExample.processOrdersPlatformThread(orders)
        platformMetrics.print("플랫폼 스레드 주문 처리")

        // 데이터 초기화
        performanceExample.initializeData()

        // 다시 상품 ID 조회
        val productIds2 = productRepository.findAll().map { it.id!! }
        val orders2 = (1..orderCount).map { i ->
            DatabasePerformanceExample.OrderRequest(
                customerName = "Customer-$i",
                productId = productIds2[i % productIds2.size],
                quantity = 1
            )
        }

        val coroutineMetrics = performanceExample.processOrdersCoroutine(orders2)
        coroutineMetrics.print("코루틴 주문 처리")

        // 데이터 초기화
        performanceExample.initializeData()

        // 다시 상품 ID 조회
        val productIds3 = productRepository.findAll().map { it.id!! }
        val orders3 = (1..orderCount).map { i ->
            DatabasePerformanceExample.OrderRequest(
                customerName = "Customer-$i",
                productId = productIds3[i % productIds3.size],
                quantity = 1
            )
        }

        val virtualMetrics = performanceExample.processOrdersVirtualThread(orders3)
        virtualMetrics.print("버추얼 스레드 주문 처리")

        printComparison(platformMetrics, coroutineMetrics, virtualMetrics)
    }

    @Test
    fun `전체 성능 비교 종합 테스트`() {
        println("\n" + "=".repeat(60))
        println("전체 성능 비교 종합 테스트")
        println("=".repeat(60))

        // 1. 기본 DB 접근
        println("\n[1] 기본 DB 접근 (동시성 10)")
        val p1 = performanceExample.platformThreadExample(10)
        performanceExample.initializeData()
        val c1 = performanceExample.coroutineExample(10)
        performanceExample.initializeData()
        val v1 = performanceExample.virtualThreadExample()
        performanceExample.initializeData()

        p1.print("플랫폼 스레드")
        c1.print("코루틴")
        v1.print("버추얼 스레드")
        printComparison(p1, c1, v1)

        // 2. 대량 조회
        println("\n[2] 대량 조회 (500회)")
        val p2 = performanceExample.bulkReadPlatformThread(500)
        val c2 = performanceExample.bulkReadCoroutine(500)
        val v2 = performanceExample.bulkReadVirtualThread(500)

        p2.print("플랫폼 스레드")
        c2.print("코루틴")
        v2.print("버추얼 스레드")
        printComparison(p2, c2, v2)

        println("\n" + "=".repeat(60))
        println("테스트 완료")
        println("=".repeat(60))
    }

    private fun printComparison(
        platform: DatabasePerformanceExample.ResourceMetrics,
        coroutine: DatabasePerformanceExample.ResourceMetrics,
        virtual: DatabasePerformanceExample.ResourceMetrics
    ) {
        println("\n--- 비교 분석 ---")

        val fastest = listOf(
            "플랫폼 스레드" to platform.executionTimeMs,
            "코루틴" to coroutine.executionTimeMs,
            "버추얼 스레드" to virtual.executionTimeMs
        ).minByOrNull { it.second }

        val leastMemory = listOf(
            "플랫폼 스레드" to platform.memoryUsedMB,
            "코루틴" to coroutine.memoryUsedMB,
            "버추얼 스레드" to virtual.memoryUsedMB
        ).minByOrNull { it.second }

        println("가장 빠른 방식: ${fastest?.first} (${fastest?.second}ms)")
        println("메모리 효율적: ${leastMemory?.first} (${leastMemory?.second}MB)")

        val baseTime = platform.executionTimeMs.toDouble()
        println("코루틴 속도: ${(baseTime / coroutine.executionTimeMs).format(2)}배")
        println("버추얼 스레드 속도: ${(baseTime / virtual.executionTimeMs).format(2)}배")
        println()
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
