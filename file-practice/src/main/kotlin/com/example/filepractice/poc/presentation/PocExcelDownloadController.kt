package com.example.filepractice.poc.presentation

import com.example.filepractice.poc.application.DownloadableData
import com.example.filepractice.poc.application.ExcelDownloader
import com.example.filepractice.poc.domain.excel.WorkbookType
import kotlinx.coroutines.runBlocking
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/poc/excel")
class PocExcelDownloadController(
    private val excelDownloader: ExcelDownloader
) {

    /**
     * 비동기 방식 엑셀 다운로드 (개선 버전)
     * - runBlocking을 StreamingResponseBody 람다 안에서만 사용
     * - 실제 스트리밍 중에만 블로킹되므로 요청 수신 시점에는 블로킹 없음
     */
    @GetMapping("/download/async")
    fun downloadExcelAsync(
        @RequestParam(defaultValue = "100") dataSize: Int,
        @RequestParam(defaultValue = "FULL") workbookType: WorkbookType
    ): ResponseEntity<StreamingResponseBody> {
        val downloadableData = createDownloadableData(dataSize)

        val streamingResponseBody = StreamingResponseBody { outputStream ->
            // StreamingResponseBody 내부에서만 runBlocking 사용
            // 이 시점에는 이미 HTTP 응답이 시작되었으므로 문제없음
            runBlocking {
                excelDownloader.write(outputStream, downloadableData, workbookType)
            }
        }

        val headers = HttpHeaders()

        headers.contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        headers.setContentDispositionFormData("attachment", "poc_excel_sync.xlsx")

        return ResponseEntity(streamingResponseBody, headers, HttpStatus.OK)
    }

    /**
     * 동기 방식 엑셀 다운로드
     */
    @GetMapping("/download/sync")
    fun downloadExcelSync(
        @RequestParam(defaultValue = "100") dataSize: Int,
        @RequestParam(defaultValue = "FULL") workbookType: WorkbookType
    ): ResponseEntity<StreamingResponseBody> {
        val downloadableData = createDownloadableData(dataSize)

        val streamingResponseBody = StreamingResponseBody { outputStream ->
            excelDownloader.writeSync(outputStream, downloadableData, workbookType)
        }

        val headers = HttpHeaders()

        headers.contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        headers.setContentDispositionFormData("attachment", "poc_excel_sync.xlsx")

        return ResponseEntity(streamingResponseBody, headers, HttpStatus.OK)
    }

    private fun createDownloadableData(dataSize: Int): DownloadableData {
        val productIds = (1..dataSize).toList()
        val orderIds = (1..dataSize * 2).toList()
        val userIds = (1..dataSize).toList()
        val categoryIds = (1..dataSize / 5).toList()
        val reviewIds = (1..dataSize * 3).toList()
        val shipmentIds = (1..dataSize * 2).toList()

        return DownloadableData(
            productIds = productIds,
            orderIds = orderIds,
            userIds = userIds,
            categoryIds = categoryIds,
            reviewIds = reviewIds,
            shipmentIds = shipmentIds
        )
    }

    private fun getContentDisposition(filename: String): String {
        return ContentDisposition.attachment()
            .filename(filename, StandardCharsets.UTF_8)
            .build()
            .toString()
    }
}
