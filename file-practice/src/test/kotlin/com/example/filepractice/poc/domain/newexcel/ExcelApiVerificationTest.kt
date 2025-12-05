package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.io.FileOutputStream

class ExcelApiVerificationTest {

    @Test
    fun `엑셀 파일 구조 검증 - Full`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = org.apache.poi.xssf.streaming.SXSSFWorkbook()
        val data = ProductExcelDataGenerator.generate(10)

        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 3, isVisible = true),
                HeaderNodeContext("imageComments", repeat = 10, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, data, context)

        // Save to file
        val file = File("build/api_verification_full.xlsx")
        FileOutputStream(file).use { fos ->
            workbook.write(fos)
        }
        workbook.close()

        // Then - Read and verify
        val readWorkbook = XSSFWorkbook(file)
        val readSheet = readWorkbook.getSheetAt(0)
        val headerRow = readSheet.getRow(0)

        println("\n=== Full Excel (images repeat=3, comments repeat=10) ===")
        println("Total columns: ${headerRow.lastCellNum}")

        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(i)
            val value = cell?.stringCellValue ?: ""
            if (value.isNotEmpty()) {
                headers.add(value)
                if (i < 10 || value.contains("이미지") || value.contains("댓글")) {
                    println("  [$i] $value")
                }
            }
        }

        // 기본 필드 확인
        assertEquals("상품 ID", headers[0])
        assertEquals("상품명", headers[1])
        assertEquals("상품 가격", headers[2])
        assertEquals("상품 갯수", headers[3])
        assertEquals("상품 내역", headers[4])

        // 이미지 관련 헤더 카운트
        val imageHeaders = headers.filter { it.contains("이미지") || it.contains("댓글") }
        println("\nImage/Comment related headers: ${imageHeaders.size}")

        // 예상 컬럼 수: 5(기본) + 3(이미지 반복) * (3(이미지 필드) + 10(댓글 반복) * 2(댓글 필드))
        // = 5 + 3 * (3 + 20) = 5 + 69 = 74
        val expectedColumns = 5 + 3 * (3 + 10 * 2)
        assertEquals(expectedColumns, headers.size, "총 컬럼 수가 일치해야 합니다")

        println("✅ Expected columns: $expectedColumns, Actual: ${headers.size}")

        readWorkbook.close()
    }

    @Test
    fun `엑셀 파일 구조 검증 - Simple`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = org.apache.poi.xssf.streaming.SXSSFWorkbook()
        val data = ProductExcelDataGenerator.generate(10)

        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = false),  // 숨김
                HeaderNodeContext("images", repeat = 0, isVisible = false)    // 숨김
            )
        )

        // When
        sheet.createSheet(workbook, data, context)

        // Save to file
        val file = File("build/api_verification_simple.xlsx")
        FileOutputStream(file).use { fos ->
            workbook.write(fos)
        }
        workbook.close()

        // Then - Read and verify
        val readWorkbook = XSSFWorkbook(file)
        val readSheet = readWorkbook.getSheetAt(0)
        val headerRow = readSheet.getRow(0)

        println("\n=== Simple Excel (no images, no content) ===")
        println("Total columns: ${headerRow.lastCellNum}")

        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(i)
            val value = cell?.stringCellValue ?: ""
            if (value.isNotEmpty()) {
                headers.add(value)
                println("  [$i] $value")
            }
        }

        // 기본 필드만 있어야 함 (content와 images 제외)
        assertEquals(4, headers.size, "기본 필드 4개만 있어야 합니다")
        assertEquals("상품 ID", headers[0])
        assertEquals("상품명", headers[1])
        assertEquals("상품 가격", headers[2])
        assertEquals("상품 갯수", headers[3])

        println("✅ Simple Excel verified: ${headers.size} columns")

        readWorkbook.close()
    }

    @Test
    fun `엑셀 파일 구조 검증 - Custom (images repeat=2)`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = org.apache.poi.xssf.streaming.SXSSFWorkbook()
        val data = ProductExcelDataGenerator.generate(10)

        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 2, isVisible = true),  // 2번만 반복
                HeaderNodeContext("imageComments", repeat = 10, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, data, context)

        // Save to file
        val file = File("build/api_verification_custom.xlsx")
        FileOutputStream(file).use { fos ->
            workbook.write(fos)
        }
        workbook.close()

        // Then - Read and verify
        val readWorkbook = XSSFWorkbook(file)
        val readSheet = readWorkbook.getSheetAt(0)
        val headerRow = readSheet.getRow(0)

        println("\n=== Custom Excel (images repeat=2, with content) ===")
        println("Total columns: ${headerRow.lastCellNum}")

        val headers = mutableListOf<String>()
        for (i in 0 until headerRow.lastCellNum) {
            val cell = headerRow.getCell(i)
            val value = cell?.stringCellValue ?: ""
            if (value.isNotEmpty()) {
                headers.add(value)
            }
        }

        // 예상 컬럼 수: 5(기본) + 2(이미지 반복) * (3(이미지 필드) + 10(댓글 반복) * 2(댓글 필드))
        // = 5 + 2 * (3 + 20) = 5 + 46 = 51
        val expectedColumns = 5 + 2 * (3 + 10 * 2)
        assertEquals(expectedColumns, headers.size, "총 컬럼 수가 일치해야 합니다")

        println("✅ Expected columns: $expectedColumns, Actual: ${headers.size}")

        readWorkbook.close()
    }
}
