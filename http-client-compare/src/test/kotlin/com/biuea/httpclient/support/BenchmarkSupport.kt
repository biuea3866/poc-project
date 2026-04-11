package com.biuea.httpclient.support

import kotlinx.coroutines.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

// ================================================================
// 벤치마크 상수
// ================================================================

/** 총 요청 수 */
const val TOTAL_REQUESTS = 200

/** Mock 서버 지연 시간 (밀리초) */
const val DELAY_MS = 100L

/** Platform Thread 풀 크기 */
const val PLATFORM_THREAD_POOL_SIZE = 50

/** 워밍업 요청 수 */
const val WARMUP_REQUESTS = 5

/** Reactor 동시성 수준 */
const val REACTOR_CONCURRENCY = 50

// ================================================================
// 벤치마크 결과 데이터
// ================================================================
data class BenchmarkResult(
    val name: String,
    val totalRequests: Int,
    val successCount: Int,
    val errorCount: Int,
    val totalTimeMs: Double,
    val avgLatencyMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val throughput: Double  // req/s
)

// ================================================================
// 결과 출력 리포터
// ================================================================
object BenchmarkReporter {

    /**
     * 벤치마크 결과를 테이블 형태로 출력한다.
     */
    fun print(results: List<BenchmarkResult>) {
        if (results.isEmpty()) {
            println("결과 없음")
            return
        }

        val separator = "=".repeat(140)
        val headerFormat = "%-30s %8s %8s %8s %10s %10s %10s %10s %10s %12s"
        val rowFormat = "%-30s %8d %8d %8d %10.1f %10.2f %10.2f %10.2f %10.2f %12.1f"

        println()
        println(separator)
        println(" 벤치마크 결과")
        println(separator)
        println(
            String.format(
                headerFormat,
                "클라이언트", "총요청", "성공", "실패",
                "총시간(ms)", "평균(ms)", "P50(ms)", "P95(ms)", "P99(ms)", "처리량(r/s)"
            )
        )
        println("-".repeat(140))

        results.forEach { r ->
            println(
                String.format(
                    rowFormat,
                    r.name, r.totalRequests, r.successCount, r.errorCount,
                    r.totalTimeMs, r.avgLatencyMs, r.p50Ms, r.p95Ms, r.p99Ms, r.throughput
                )
            )
        }

        println(separator)
        println()
    }
}

// ================================================================
// 퍼센타일 계산 유틸
// ================================================================

/**
 * 정렬된 리스트에서 지정된 퍼센타일 값을 계산한다.
 *
 * @param sortedLatencies 오름차순 정렬된 지연 시간 리스트
 * @param percentile 퍼센타일 (0.0 ~ 1.0)
 * @return 퍼센타일 값 (밀리초)
 */
fun percentile(sortedLatencies: List<Long>, percentile: Double): Double {
    if (sortedLatencies.isEmpty()) return 0.0
    val index = (percentile * (sortedLatencies.size - 1)).toInt()
    return sortedLatencies[index].toDouble()
}

/**
 * 지연 시간 리스트로부터 BenchmarkResult를 생성한다.
 */
fun buildResult(
    name: String,
    totalRequests: Int,
    latencies: List<Long>,
    errorCount: Int,
    totalTimeMs: Double
): BenchmarkResult {
    val sorted = latencies.sorted()
    val successCount = latencies.size
    val avgLatency = if (sorted.isNotEmpty()) sorted.average() else 0.0
    val throughput = if (totalTimeMs > 0) (totalRequests / (totalTimeMs / 1000.0)) else 0.0

    return BenchmarkResult(
        name = name,
        totalRequests = totalRequests,
        successCount = successCount,
        errorCount = errorCount,
        totalTimeMs = totalTimeMs,
        avgLatencyMs = avgLatency,
        p50Ms = percentile(sorted, 0.50),
        p95Ms = percentile(sorted, 0.95),
        p99Ms = percentile(sorted, 0.99),
        throughput = throughput
    )
}

// ================================================================
// 블로킹(스레드 기반) 벤치마크 헬퍼
// ================================================================

/**
 * 스레드 풀 기반 동시성 벤치마크를 수행한다.
 *
 * Platform Thread, Virtual Thread 모두 이 함수를 사용한다.
 *
 * @param name 벤치마크 이름 (ex: "RestTemplate + PlatformThread")
 * @param executor 사용할 ExecutorService
 * @param totalRequests 총 요청 수
 * @param warmupRequests 워밍업 요청 수
 * @param action 실행할 HTTP 호출 (블로킹)
 * @return BenchmarkResult
 */
