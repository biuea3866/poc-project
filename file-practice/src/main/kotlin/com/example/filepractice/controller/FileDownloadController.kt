package com.example.filepractice.controller

import com.example.filepractice.domain.Coupon
import com.example.filepractice.domain.Order
import com.example.filepractice.domain.Product
import com.example.filepractice.service.ExcelService
import com.example.filepractice.service.ITextPdfService
import com.example.filepractice.service.OrderService
import com.example.filepractice.service.PdfService
import com.example.filepractice.service.FileStorageService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@RestController
class FileDownloadController(
    private val excelService: ExcelService,
    private val pdfService: PdfService,
    private val orderService: OrderService,
    private val fileStorageService: FileStorageService,
    private val iTextPdfService: ITextPdfService
) {

    @GetMapping("/download/excel")
    fun downloadExcel(@RequestParam(required = false) columns: Set<String>?): ResponseEntity<ByteArray> {
        val dummyOrders = orderService.createDummyOrders()
        val columnsToInclude = columns ?: setOf("order_id", "order_at", "product_name", "coupon_name", "product_id", "product_price")

        val workbook = excelService.createOrderExcel(dummyOrders, columnsToInclude) as SXSSFWorkbook
        try {
            val outputStream = java.io.ByteArrayOutputStream()
            workbook.write(outputStream)
            val excelBytes = outputStream.toByteArray()

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            headers.setContentDispositionFormData("attachment", "orders.xlsx")

            return ResponseEntity(excelBytes, headers, HttpStatus.OK)
        } finally {
            workbook.dispose()
        }
    }

    @GetMapping("/generate/excel-async")
    fun generateExcelAsync(@RequestParam(required = false) columns: Set<String>?): ResponseEntity<String> {
        val columnsToInclude = columns ?: setOf("order_id", "order_at", "product_name", "coupon_name", "product_id", "product_price")

        GlobalScope.launch {
            val dummyOrders = orderService.createDummyOrders()
            val workbook = excelService.createOrderExcel(dummyOrders, columnsToInclude) as SXSSFWorkbook
            try {
                val fileName = "orders-${UUID.randomUUID()}.xlsx"
                val outputStream = java.io.ByteArrayOutputStream()
                workbook.write(outputStream)
                fileStorageService.saveFile("build/downloads", fileName, outputStream.toByteArray())
                println("비동기 엑셀 생성 완료: $fileName")
            } finally {
                workbook.dispose()
            }
        }

        return ResponseEntity("엑셀 파일 생성 요청을 받았습니다. 잠시 후 처리됩니다.", HttpStatus.ACCEPTED)
    }

    @GetMapping("/download/pdf")
    fun downloadPdf(): ResponseEntity<ByteArray> {
        val dummyOrders = orderService.createDummyOrders()
        val pdfBytes = pdfService.createOrderPdf(dummyOrders)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "orders.pdf")

        return ResponseEntity(pdfBytes, headers, HttpStatus.OK)
    }

    @GetMapping("/generate/pdf-async")
    fun generatePdfAsync(): ResponseEntity<String> {
        GlobalScope.launch {
            val dummyOrders = orderService.createDummyOrders()
            val pdfBytes = pdfService.createOrderPdf(dummyOrders)

            val fileName = "orders-${UUID.randomUUID()}.pdf"
            fileStorageService.saveFile("build/downloads", fileName, pdfBytes)
            println("비동기 PDF 생성 완료: $fileName")
        }

        return ResponseEntity("PDF 파일 생성 요청을 받았습니다. 잠시 후 처리됩니다.", HttpStatus.ACCEPTED)
    }

    @GetMapping("/download/pdf-itext")
    fun downloadITextPdf(): ResponseEntity<ByteArray> {
        val dummyOrders = orderService.createDummyOrders()
        val pdfBytes = iTextPdfService.createOrderPdf(dummyOrders)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "orders-itext.pdf")

        return ResponseEntity(pdfBytes, headers, HttpStatus.OK)
    }

    @GetMapping("/generate/pdf-itext-async")
    fun generateITextPdfAsync(): ResponseEntity<String> {
        GlobalScope.launch {
            val dummyOrders = orderService.createDummyOrders()
            val pdfBytes = iTextPdfService.createOrderPdf(dummyOrders)

            val fileName = "orders-itext-${UUID.randomUUID()}.pdf"
            fileStorageService.saveFile("build/downloads", fileName, pdfBytes)
            println("비동기 iText PDF 생성 완료: $fileName")
        }

        return ResponseEntity("iText PDF 파일 생성 요청을 받았습니다. 잠시 후 처리됩니다.", HttpStatus.ACCEPTED)
    }
}
