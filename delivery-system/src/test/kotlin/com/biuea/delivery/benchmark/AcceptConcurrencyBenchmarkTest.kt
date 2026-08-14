package com.biuea.delivery.benchmark

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import com.biuea.delivery.infrastructure.persistence.AcceptStrategyTestConfiguration
import com.biuea.delivery.infrastructure.persistence.AcceptStrategyTestContext
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 수락 동시성 제어 전략 3종의 실측 벤치마크. 실행: ./gradlew benchmarkTest
 *
 * 이 클래스만 Kotest 가 아닌 JUnit 인 이유: benchmarkTest 태스크가 JUnit 플랫폼 태그(includeTags("benchmark"))로
 * 대상을 고르는데, Kotest 엔진은 JUnit 태그를 노출하지 않아 태그로 걸러지지 않는다.
 * (실측 확인: @Tag 를 붙인 Kotest 스펙은 test 에서 제외되지 않고 benchmarkTest 에서는 아예 수집되지 않았다.)
 * 동작 검증 테스트는 전부 Kotest 로 두고, 측정 진입점만 JUnit 으로 맞춘다.
 */
@Tag("benchmark")
class AcceptConcurrencyBenchmarkTest {

    @Test
    fun `경합 수준별로 전략 3종의 수락 지연과 처리량을 측정한다`() {
        warmUp()

        val measurements = AcceptStrategyTestContext.strategies().flatMap { (strategyName, strategy) ->
            CONCURRENT_RIDER_COUNTS.map { concurrentRiderCount ->
                measure(strategyName, concurrentRiderCount, strategy)
            }
        }

        printMeasurementTable(measurements)

        // 측정값이 나왔어도 단일 승자가 깨졌다면 그 수치는 의미가 없다.
        measurements.forEach { it.acceptedCount shouldBe 1 }
    }

    /** JIT·커넥션 풀·Hibernate 첫 쿼리 준비 비용이 1회차 측정에 섞이지 않게 한다. */
    private fun warmUp() {
        AcceptStrategyTestContext.strategies().forEach { (_, strategy) ->
            AcceptStrategyTestContext.clearOrders()
            val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder()
            AcceptStrategyTestContext.runConcurrently(WARM_UP_RIDER_COUNT) { riderIndex ->
                strategy.accept(orderId, riderIndex.toLong() + 1)
            }
        }
    }

    private fun measure(
        strategyName: String,
        concurrentRiderCount: Int,
        strategy: DeliveryAcceptStrategy,
    ): AcceptConcurrencyMeasurement {
        AcceptStrategyTestContext.clearOrders()
        val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder()
        val optimisticLockAcceptStrategy = AcceptStrategyTestContext.optimisticLockAcceptStrategy
        optimisticLockAcceptStrategy.resetTotalRetryCount()
        val connectionHoldTimeRecorder = AcceptStrategyTestContext.connectionHoldTimeRecorder
        connectionHoldTimeRecorder.reset()

        val samples = AcceptStrategyTestContext.runConcurrently(concurrentRiderCount) { riderIndex ->
            val startedAtNanos = System.nanoTime()
            val outcome = strategy.accept(orderId, riderIndex.toLong() + 1)
            AcceptSample(startedAtNanos, System.nanoTime(), outcome)
        }

        // 스레드 풀 생성 비용을 빼고, 첫 수락 시작 ~ 마지막 수락 종료 구간만 처리 시간으로 본다.
        val totalElapsedMillis =
            (samples.maxOf { it.finishedAtNanos } - samples.minOf { it.startedAtNanos }) / NANOS_PER_MILLI
        val latencyMillis = samples.map { (it.finishedAtNanos - it.startedAtNanos) / NANOS_PER_MILLI }.sorted()

        return AcceptConcurrencyMeasurement(
            strategyName = strategyName,
            concurrentRiderCount = concurrentRiderCount,
            totalElapsedMillis = totalElapsedMillis,
            latencyP50Millis = percentileOf(latencyMillis, 50),
            latencyP95Millis = percentileOf(latencyMillis, 95),
            latencyP99Millis = percentileOf(latencyMillis, 99),
            acceptedCount = samples.count { it.outcome is AcceptOutcome.Accepted },
            retryCount = optimisticLockAcceptStrategy.totalRetryCount.takeIf { strategy === optimisticLockAcceptStrategy },
            connectionAcquireWaitP95Millis = percentileOf(connectionHoldTimeRecorder.acquireWaitMillis().sorted(), 95),
            connectionHoldP95Millis = percentileOf(connectionHoldTimeRecorder.holdMillis().sorted(), 95),
            throughputPerSecond = concurrentRiderCount * MILLIS_PER_SECOND / totalElapsedMillis,
        )
    }

