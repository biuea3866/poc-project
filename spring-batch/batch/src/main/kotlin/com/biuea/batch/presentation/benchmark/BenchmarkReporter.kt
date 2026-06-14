package com.biuea.batch.presentation.benchmark

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 벤치마크 결과를 markdown + CSV 로 산출한다.
 */
@Component
class BenchmarkReporter(
    @param:Value("\${benchmark.report-dir:docs}")
    private val reportDir: String,
) {
    fun report(results: List<StrategyResult>, totalRawRows: Long, totalGroups: Long) {
        val dir = Path.of(reportDir)
        Files.createDirectories(dir)
        val markdown = buildMarkdown(results, totalRawRows, totalGroups)
        val csv = buildCsv(results)
        Files.writeString(dir.resolve("benchmark-result.md"), markdown)
        Files.writeString(dir.resolve("benchmark-result.csv"), csv)
        println(markdown)
        println("\n결과 파일: ${dir.resolve("benchmark-result.md")}, ${dir.resolve("benchmark-result.csv")}")
    }

    private fun buildMarkdown(results: List<StrategyResult>, totalRawRows: Long, totalGroups: Long): String {
        val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val cores = Runtime.getRuntime().availableProcessors()
        val repeat = results.firstOrNull()?.runs?.size ?: 0

        val sb = StringBuilder()
        sb.appendLine("# Spring Batch 6전략 성능 비교 결과")
        sb.appendLine()
        sb.appendLine("- 측정 시각: $now")
        sb.appendLine("- 데이터: raw 예약 알림 ${"%,d".format(totalRawRows)}건 → 발송 단위(묶음) ${"%,d".format(totalGroups)}건")
        sb.appendLine("- 반복: 전략당 ${repeat}회 평균")
        sb.appendLine("- CPU 코어: $cores")
        sb.appendLine()
        sb.appendLine("## 평균 비교")
        sb.appendLine()
        sb.appendLine("| 전략 | 평균 소요(ms) | 처리량(묶음/s) | 평균 CPU(%) | peak heap(MB) | 평균 heap(MB) |")
        sb.appendLine("|---|---|---|---|---|---|")
        results.forEach { r ->
            sb.appendLine(
                "| ${r.displayName} | ${"%,.0f".format(r.avgDurationMs)} | ${"%,.0f".format(r.avgThroughputPerSec)} " +
                    "| ${"%.1f".format(r.avgCpuPercent)} | ${"%.1f".format(r.avgPeakHeapMb)} | ${"%.1f".format(r.avgHeapMb)} |",
            )
        }
        sb.appendLine()
        sb.appendLine("## 회차별 상세")
        sb.appendLine()
        results.forEach { r ->
            sb.appendLine("### ${r.displayName} (`${r.jobName}`)")
            sb.appendLine()
            sb.appendLine("| 회차 | 소요(ms) | 묶음 수 | 처리량(묶음/s) | CPU(%) | peak heap(MB) | 평균 heap(MB) |")
            sb.appendLine("|---|---|---|---|---|---|---|")
            r.runs.forEach { run ->
                sb.appendLine(
                    "| ${run.runIndex} | ${"%,d".format(run.durationMs)} | ${"%,d".format(run.itemsWritten)} " +
                        "| ${"%,.0f".format(run.throughputPerSec)} | ${"%.1f".format(run.avgCpuLoad * 100)} " +
                        "| ${"%.1f".format(run.peakHeapBytes.toDouble() / MB)} | ${"%.1f".format(run.avgHeapBytes.toDouble() / MB)} |",
                )
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun buildCsv(results: List<StrategyResult>): String {
        val sb = StringBuilder()
        sb.appendLine("strategy,job_name,run_index,duration_ms,items_written,throughput_per_sec,avg_cpu_percent,peak_heap_mb,avg_heap_mb")
        results.forEach { r ->
            r.runs.forEach { run ->
                sb.appendLine(
                    listOf(
                        r.displayName,
                        r.jobName,
                        run.runIndex,
                        run.durationMs,
                        run.itemsWritten,
                        "%.2f".format(run.throughputPerSec),
                        "%.2f".format(run.avgCpuLoad * 100),
                        "%.2f".format(run.peakHeapBytes.toDouble() / MB),
                        "%.2f".format(run.avgHeapBytes.toDouble() / MB),
                    ).joinToString(","),
                )
            }
        }
        return sb.toString()
    }

    companion object {
        private const val MB = 1024.0 * 1024.0
    }
}
