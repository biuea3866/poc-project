package com.example.filepractice.poc.presentation

import com.example.filepractice.poc.domain.newexcel.*
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream

@RestController
@RequestMapping("/api/newexcel")
class NewExcelDownloadController {

    /**
     * 상품 엑셀 다운로드 (전체 컬럼)
     */
    @GetMapping("/products/download")
    fun downloadProductsExcel(
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<ByteArray> {
        // 샘플 데이터 생성
        val productData = ProductExcelDataGenerator.generate(size)

        // 엑셀 시트 생성
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        // 모든 컬럼 표시
        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 3, isVisible = true)  // 이미지 3개까지
            )
        )

        sheet.createSheet(workbook, productData, context)

        // 파일 생성
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        val bytes = outputStream.toByteArray()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products_full.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }

    /**
     * 상품 엑셀 다운로드 (필수 컬럼만)
     */
    @GetMapping("/products/download/simple")
    fun downloadProductsExcelSimple(
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<ByteArray> {
        val productData = ProductExcelDataGenerator.generate(size)
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        // 필수 컬럼만 표시
        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = false),  // 숨김
                HeaderNodeContext("images", repeat = 0, isVisible = false)   // 숨김
            )
        )

        sheet.createSheet(workbook, productData, context)

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        val bytes = outputStream.toByteArray()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products_simple.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }

    /**
     * 상품 엑셀 다운로드 (커스텀 필터)
     */
    @GetMapping("/products/download/custom")
    fun downloadProductsExcelCustom(
        @RequestParam(defaultValue = "100") size: Int,
        @RequestParam(defaultValue = "true") showId: Boolean,
        @RequestParam(defaultValue = "true") showName: Boolean,
        @RequestParam(defaultValue = "true") showAmount: Boolean,
        @RequestParam(defaultValue = "true") showQty: Boolean,
        @RequestParam(defaultValue = "false") showContent: Boolean,
        @RequestParam(defaultValue = "1") imageRepeat: Int
    ): ResponseEntity<ByteArray> {
        val productData = ProductExcelDataGenerator.generate(size)
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        // 사용자 정의 필터
        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = showId),
                HeaderNodeContext("productName", repeat = 1, isVisible = showName),
                HeaderNodeContext("amount", repeat = 1, isVisible = showAmount),
                HeaderNodeContext("qty", repeat = 1, isVisible = showQty),
                HeaderNodeContext("content", repeat = 1, isVisible = showContent),
                HeaderNodeContext("images", repeat = imageRepeat, isVisible = imageRepeat > 0)
            )
        )

        sheet.createSheet(workbook, productData, context)

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        val bytes = outputStream.toByteArray()

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products_custom.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
    }
}