fun benchmarkBlocking(
    name: String,
    executor: ExecutorService,
    totalRequests: Int = TOTAL_REQUESTS,
    warmupRequests: Int = WARMUP_REQUESTS,
    action: () -> String?
): BenchmarkResult {
    // 워밍업
    repeat(warmupRequests) {
        try {
            action()
        } catch (_: Exception) {
        }
    }

    val latencies = CopyOnWriteArrayList<Long>()
    val errorCount = AtomicInteger(0)
    val latch = CountDownLatch(totalRequests)

    val startTime = System.nanoTime()

    repeat(totalRequests) {
        executor.submit {
            try {
                val reqStart = System.nanoTime()
                action()
                val reqEnd = System.nanoTime()
                latencies.add((reqEnd - reqStart) / 1_000_000)
            } catch (e: Exception) {
                errorCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }
    }

    latch.await()
    val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000.0

    return buildResult(name, totalRequests, latencies.toList(), errorCount.get(), totalTimeMs)
}

// ================================================================
// Reactor(리액티브) 벤치마크 헬퍼
// ================================================================

/**
 * Reactor Flux 기반 동시성 벤치마크를 수행한다.
 *
 * @param name 벤치마크 이름 (ex: "WebClient + Reactor")
 * @param totalRequests 총 요청 수
 * @param concurrency 동시 실행 수 (flatMap concurrency)
 * @param warmupRequests 워밍업 요청 수
 * @param monoSupplier Mono를 반환하는 HTTP 호출 공급자
 * @return BenchmarkResult
 */
fun benchmarkReactive(
    name: String,
    totalRequests: Int = TOTAL_REQUESTS,
    concurrency: Int = REACTOR_CONCURRENCY,
    warmupRequests: Int = WARMUP_REQUESTS,
    monoSupplier: () -> Mono<String>
): BenchmarkResult {
    // 워밍업
    repeat(warmupRequests) {
        try {
            monoSupplier().block()
        } catch (_: Exception) {
        }
    }

    val latencies = CopyOnWriteArrayList<Long>()
    val errorCount = AtomicInteger(0)

    val startTime = System.nanoTime()

    Flux.range(0, totalRequests)
        .flatMap({ _ ->
            val reqStart = System.nanoTime()
            monoSupplier()
                .doOnSuccess {
                    val reqEnd = System.nanoTime()
                    latencies.add((reqEnd - reqStart) / 1_000_000)
                }
                .doOnError {
                    errorCount.incrementAndGet()
                }
                .onErrorResume { Mono.empty() }
        }, concurrency)
        .collectList()
        .block()

    val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000.0

    return buildResult(name, totalRequests, latencies.toList(), errorCount.get(), totalTimeMs)
}

// ================================================================
// Kotlin 코루틴 벤치마크 헬퍼
// ================================================================

/**
 * Kotlin 코루틴 기반 동시성 벤치마크를 수행한다.
 *
 * @param name 벤치마크 이름 (ex: "Ktor + Coroutine")
 * @param totalRequests 총 요청 수
 * @param dispatcher 사용할 Dispatcher
 * @param warmupRequests 워밍업 요청 수
 * @param suspendAction suspend 함수로 실행할 HTTP 호출
 * @return BenchmarkResult
 */
fun benchmarkCoroutine(
    name: String,
    totalRequests: Int = TOTAL_REQUESTS,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    warmupRequests: Int = WARMUP_REQUESTS,
    suspendAction: suspend () -> String
): BenchmarkResult {
    return runBlocking {
        // 워밍업
        repeat(warmupRequests) {
            try {
                withContext(dispatcher) { suspendAction() }
            } catch (_: Exception) {
            }
        }

        val latencies = CopyOnWriteArrayList<Long>()
        val errorCount = AtomicInteger(0)

        val startTime = System.nanoTime()

        // 모든 요청을 코루틴으로 동시 실행
        val jobs = (1..totalRequests).map {
            async(dispatcher) {
                try {
                    val reqStart = System.nanoTime()
                    suspendAction()
                    val reqEnd = System.nanoTime()
                    latencies.add((reqEnd - reqStart) / 1_000_000)
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        val totalTimeMs = (System.nanoTime() - startTime) / 1_000_000.0

        buildResult(name, totalRequests, latencies.toList(), errorCount.get(), totalTimeMs)
    }
}
