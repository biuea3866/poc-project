package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.FileOutputStream

class IsVisibleFilterTest {

    @Test
    fun `isVisible로 컬럼 필터링 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        val testData = listOf(
            ProductExcelData(
                productId = 1L,
                productName = "테스트 상품",
                amount = 10000,
                qty = 5,
                content = "테스트 내용",
                images = emptyList()
            )
        )

        // productId, productName만 보이도록 설정
        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = false),  // 숨김
                HeaderNodeContext("qty", repeat = 1, isVisible = false),      // 숨김
                HeaderNodeContext("content", repeat = 1, isVisible = false),  // 숨김
                HeaderNodeContext("images", repeat = 0, isVisible = false)    // 숨김
            )
        )

        // When
        sheet.createSheet(workbook, testData, context)

        // Then
        val createdSheet = workbook.getSheetAt(0)
        val headerRow = createdSheet.getRow(0)
        val dataRow = createdSheet.getRow(1)

        // 헤더 확인 (visible한 컬럼만)
        assertEquals("상품 ID", headerRow.getCell(0)?.stringCellValue)
        assertEquals("상품명", headerRow.getCell(1)?.stringCellValue)
        assertNull(headerRow.getCell(2), "숨긴 컬럼은 헤더가 없어야 합니다")

        // 데이터 확인 (visible한 컬럼만)
        assertEquals(1.0, dataRow.getCell(0)?.numericCellValue)
        assertEquals("테스트 상품", dataRow.getCell(1)?.stringCellValue)
        assertNull(dataRow.getCell(2), "숨긴 컬럼은 데이터가 없어야 합니다")

        // 파일 저장
        FileOutputStream("build/test_visible_filter.xlsx").use { fos ->
            workbook.write(fos)
        }

        println("✅ isVisible 필터링이 정상 동작합니다")
        println("   - 보이는 컬럼: productId, productName")
        println("   - 숨긴 컬럼: amount, qty, content, images")

        workbook.close()
    }

    @Test
    fun `중첩 리스트의 isVisible 필터링 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        val testData = listOf(
            ProductExcelData(
                productId = 1L,
                productName = "상품",
                amount = 1000,
                qty = 1,
                content = "내용",
                images = listOf(
                    ProductImage(
                        imageId = 100L,
                        imageName = "image.jpg",
                        imageSize = 1024,
                        imageComments = emptyList()
                    )
                )
            )
        )

        // 모든 컬럼 보이도록 설정 (기본값 테스트)
        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 1, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, testData, context)

        // Then
        val createdSheet = workbook.getSheetAt(0)
        val headerRow = createdSheet.getRow(0)

        // 헤더 확인 (최소한 기본 필드들은 있어야 함)
        assertEquals("상품 ID", headerRow.getCell(0)?.stringCellValue)
        assertEquals("상품명", headerRow.getCell(1)?.stringCellValue)
        assertEquals("상품 가격", headerRow.getCell(2)?.stringCellValue)

        // 파일 저장
        FileOutputStream("build/test_nested_visible_filter.xlsx").use { fos ->
            workbook.write(fos)
        }

        println("✅ 중첩 리스트 기본 테스트 통과")

        workbook.close()
    }
}
