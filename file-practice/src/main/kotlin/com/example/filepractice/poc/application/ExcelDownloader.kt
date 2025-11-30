package com.example.filepractice.poc.application

import com.example.filepractice.poc.domain.excel.SheetType
import com.example.filepractice.poc.domain.excel.WorkbookType
import com.sun.management.OperatingSystemMXBean
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.OutputStream
import java.lang.management.ManagementFactory

@Component
class ExcelDownloader(
    private val excelSheetIntegrator: ExcelSheetIntegrator,
    private val excelDataFetcher: ExcelDataFetcher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    private val threadBean = ManagementFactory.getThreadMXBean()
    /**
     * 비동기 방식 엑셀 다운로드 (개선 버전)
     * - suspend 함수로 변경하여 스레드 블로킹 방지
     * - 압축 비활성화로 CPU 오버헤드 감소
     */
    suspend fun write(
        outputStream: OutputStream,
        downloadableData: DownloadableData,
        workbookType: WorkbookType
    ) {
        val startTime = System.currentTimeMillis()
        val startCpuTime = threadBean.currentThreadCpuTime
        val runtime = Runtime.getRuntime()

        // 초기 메모리 및 CPU 측정
        runtime.gc()
        val startMemory = getUsedMemoryMB(runtime)
        val startCpu = getCpuUsage()
        logger.info("[비동기] 시작 - 메모리: ${startMemory}MB, CPU: ${startCpu}%")

        val workbook = SXSSFWorkbook(STREAMING_SIZE)
            .apply { this.isCompressTempFiles = false }
        val excelDataMap = excelDataFetcher.fetchMapBy(downloadableData, SheetType.getSheetTypesBy(workbookType))

        val afterFetchMemory = getUsedMemoryMB(runtime)
        val afterFetchCpu = getCpuUsage()
        logger.info("[비동기] 데이터 준비 완료 - 메모리: ${afterFetchMemory}MB (+${afterFetchMemory - startMemory}MB), CPU: ${afterFetchCpu}%")

        try {
            outputStream.use {
                excelSheetIntegrator.composeAsync(workbook, excelDataMap, SheetType.getSheetTypesBy(workbookType))
                val afterComposeMemory = getUsedMemoryMB(runtime)
                val afterComposeCpu = getCpuUsage()
                logger.info("[비동기] 시트 작성 완료 - 메모리: ${afterComposeMemory}MB (+${afterComposeMemory - afterFetchMemory}MB), CPU: ${afterComposeCpu}%")

                workbook.write(it)
                val afterWriteMemory = getUsedMemoryMB(runtime)
                val afterWriteCpu = getCpuUsage()
                logger.info("[비동기] 파일 쓰기 완료 - 메모리: ${afterWriteMemory}MB (+${afterWriteMemory - afterComposeMemory}MB), CPU: ${afterWriteCpu}%")
            }
        } finally {
            workbook.close()
            workbook.dispose()

            runtime.gc()
            val endMemory = getUsedMemoryMB(runtime)
            val endCpuTime = threadBean.currentThreadCpuTime
            val duration = System.currentTimeMillis() - startTime
            val totalMemoryUsed = endMemory - startMemory
            val cpuTimeUsed = (endCpuTime - startCpuTime) / 1_000_000 // ms
            val avgCpuUsage = if (duration > 0) (cpuTimeUsed.toDouble() / duration * 100) else 0.0

            logger.info("[비동기] 완료 - 소요시간: ${duration}ms, CPU 시간: ${cpuTimeUsed}ms, 평균 CPU: ${"%.2f".format(avgCpuUsage)}%, 총 메모리 사용: ${totalMemoryUsed}MB")
        }
    }

    /**
     * 동기 방식 엑셀 다운로드 (성능 비교용)
     */
    fun writeSync(
        outputStream: OutputStream,
        downloadableData: DownloadableData,
        workbookType: WorkbookType
    ) {
        val startTime = System.currentTimeMillis()
        val startCpuTime = threadBean.currentThreadCpuTime
        val runtime = Runtime.getRuntime()

        // 초기 메모리 및 CPU 측정
        runtime.gc()
        val startMemory = getUsedMemoryMB(runtime)
        val startCpu = getCpuUsage()
        logger.info("[동기] 시작 - 메모리: ${startMemory}MB, CPU: ${startCpu}%")

        val workbook = SXSSFWorkbook(STREAMING_SIZE)
            .apply { this.isCompressTempFiles = false }
        val excelDataMap = excelDataFetcher.fetchMapBy(downloadableData, SheetType.getSheetTypesBy(workbookType))

        val afterFetchMemory = getUsedMemoryMB(runtime)
        val afterFetchCpu = getCpuUsage()
        logger.info("[동기] 데이터 준비 완료 - 메모리: ${afterFetchMemory}MB (+${afterFetchMemory - startMemory}MB), CPU: ${afterFetchCpu}%")

        try {
            outputStream.use {
                excelSheetIntegrator.composeSync(workbook, excelDataMap, SheetType.getSheetTypesBy(workbookType))
                val afterComposeMemory = getUsedMemoryMB(runtime)
                val afterComposeCpu = getCpuUsage()
                logger.info("[동기] 시트 작성 완료 - 메모리: ${afterComposeMemory}MB (+${afterComposeMemory - afterFetchMemory}MB), CPU: ${afterComposeCpu}%")

                workbook.write(it)
                val afterWriteMemory = getUsedMemoryMB(runtime)
                val afterWriteCpu = getCpuUsage()
                logger.info("[동기] 파일 쓰기 완료 - 메모리: ${afterWriteMemory}MB (+${afterWriteMemory - afterComposeMemory}MB), CPU: ${afterWriteCpu}%")
            }
        } finally {
            workbook.close()
            workbook.dispose()

            runtime.gc()
            val endMemory = getUsedMemoryMB(runtime)
            val endCpuTime = threadBean.currentThreadCpuTime
            val duration = System.currentTimeMillis() - startTime
            val totalMemoryUsed = endMemory - startMemory
            val cpuTimeUsed = (endCpuTime - startCpuTime) / 1_000_000 // ms
            val avgCpuUsage = if (duration > 0) (cpuTimeUsed.toDouble() / duration * 100) else 0.0

            logger.info("[동기] 완료 - 소요시간: ${duration}ms, CPU 시간: ${cpuTimeUsed}ms, 평균 CPU: ${"%.2f".format(avgCpuUsage)}%, 총 메모리 사용: ${totalMemoryUsed}MB")
        }
    }

    private fun getUsedMemoryMB(runtime: Runtime): Long {
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun getCpuUsage(): String {
        val cpuLoad = osBean.processCpuLoad
        return if (cpuLoad >= 0) {
            "%.2f".format(cpuLoad * 100)
        } else {
            "N/A"
        }
    }

    companion object {
        // 스트리밍 윈도우 크기: 메모리에 유지할 행 수
        // 100은 적절한 균형값 (너무 작으면 flush 오버헤드, 너무 크면 메모리 사용 증가)
        private const val STREAMING_SIZE = 100
    }
}
