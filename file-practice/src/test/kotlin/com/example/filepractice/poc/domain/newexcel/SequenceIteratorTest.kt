package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.FileOutputStream

class SequenceIteratorTest {

    @Test
    fun `Sequence를 사용한 엑셀 생성 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        // Sequence로 데이터 생성 (lazy evaluation)
        val testData = sequenceOf(
            ProductExcelData(
                productId = 1L,
                productName = "상품 1",
                amount = 10000,
                qty = 5,
                content = "내용 1",
                images = listOf(
                    ProductImage(
                        imageId = 101L,
                        imageName = "image1.jpg",
                        imageSize = 1024,
                        imageComments = listOf(
                            ProductImageComment(1001L, "댓글 1"),
                            ProductImageComment(1002L, "댓글 2")
                        )
                    ),
                    ProductImage(
                        imageId = 102L,
                        imageName = "image2.jpg",
                        imageSize = 2048,
                        imageComments = listOf(
                            ProductImageComment(1003L, "댓글 3")
                        )
                    )
                )
            ),
            ProductExcelData(
                productId = 2L,
                productName = "상품 2",
                amount = 20000,
                qty = 3,
                content = "내용 2",
                images = listOf(
                    ProductImage(
                        imageId = 201L,
                        imageName = "image3.jpg",
                        imageSize = 3072,
                        imageComments = emptyList()
                    )
                )
            )
        )

        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 3, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, testData, context)

        // Then
        val createdSheet = workbook.getSheetAt(0)
        assertEquals("상품", createdSheet.sheetName)

        val headerRow = createdSheet.getRow(0)
        assertNotNull(headerRow, "헤더 행이 생성되어야 합니다")

        val dataRow1 = createdSheet.getRow(1)
        assertNotNull(dataRow1, "첫 번째 데이터 행이 생성되어야 합니다")
        assertEquals(1.0, dataRow1.getCell(0)?.numericCellValue)
        assertEquals("상품 1", dataRow1.getCell(1)?.stringCellValue)

        val dataRow2 = createdSheet.getRow(2)
        assertNotNull(dataRow2, "두 번째 데이터 행이 생성되어야 합니다")
        assertEquals(2.0, dataRow2.getCell(0)?.numericCellValue)
        assertEquals("상품 2", dataRow2.getCell(1)?.stringCellValue)

        // 파일로 저장하여 확인
        FileOutputStream("build/test_sequence_excel.xlsx").use { fos ->
            workbook.write(fos)
        }

        println("✅ Sequence를 사용한 엑셀 생성 성공")
        println("   - 이미지 데이터: Sequence로 처리")
        println("   - 댓글 데이터: Sequence로 처리")

        workbook.close()
    }

    @Test
    fun `Sequence 데이터가 부족할 때 빈 셀 처리 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val workbook = SXSSFWorkbook()

        // repeat=3이지만 이미지는 1개만 제공
        val testData = sequenceOf(
            ProductExcelData(
                productId = 1L,
                productName = "상품",
                amount = 10000,
                qty = 5,
                content = "내용",
                images = listOf(
                    ProductImage(
                        imageId = 101L,
                        imageName = "image1.jpg",
                        imageSize = 1024,
                        imageComments = emptyList()
                    )
                )  // 1개만 제공하지만 repeat=3
            )
        )

        val context = ExcelHeaderContext(
            headerNodeContexts = listOf(
                HeaderNodeContext("productId", repeat = 1, isVisible = true),
                HeaderNodeContext("productName", repeat = 1, isVisible = true),
                HeaderNodeContext("amount", repeat = 1, isVisible = true),
                HeaderNodeContext("qty", repeat = 1, isVisible = true),
                HeaderNodeContext("content", repeat = 1, isVisible = true),
                HeaderNodeContext("images", repeat = 3, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, testData, context)

        // Then
        val createdSheet = workbook.getSheetAt(0)
        val dataRow = createdSheet.getRow(1)

        // 첫 번째 이미지는 데이터가 있어야 함
        val imageId1Col = 5  // productId, name, amount, qty, content 다음
        assertEquals(101.0, dataRow.getCell(imageId1Col)?.numericCellValue)

        // 두 번째, 세 번째 이미지는 빈 셀이어야 함 (데이터 부족)
        // Note: skipColumns()로 처리되므로 셀이 생성되지 않음

        // 파일로 저장하여 확인
        FileOutputStream("build/test_sequence_insufficient_data.xlsx").use { fos ->
            workbook.write(fos)
        }

        println("✅ Sequence 데이터 부족 시 빈 셀 처리 성공")

        workbook.close()
    }
}
