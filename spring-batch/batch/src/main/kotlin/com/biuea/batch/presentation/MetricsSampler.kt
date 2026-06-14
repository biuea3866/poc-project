package com.biuea.batch.presentation

import com.sun.management.OperatingSystemMXBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * 측정 구간 동안 별도 데몬 스레드로 heap·CPU 를 폴링한다.
 * start() 로 녹화를 시작하고, 측정 대상 실행 후 stop() 으로 요약을 얻는다.
 */
@Component
class MetricsSampler(
    @param:Value("\${benchmark.sampling-interval-ms:100}")
    private val intervalMs: Long,
) {
    fun start(): Recording = Recording(intervalMs).also { it.begin() }

    class Recording(
        private val intervalMs: Long,
    ) {
        private val memoryBean = ManagementFactory.getMemoryMXBean()
        private val osBean =
            ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        private val heapSamples = CopyOnWriteArrayList<Long>()
        private val cpuSamples = CopyOnWriteArrayList<Double>()

        @Volatile
        private var running = false
        private lateinit var samplerThread: Thread

        fun begin() {
            running = true
            samplerThread =
                thread(start = true, isDaemon = true, name = "metrics-sampler") {
                    while (running) {
                        heapSamples.add(memoryBean.heapMemoryUsage.used)
                        val cpu = osBean.processCpuLoad
                        if (cpu >= 0) cpuSamples.add(cpu)
                        Thread.sleep(intervalMs)
                    }
                }
        }

        fun stop(): MetricsSummary {
            running = false
            samplerThread.join(intervalMs * 5)
            return MetricsSummary(
                peakHeapBytes = heapSamples.maxOrNull() ?: 0L,
                avgHeapBytes = if (heapSamples.isEmpty()) 0L else heapSamples.average().toLong(),
                avgCpuLoad = if (cpuSamples.isEmpty()) 0.0 else cpuSamples.average(),
                sampleCount = heapSamples.size,
            )
        }
    }
}

data class MetricsSummary(
    val peakHeapBytes: Long,
    val avgHeapBytes: Long,
    val avgCpuLoad: Double,
    val sampleCount: Int,
)
