package com.example.filepractice.claude.controller

import com.example.filepractice.claude.dto.ColumnConfig
import com.example.filepractice.claude.dto.DownloadRequest
import com.example.filepractice.claude.service.AsyncFileDownloadService
import com.example.filepractice.claude.service.ExcelGenerationService
import com.example.filepractice.claude.service.GetOrderService
import com.example.filepractice.claude.service.PdfGenerationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/**
 * 파일 다운로드 컨트롤러
 *
 * 주문 내역을 엑셀 또는 PDF 파일로 다운로드하는 API를 제공합니다.
 * - 동기 다운로드: 스트리밍 방식으로 파일을 생성하여 브라우저로 다운로드 (OOM 방지)
 * - 비동기 다운로드: 백그라운드에서 파일을 생성하고 이메일로 발송
 *
 * StreamingResponseBody를 사용하여 대용량 파일 다운로드 시 OOM 문제를 방지합니다.
 */
@RestController
@RequestMapping("/api/claude/download")
class ClaudeFileDownloadController(
    private val getOrderService: GetOrderService,
    private val excelGenerationService: ExcelGenerationService,
    private val pdfGenerationService: PdfGenerationService,
    private val asyncFileDownloadService: AsyncFileDownloadService
) {

    /**
     * 동기 방식 - 엑셀 다운로드 (스트리밍)
     *
     * StreamingResponseBody를 사용하여 대용량 파일도 메모리 문제 없이 다운로드
     * 메모리에 전체 파일을 올리지 않고 청크 단위로 스트리밍
     *
     * @param userId 사용자 ID
     * @param includePrice 가격 정보 포함 여부 (기본값: true)
     * @param useLargeDataset 대용량 데이터셋 사용 여부 (기본값: false, 테스트용)
     * @return 엑셀 파일 스트림
     */
    @GetMapping("/excel/sync")
    fun downloadExcelSync(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "true") includePrice: Boolean,
        @RequestParam(defaultValue = "false") useLargeDataset: Boolean
    ): ResponseEntity<StreamingResponseBody> {

        // 컬럼 설정
        val columnConfig = if (includePrice) {
            ColumnConfig.default()
        } else {
            ColumnConfig.withoutPriceInfo()
        }

        // StreamingResponseBody: OutputStream을 직접 제공받아 스트리밍 쓰기
        val streamingBody = StreamingResponseBody { outputStream ->
            if (useLargeDataset) {
                // 대용량 데이터: Sequence로 스트리밍 조회
                val ordersSequence = getOrderService.getOrdersByUserIdAsSequence(userId)
                excelGenerationService.generateOrderExcelFromSequence(
                    ordersSequence,
                    columnConfig,
                    outputStream
                )
            } else {
                // 소량 데이터: List로 한번에 조회
                val orders = getOrderService.getOrdersByUserId(userId)
                excelGenerationService.generateOrderExcel(
                    orders,
                    columnConfig,
                    outputStream
                )
            }
        }

        // HTTP 응답 헤더 설정
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        headers.setContentDispositionFormData("attachment", "order_history_${userId}.xlsx")

        return ResponseEntity(streamingBody, headers, HttpStatus.OK)
    }

    /**
     * 비동기 방식 - 엑셀 다운로드
     *
     * 백그라운드에서 엑셀 파일을 생성하고 이메일로 발송합니다.
     *
     * @param request 다운로드 요청 정보
     * @return 처리 상태 메시지
     */
    @PostMapping("/excel/async")
    fun downloadExcelAsync(@RequestBody request: DownloadRequest): ResponseEntity<Map<String, String>> {
        // 이메일 주소 필수 검증
        val email = request.email
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "이메일 주소가 필요합니다."))

        // 비동기 처리 시작
        asyncFileDownloadService.generateAndSendExcelAsync(
            userId = request.userId,
            email = email,
            columnConfig = request.columnConfig
        )

        val response = mapOf(
            "message" to "엑셀 파일 생성이 시작되었습니다. 완료되면 이메일로 발송됩니다.",
            "email" to email
        )

        return ResponseEntity.accepted().body(response)
    }

    /**
     * 동기 방식 - PDF 다운로드 (스트리밍)
     *
     * StreamingResponseBody를 사용하여 대용량 파일도 메모리 문제 없이 다운로드
     * 메모리에 전체 파일을 올리지 않고 청크 단위로 스트리밍
     *
     * @param userId 사용자 ID
     * @param useLargeDataset 대용량 데이터셋 사용 여부 (기본값: false, 테스트용)
     * @return PDF 파일 스트림
     */
    @GetMapping("/pdf/sync")
    fun downloadPdfSync(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "false") useLargeDataset: Boolean
    ): ResponseEntity<StreamingResponseBody> {

        // StreamingResponseBody: OutputStream을 직접 제공받아 스트리밍 쓰기
        val streamingBody = StreamingResponseBody { outputStream ->
            if (useLargeDataset) {
                // 대용량 데이터: Sequence로 스트리밍 조회
                val ordersSequence = getOrderService.getOrdersByUserIdAsSequence(userId)
                pdfGenerationService.generateOrderPdfFromSequence(
                    ordersSequence,
                    outputStream
                )
            } else {
                // 소량 데이터: List로 한번에 조회
                val orders = getOrderService.getOrdersByUserId(userId)
                pdfGenerationService.generateOrderPdf(
                    orders,
                    outputStream
                )
            }
        }

        // HTTP 응답 헤더 설정
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "order_history_${userId}.pdf")

        return ResponseEntity(streamingBody, headers, HttpStatus.OK)
    }

    /**
     * 비동기 방식 - PDF 다운로드
     *
     * 백그라운드에서 PDF 파일을 생성하고 이메일로 발송합니다.
     *
     * @param request 다운로드 요청 정보
     * @return 처리 상태 메시지
     */
    @PostMapping("/pdf/async")
    fun downloadPdfAsync(@RequestBody request: DownloadRequest): ResponseEntity<Map<String, String>> {
        // 이메일 주소 필수 검증
        val email = request.email
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "이메일 주소가 필요합니다."))

        // 비동기 처리 시작
        asyncFileDownloadService.generateAndSendPdfAsync(
            userId = request.userId,
            email = email
        )

        val response = mapOf(
            "message" to "PDF 파일 생성이 시작되었습니다. 완료되면 이메일로 발송됩니다.",
            "email" to email
        )

        return ResponseEntity.accepted().body(response)
    }

    /**
     * 헬스 체크 엔드포인트
     *
     * API 서버가 정상 동작하는지 확인합니다.
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "OK"))
    }
}
