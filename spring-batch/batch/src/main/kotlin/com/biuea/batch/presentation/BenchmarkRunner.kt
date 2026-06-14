package com.biuea.batch.presentation

import com.biuea.batch.application.ResetBatchStateUseCase
import com.biuea.batch.domain.ReservedNotificationRepository
import com.biuea.batch.infrastructure.batch.common.BatchConstants
import com.biuea.batch.presentation.benchmark.BenchmarkReporter
import com.biuea.batch.presentation.benchmark.RunMetrics
import com.biuea.batch.presentation.benchmark.StrategyResult
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * benchmark.mode=benchmark 일 때 6개 전략을 각 N회 실행하고 메모리·CPU·처리속도를 측정해 리포트를 산출한다.
 * 측정 구간(JobOperator 실행) 밖에서 reset·GC 를 수행해 매 회차 동일 작업량을 보장한다.
 */
@Component
class BenchmarkRunner(
    private val jobs: Map<String, Job>,
    private val jobOperator: JobOperator,
    private val resetBatchStateUseCase: ResetBatchStateUseCase,
    private val repository: ReservedNotificationRepository,
    private val metricsSampler: MetricsSampler,
    private val reporter: BenchmarkReporter,
    @param:Value("\${benchmark.mode:none}") private val mode: String,
    @param:Value("\${benchmark.repeat-count:5}") private val repeatCount: Int,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(BenchmarkRunner::class.java)

    private val strategies =
        linkedMapOf(
            BatchConstants.JOB_SINGLE_CHUNK to "1. 단일스레드 Chunk",
            BatchConstants.JOB_SINGLE_TASKLET to "2. 단일스레드 Tasklet",
            BatchConstants.JOB_ASYNC to "3. Async Processor/Writer",
            BatchConstants.JOB_MULTI_THREAD to "4. Multi-thread Step",
            BatchConstants.JOB_PARALLEL to "5. Parallel Step (Split)",
            BatchConstants.JOB_PARTITION to "6. Partitioning",
        )

    override fun run(vararg args: String) {
        if (mode != "benchmark") return

        val totalRaw = repository.countTotal()
        require(totalRaw > 0) { "데이터가 비어 있습니다. 먼저 --benchmark.mode=seed 로 시딩하세요." }
        log.info("벤치마크 시작 — raw {}건, 전략 {}개 × {}회", totalRaw, strategies.size, repeatCount)

        val results =
            strategies.map { (jobName, displayName) ->
                val runs = (1..repeatCount).map { index -> runOnce(jobName, displayName, index) }
                StrategyResult(jobName, displayName, runs)
            }

        val totalGroups = results.firstOrNull()?.runs?.firstOrNull()?.itemsWritten ?: 0L
        reporter.report(results, totalRaw, totalGroups)
        log.info("벤치마크 완료")
    }

    private fun runOnce(jobName: String, displayName: String, index: Int): RunMetrics {
        resetBatchStateUseCase.execute()
        System.gc()
        Thread.sleep(1000) // GC·캐시 안정화 (측정 구간 밖)

        val recording = metricsSampler.start()
        val startNanos = System.nanoTime()
        val execution =
            jobOperator.start(
                jobs.getValue(jobName),
                JobParametersBuilder().addString("run.id", UUID.randomUUID().toString()).toJobParameters(),
            )
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000
        val summary = recording.stop()

        check(execution.status == BatchStatus.COMPLETED) {
            "$jobName run#$index 실패: status=${execution.status}"
        }
        // 파티션 매니저 step 은 워커들의 writeCount 를 합산해 들고 있으므로 함께 더하면 2배가 된다 → 제외.
        val itemsWritten =
            execution.stepExecutions
                .filterNot { it.stepName == BatchConstants.PARTITION_MANAGER_STEP }
                .sumOf { it.writeCount }
        log.info(
            "[{}] run#{} {}ms, {}묶음, CPU {}%, peakHeap {}MB",
            displayName, index, durationMs, itemsWritten,
            "%.1f".format(summary.avgCpuLoad * 100),
            "%.1f".format(summary.peakHeapBytes / 1024.0 / 1024.0),
        )
        return RunMetrics(
            runIndex = index,
            durationMs = durationMs,
            itemsWritten = itemsWritten,
            avgCpuLoad = summary.avgCpuLoad,
            peakHeapBytes = summary.peakHeapBytes,
            avgHeapBytes = summary.avgHeapBytes,
        )
    }
}
