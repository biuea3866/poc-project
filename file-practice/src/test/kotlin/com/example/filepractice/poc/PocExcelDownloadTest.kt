package com.example.filepractice.poc

import com.example.filepractice.poc.application.DownloadableData
import com.example.filepractice.poc.application.ExcelDownloader
import com.example.filepractice.poc.domain.excel.WorkbookType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

@SpringBootTest
class PocExcelDownloadTest {

    @Autowired
    private lateinit var excelDownloader: ExcelDownloader

    @Test
    fun `비동기 방식 엑셀 다운로드 테스트`() = runBlocking {
        val downloadableData = createDownloadableData(1000)
        val outputPath = Paths.get("build", "poc_excel_async_test.xlsx")

        // 메모리 사용량 측정
        val runtime = Runtime.getRuntime()
        runtime.gc() // GC 실행
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        val duration = measureTimeMillis {
            FileOutputStream(outputPath.toFile()).use { outputStream ->
                excelDownloader.write(outputStream, downloadableData, WorkbookType.FULL)
            }
        }

        runtime.gc()
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedMemory = (afterMemory - beforeMemory) / (1024 * 1024) // MB

        println("=== 비동기 방식 엑셀 다운로드 ===")
        println("소요 시간: ${duration}ms")
        println("메모리 사용량: ${usedMemory}MB")
        println("파일 위치: ${outputPath.toAbsolutePath()}")
        println("파일 크기: ${outputPath.toFile().length() / 1024}KB")
    }

    @Test
    fun `동기 방식 엑셀 다운로드 테스트`() {
        val downloadableData = createDownloadableData(1000)
        val outputPath = Paths.get("build", "poc_excel_sync_test.xlsx")

        // 메모리 사용량 측정
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

        val duration = measureTimeMillis {
            FileOutputStream(outputPath.toFile()).use { outputStream ->
                excelDownloader.writeSync(outputStream, downloadableData, WorkbookType.FULL)
            }
        }

        runtime.gc()
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val usedMemory = (afterMemory - beforeMemory) / (1024 * 1024) // MB

        println("=== 동기 방식 엑셀 다운로드 ===")
        println("소요 시간: ${duration}ms")
        println("메모리 사용량: ${usedMemory}MB")
        println("파일 위치: ${outputPath.toAbsolutePath()}")
        println("파일 크기: ${outputPath.toFile().length() / 1024}KB")
    }

    @Test
    fun `SIMPLE 워크북 타입 테스트`() = runBlocking {
        val downloadableData = createDownloadableData(500)
        val outputPath = Paths.get("build", "poc_excel_simple_test.xlsx")

        val duration = measureTimeMillis {
            FileOutputStream(outputPath.toFile()).use { outputStream ->
                excelDownloader.write(outputStream, downloadableData, WorkbookType.SIMPLE)
            }
        }

        println("=== SIMPLE 워크북 다운로드 ===")
        println("소요 시간: ${duration}ms")
        println("파일 위치: ${outputPath.toAbsolutePath()}")
    }

    private fun createDownloadableData(size: Int): DownloadableData {
        return DownloadableData(
            productIds = (1..size).toList(),
            orderIds = (1..size * 2).toList(),
            userIds = (1..size).toList(),
            categoryIds = (1..size / 5).toList(),
            reviewIds = (1..size * 3).toList(),
            shipmentIds = (1..size * 2).toList()
        )
    }
}
