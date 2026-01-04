package com.biuea.kotlinpractice.async

import com.biuea.kotlinpractice.domain.Order
import com.biuea.kotlinpractice.domain.OrderStatus
import com.biuea.kotlinpractice.domain.Product
import com.biuea.kotlinpractice.repository.OrderRepository
import com.biuea.kotlinpractice.repository.ProductRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock as mutexWithLock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock as lockWithLock
import kotlin.system.measureTimeMillis

/**
 * 데이터베이스 성능 비교 예제
 *
 * 스레드, 코루틴, 버추얼 스레드를 사용한 DB 접근 성능 비교
 *
 * Self-invocation 문제 해결:
 * - 트랜잭션 메서드를 OrderTransactionService로 분리
 * - 프록시가 정상적으로 작동하도록 외부 서비스 호출
 */
@Service
class DatabasePerformanceExample(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderTransactionService: OrderTransactionService
) {

    /**
     * 초기 데이터 생성
     */
    @Transactional
    fun initializeData() {
        println("\n=== 초기 데이터 생성 ===")

        // 기존 데이터 삭제
        orderRepository.deleteAll()
        productRepository.deleteAll()

        // 상품 데이터 생성
        val products = (1..100).map { i ->
            Product(
                name = "Product-$i",
                price = BigDecimal.valueOf(1000L * i),
                stock = 100
            )
        }
        val savedProducts = productRepository.saveAll(products)
        println("생성된 상품: ${savedProducts.size}개")
    }

    /**
     * 리소스 사용량 측정
     */
    data class ResourceMetrics(
        val executionTimeMs: Long,
        val memoryUsedMB: Long,
        val threadCount: Int,
        val recordsProcessed: Int
    ) {
        fun print(label: String) {
            println("[$label]")
            println("  실행 시간: ${executionTimeMs}ms")
            println("  메모리 사용: ${memoryUsedMB}MB")
            println("  스레드 수: $threadCount")
            println("  처리 레코드: ${recordsProcessed}개")
            println("  초당 처리량: ${(recordsProcessed.toDouble() / (executionTimeMs / 1000.0)).toInt()}/s")
        }
    }

    private fun measureResources(block: () -> Int): ResourceMetrics {
        val runtime = Runtime.getRuntime()
        System.gc()
        Thread.sleep(100)

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        val initialThreadCount = Thread.activeCount()

        val recordsProcessed: Int
        val executionTime = measureTimeMillis {
            recordsProcessed = block()
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val finalThreadCount = Thread.activeCount()

        return ResourceMetrics(
            executionTimeMs = executionTime,
            memoryUsedMB = (finalMemory - initialMemory) / 1024 / 1024,
            threadCount = maxOf(finalThreadCount - initialThreadCount, 0),
            recordsProcessed = recordsProcessed
        )
    }

    /**
     * 1. 플랫폼 스레드를 사용한 DB 접근
     */
    fun platformThreadExample(concurrency: Int = 10): ResourceMetrics {
        println("\n=== 플랫폼 스레드 DB 접근 (동시성: $concurrency) ===")

        return measureResources {
            val executor = Executors.newFixedThreadPool(concurrency)
            var processed = 0

            try {
                val products = productRepository.findAll()
                val futures = products.map { product ->
                    executor.submit {
                        orderTransactionService.createOrderInNewTransaction(product, 1)
                        synchronized(this) { processed++ }
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }

            processed
        }
    }

    /**
     * 2. 코루틴을 사용한 DB 접근
     *
     * 코루틴 동기화:
     * - synchronized 대신 Mutex 사용 (non-blocking)
     * - 코루틴이 suspend되어 스레드를 블로킹하지 않음
     */
    fun coroutineExample(concurrency: Int = 10): ResourceMetrics {
        println("\n=== 코루틴 DB 접근 (동시성: $concurrency) ===")

        return measureResources {
            runBlocking {
                val products = productRepository.findAll()
                val limitedDispatcher = Dispatchers.IO.limitedParallelism(concurrency)
                val mutex = Mutex()
                var processed = 0

                val jobs = products.map { product ->
                    async(limitedDispatcher) {
                        orderTransactionService.createOrderInNewTransaction(product, 1)
                        mutex.mutexWithLock { processed++ }
                    }
                }

                jobs.awaitAll()
                processed
            }
        }
    }

    /**
     * 3. 버추얼 스레드를 사용한 DB 접근
     *
     * Pinning 문제 방지:
     * - synchronized 대신 ReentrantLock 사용
     * - virtual thread가 carrier thread에 고정되지 않음
     */
    fun virtualThreadExample(): ResourceMetrics {
        println("\n=== 버추얼 스레드 DB 접근 ===")

        return measureResources {
            val lock = ReentrantLock()
            var processed = 0

            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val products = productRepository.findAll()
                val futures = products.map { product ->
                    executor.submit {
                        orderTransactionService.createOrderInNewTransaction(product, 1)
                        lock.lockWithLock { processed++ }
                    }
                }
                futures.forEach { it.get() }
            }

            processed
        }
    }

    /**
     * 대량 조회 성능 테스트 - 플랫폼 스레드
     */
    fun bulkReadPlatformThread(readCount: Int = 1000): ResourceMetrics {
        println("\n=== 대량 조회 (플랫폼 스레드) - $readCount 회 ===")

        return measureResources {
            val executor = Executors.newFixedThreadPool(100)
            var processed = 0

            try {
                val futures = (1..readCount).map {
                    executor.submit {
                        productRepository.findAll()
                        synchronized(this) { processed++ }
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }

            processed
        }
    }

    /**
     * 대량 조회 성능 테스트 - 코루틴
     */
    fun bulkReadCoroutine(readCount: Int = 1000): ResourceMetrics {
        println("\n=== 대량 조회 (코루틴) - $readCount 회 ===")

        return measureResources {
            runBlocking {
                val mutex = Mutex()
                var processed = 0

                val jobs = (1..readCount).map {
                    async(Dispatchers.IO) {
                        productRepository.findAll()
                        mutex.mutexWithLock { processed++ }
                    }
                }

                jobs.awaitAll()
                processed
            }
        }
    }

    /**
     * 대량 조회 성능 테스트 - 버추얼 스레드
     */
    fun bulkReadVirtualThread(readCount: Int = 1000): ResourceMetrics {
        println("\n=== 대량 조회 (버추얼 스레드) - $readCount 회 ===")

        return measureResources {
            val lock = ReentrantLock()
            var processed = 0

            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = (1..readCount).map {
                    executor.submit {
                        productRepository.findAll()
                        lock.lockWithLock { processed++ }
                    }
                }
                futures.forEach { it.get() }
            }

            processed
        }
    }

    /**
     * 복잡한 비즈니스 로직 - 주문 처리
     */
    data class OrderRequest(
        val customerName: String,
        val productId: Long,
        val quantity: Int
    )

    fun processOrdersPlatformThread(orders: List<OrderRequest>): ResourceMetrics {
        println("\n=== 주문 처리 (플랫폼 스레드) - ${orders.size}건 ===")

        return measureResources {
            val executor = Executors.newFixedThreadPool(50)
            var processed = 0

            try {
                val futures = orders.map { request ->
                    executor.submit {
                        orderTransactionService.processOrderInNewTransaction(request)
                        synchronized(this) { processed++ }
                    }
                }
                futures.forEach { it.get() }
            } finally {
                executor.shutdown()
            }

            processed
        }
    }

    fun processOrdersCoroutine(orders: List<OrderRequest>): ResourceMetrics {
        println("\n=== 주문 처리 (코루틴) - ${orders.size}건 ===")

        return measureResources {
            runBlocking {
                val mutex = Mutex()
                var processed = 0

                val jobs = orders.map { request ->
                    async(Dispatchers.IO) {
                        orderTransactionService.processOrderInNewTransaction(request)
                        mutex.mutexWithLock { processed++ }
                    }
                }

                jobs.awaitAll()
                processed
            }
        }
    }

    fun processOrdersVirtualThread(orders: List<OrderRequest>): ResourceMetrics {
        println("\n=== 주문 처리 (버추얼 스레드) - ${orders.size}건 ===")

        return measureResources {
            val lock = ReentrantLock()
            var processed = 0

            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                val futures = orders.map { request ->
                    executor.submit {
                        orderTransactionService.processOrderInNewTransaction(request)
                        lock.lockWithLock { processed++ }
                    }
                }
                futures.forEach { it.get() }
            }

            processed
        }
    }

    /**
     * 통계 조회
     */
    fun printStatistics() {
        println("\n=== 데이터베이스 통계 ===")
        println("전체 상품 수: ${productRepository.count()}")
        println("전체 주문 수: ${orderRepository.count()}")
        println("대기 중인 주문: ${orderRepository.findByStatus(OrderStatus.PENDING).size}")
        println("확정된 주문: ${orderRepository.findByStatus(OrderStatus.CONFIRMED).size}")
    }
}
