package com.biuea.springdata.kafka.service

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Service
@EnableScheduling
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class PerformanceMonitor(
    private val meterRegistry: MeterRegistry
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val normalEventCount = AtomicLong(0)
    private val batchEventCount = AtomicLong(0)
    private val optimizedEventCount = AtomicLong(0)
    private val parallelEventCount = AtomicLong(0)

    private var lastReportTime = Instant.now()
    private var lastNormalCount = 0L
    private var lastBatchCount = 0L
    private var lastOptimizedCount = 0L
    private var lastParallelCount = 0L

    fun recordNormalEvent() {
        normalEventCount.incrementAndGet()
    }

    fun recordBatchEvents(count: Int) {
        batchEventCount.addAndGet(count.toLong())
    }

    fun recordOptimizedEvent() {
        optimizedEventCount.incrementAndGet()
    }

    fun recordOptimizedEvents(count: Int) {
        optimizedEventCount.addAndGet(count.toLong())
    }

    fun recordParallelEvent() {
        parallelEventCount.incrementAndGet()
    }

    fun recordParallelEvents(count: Int) {
        parallelEventCount.addAndGet(count.toLong())
    }

    @Scheduled(fixedRate = 10000)
    fun reportMetrics() {
        val now = Instant.now()
        val elapsed = (now.toEpochMilli() - lastReportTime.toEpochMilli()) / 1000.0

        val currentNormal = normalEventCount.get()
        val currentBatch = batchEventCount.get()
        val currentOptimized = optimizedEventCount.get()
        val currentParallel = parallelEventCount.get()

        val normalTps = (currentNormal - lastNormalCount) / elapsed
        val batchTps = (currentBatch - lastBatchCount) / elapsed
        val optimizedTps = (currentOptimized - lastOptimizedCount) / elapsed
        val parallelTps = (currentParallel - lastParallelCount) / elapsed

        if (normalTps > 0 || batchTps > 0 || optimizedTps > 0 || parallelTps > 0) {
            log.info(
                """
                |
                |=== Performance Report ===
                |Normal Consumer:    {} events, TPS: {:.2f}
                |Batch Consumer:     {} events, TPS: {:.2f}
                |Optimized Consumer: {} events, TPS: {:.2f}
                |Parallel Consumer:  {} events, TPS: {:.2f}
                |==========================
                """.trimMargin(),
                currentNormal, normalTps,
                currentBatch, batchTps,
                currentOptimized, optimizedTps,
                currentParallel, parallelTps
            )
        }

        lastReportTime = now
        lastNormalCount = currentNormal
        lastBatchCount = currentBatch
        lastOptimizedCount = currentOptimized
        lastParallelCount = currentParallel

        // Update Micrometer gauges
        meterRegistry.gauge("kafka.consumer.normal.tps", normalTps)
        meterRegistry.gauge("kafka.consumer.batch.tps", batchTps)
        meterRegistry.gauge("kafka.consumer.optimized.tps", optimizedTps)
        meterRegistry.gauge("kafka.consumer.parallel.tps", parallelTps)
    }

    fun getStats(): PerformanceStats {
        return PerformanceStats(
            normalEventCount = normalEventCount.get(),
            batchEventCount = batchEventCount.get(),
            optimizedEventCount = optimizedEventCount.get(),
            parallelEventCount = parallelEventCount.get()
        )
    }

    fun reset() {
        normalEventCount.set(0)
        batchEventCount.set(0)
        optimizedEventCount.set(0)
        parallelEventCount.set(0)
        lastNormalCount = 0
        lastBatchCount = 0
        lastOptimizedCount = 0
        lastParallelCount = 0
        lastReportTime = Instant.now()
        log.info("Performance counters reset")
    }
}

data class PerformanceStats(
    val normalEventCount: Long,
    val batchEventCount: Long,
    val optimizedEventCount: Long,
    val parallelEventCount: Long
)
