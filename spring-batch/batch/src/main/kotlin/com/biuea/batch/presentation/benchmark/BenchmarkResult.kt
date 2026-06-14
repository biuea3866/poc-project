package com.biuea.batch.presentation.benchmark

/** 1회 실행의 측정값. */
data class RunMetrics(
    val runIndex: Int,
    val durationMs: Long,
    val itemsWritten: Long,
    val avgCpuLoad: Double,
    val peakHeapBytes: Long,
    val avgHeapBytes: Long,
) {
    val throughputPerSec: Double
        get() = if (durationMs == 0L) 0.0 else itemsWritten.toDouble() / (durationMs / 1000.0)
}

/** 한 전략의 N회 실행 결과와 평균. */
data class StrategyResult(
    val jobName: String,
    val displayName: String,
    val runs: List<RunMetrics>,
) {
    val avgDurationMs: Double get() = runs.map { it.durationMs }.average()
    val avgThroughputPerSec: Double get() = runs.map { it.throughputPerSec }.average()
    val avgCpuPercent: Double get() = runs.map { it.avgCpuLoad * 100 }.average()
    val avgPeakHeapMb: Double get() = runs.map { it.peakHeapBytes.toDouble() / MB }.average()
    val avgHeapMb: Double get() = runs.map { it.avgHeapBytes.toDouble() / MB }.average()

    companion object {
        private const val MB = 1024.0 * 1024.0
    }
}
