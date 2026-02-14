package com.biuea.kotlinpractice.async

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Java 병렬 처리 5가지 기법 예제
 * 1. ForkJoinPool (분할 정복)
 * 2. ParallelStream
 * 3. CompletableFuture (비동기 조합)
 * 4. ExecutorService (스레드 풀)
 * 5. CountDownLatch (동기화)
 */
object ParallelProcessingExample {

    // ========================================
    // 1. ForkJoinPool - 분할 정복으로 합산
    // ========================================

    /**
     * RecursiveTask를 이용한 분할 정복 합산
     * 1~1,000,000 까지의 합을 작은 단위로 쪼개서 병렬 계산
     */
    class SumTask(
        private val numbers: LongArray,
        private val start: Int,
        private val end: Int,
    ) : RecursiveTask<Long>() {

        companion object {
            const val THRESHOLD = 10_000
        }

        override fun compute(): Long {
            if (end - start <= THRESHOLD) {
                var sum = 0L
                for (i in start until end) {
                    sum += numbers[i]
                }
                return sum
            }

            val mid = (start + end) / 2
            val left = SumTask(numbers, start, mid)
            val right = SumTask(numbers, mid, end)

            left.fork()
            val rightResult = right.compute()
            val leftResult = left.join()

            return leftResult + rightResult
        }
    }

    fun forkJoinPoolExample() {
        println("\n=== 1. ForkJoinPool (분할 정복) 예제 ===")

        val numbers = LongArray(1_000_000) { it + 1L }
        val pool = ForkJoinPool()

        val time = measureTimeMillis {
            val result = pool.invoke(SumTask(numbers, 0, numbers.size))
            println("ForkJoinPool 합산 결과: $result")
        }
        println("소요 시간: ${time}ms")
        println("parallelism: ${pool.parallelism}")

        pool.shutdown()
    }

    // ========================================
    // 2. ParallelStream - 병렬 스트림
    // ========================================

    fun parallelStreamExample() {
        println("\n=== 2. ParallelStream 예제 ===")

        val numbers = (1L..1_000_000L).toList()

        // 순차 처리
        val sequentialTime = measureTimeMillis {
            val result = numbers.stream()
                .filter { it % 2 == 0L }
                .mapToLong { it * it }
                .sum()
            println("순차 처리 결과: $result")
        }
        println("순차 소요 시간: ${sequentialTime}ms")

        // 병렬 처리
        val parallelTime = measureTimeMillis {
            val result = numbers.parallelStream()
                .filter { it % 2 == 0L }
                .mapToLong { it * it }
                .sum()
            println("병렬 처리 결과: $result")
        }
        println("병렬 소요 시간: ${parallelTime}ms")
    }

    // ========================================
    // 3. CompletableFuture - 비동기 작업 조합
    // ========================================

    private fun simulateApiCall(apiName: String, delayMs: Long): String {
        Thread.sleep(delayMs)
        return "$apiName 응답 (${delayMs}ms)"
    }

    fun completableFutureExample() {
        println("\n=== 3. CompletableFuture (비동기 조합) 예제 ===")

        val time = measureTimeMillis {
            val userFuture = CompletableFuture.supplyAsync {
                simulateApiCall("유저 API", 300)
            }
            val orderFuture = CompletableFuture.supplyAsync {
                simulateApiCall("주문 API", 500)
            }
            val paymentFuture = CompletableFuture.supplyAsync {
                simulateApiCall("결제 API", 200)
            }

            // 3개 API 모두 완료 대기
            CompletableFuture.allOf(userFuture, orderFuture, paymentFuture).join()

            println("  ${userFuture.get()}")
            println("  ${orderFuture.get()}")
            println("  ${paymentFuture.get()}")
        }
        println("총 소요 시간: ${time}ms (순차면 1000ms, 병렬이라 ~500ms)")

        // thenCombine 예제
        println("\n--- thenCombine 예제 ---")
        val result = CompletableFuture.supplyAsync { 10 }
            .thenCombine(CompletableFuture.supplyAsync { 20 }) { a, b -> a + b }
            .thenApply { "합산 결과: $it" }
            .get()
        println("  $result")
    }

    // ========================================
    // 4. ExecutorService - 스레드 풀
    // ========================================

    fun executorServiceExample() {
        println("\n=== 4. ExecutorService (스레드 풀) 예제 ===")

        val executor = Executors.newFixedThreadPool(4)

        val time = measureTimeMillis {
            val futures = (1..8).map { taskId ->
                executor.submit(Callable {
                    val threadName = Thread.currentThread().name
                    Thread.sleep(200)
                    "Task-$taskId 완료 [$threadName]"
                })
            }

            futures.forEach { println("  ${it.get()}") }
        }
        println("소요 시간: ${time}ms (8개 작업, 4스레드 → ~400ms)")

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    // ========================================
    // 5. CountDownLatch - 동기화 대기
    // ========================================

    fun countDownLatchExample() {
        println("\n=== 5. CountDownLatch (동기화) 예제 ===")

        val workerCount = 5
        val latch = CountDownLatch(workerCount)
        val totalResult = AtomicLong(0)

        val time = measureTimeMillis {
            repeat(workerCount) { i ->
                Thread.ofPlatform().start {
                    val partialResult = (i + 1) * 100L
                    Thread.sleep((100..300).random().toLong())
                    totalResult.addAndGet(partialResult)
                    println("  Worker-$i 완료 (부분 결과: $partialResult)")
                    latch.countDown()
                }
            }

            println("메인 스레드: 모든 워커 완료 대기 중...")
            latch.await()
            println("모든 워커 완료! 최종 결과: ${totalResult.get()}")
        }
        println("소요 시간: ${time}ms")
    }
}

fun main() {
    println("╔══════════════════════════════════════╗")
    println("║   Java 병렬 처리 5가지 기법 예제      ║")
    println("╚══════════════════════════════════════╝")

    ParallelProcessingExample.forkJoinPoolExample()
    ParallelProcessingExample.parallelStreamExample()
    ParallelProcessingExample.completableFutureExample()
    ParallelProcessingExample.executorServiceExample()
    ParallelProcessingExample.countDownLatchExample()

    println("\n모든 예제 실행 완료!")
}