    private fun percentileOf(sortedValues: List<Double>, percentile: Int): Double {
        if (sortedValues.isEmpty()) return 0.0
        val rank = Math.ceil(sortedValues.size * percentile / 100.0).toInt()
        return sortedValues[(rank - 1).coerceIn(sortedValues.indices)]
    }

    private fun printMeasurementTable(measurements: List<AcceptConcurrencyMeasurement>) {
        val header = listOf(
            "전략" to 22,
            "동시 라이더" to 12,
            "총 처리(ms)" to 12,
            "P50(ms)" to 9,
            "P95(ms)" to 9,
            "P99(ms)" to 9,
            "성공" to 6,
            "재시도" to 8,
            "커넥션 대기 P95(ms)" to 20,
            "커넥션 점유 P95(ms)" to 20,
            "처리량(건/s)" to 13,
        )
        println()
        println("## 수락 동시성 제어 전략 실측 (MySQL 8.0 + Redis 7, 커넥션 풀 ${AcceptStrategyTestConfiguration.CONNECTION_POOL_SIZE})")
        println()
        println(header.joinToString(" | ", "| ", " |") { (title, width) -> pad(title, width) })
        println(header.joinToString(" | ", "| ", " |") { (_, width) -> "-".repeat(width) })
        measurements.forEach { measurement ->
            val columns = listOf(
                measurement.strategyName,
                measurement.concurrentRiderCount.toString(),
                "%.1f".format(measurement.totalElapsedMillis),
                "%.2f".format(measurement.latencyP50Millis),
                "%.2f".format(measurement.latencyP95Millis),
                "%.2f".format(measurement.latencyP99Millis),
                "${measurement.acceptedCount}건",
                measurement.retryCount?.toString() ?: "-",
                "%.2f".format(measurement.connectionAcquireWaitP95Millis),
                "%.1f".format(measurement.connectionHoldP95Millis),
                "%.1f".format(measurement.throughputPerSecond),
            )
            println(columns.zip(header).joinToString(" | ", "| ", " |") { (value, spec) -> pad(value, spec.second) })
        }
        println()
    }

    /** 한글은 터미널에서 두 칸을 차지한다. 표를 눈으로 읽으려면 폭을 보정해야 한다. */
    private fun pad(text: String, width: Int): String {
        val displayWidth = text.fold(0) { accumulatedWidth, character ->
            accumulatedWidth + if (character.code in CJK_CODE_RANGE) 2 else 1
        }
        return text + " ".repeat((width - displayWidth).coerceAtLeast(0))
    }

    private data class AcceptSample(
        val startedAtNanos: Long,
        val finishedAtNanos: Long,
        val outcome: AcceptOutcome,
    )

    private data class AcceptConcurrencyMeasurement(
        val strategyName: String,
        val concurrentRiderCount: Int,
        val totalElapsedMillis: Double,
        val latencyP50Millis: Double,
        val latencyP95Millis: Double,
        val latencyP99Millis: Double,
        val acceptedCount: Int,
        val retryCount: Long?,
        val connectionAcquireWaitP95Millis: Double,
        val connectionHoldP95Millis: Double,
        val throughputPerSecond: Double,
    )

    companion object {
        private val CONCURRENT_RIDER_COUNTS = listOf(10, 50, 100, 200)
        private const val WARM_UP_RIDER_COUNT = 10
        private const val NANOS_PER_MILLI = 1_000_000.0
        private const val MILLIS_PER_SECOND = 1_000.0
        private val CJK_CODE_RANGE = 0x1100..0xFFDC
    }
}
