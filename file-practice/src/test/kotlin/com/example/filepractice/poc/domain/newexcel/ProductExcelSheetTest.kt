package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.FileOutputStream

class ProductExcelSheetTest {

    @Test
    fun `템플릿 기반 엑셀 생성 테스트`() {
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
                images = listOf(
                    ProductImage(
                        imageId = 101L,
                        imageName = "image1.jpg",
                        imageSize = 1024,
                        imageComments = listOf(
                            ProductImageComment(1001L, "좋아요"),
                            ProductImageComment(1002L, "멋져요")
                        )
                    ),
                    ProductImage(
                        imageId = 102L,
                        imageName = "image2.jpg",
                        imageSize = 2048,
                        imageComments = listOf(
                            ProductImageComment(1003L, "최고!")
                        )
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
                HeaderNodeContext("productImageList", repeat = 3, isVisible = true)
            )
        )

        // When
        sheet.createSheet(workbook, testData, context)

        // Then
        val createdSheet = workbook.getSheetAt(0)
        assertEquals("상품", createdSheet.sheetName)
        assertTrue(createdSheet.getRow(0) != null, "헤더 행이 생성되어야 합니다")
        assertTrue(createdSheet.getRow(1) != null, "데이터 행이 생성되어야 합니다")

        // 파일로 저장하여 확인
        FileOutputStream("build/test_product_excel.xlsx").use { fos ->
            workbook.write(fos)
        }

        println("✅ 엑셀 파일이 build/test_product_excel.xlsx 에 생성되었습니다")

        workbook.close()
    }

    @Test
    fun `템플릿 구조 확인 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val nodes = sheet.getNodes()

        // When & Then
        assertEquals(6, nodes.size, "최상위 노드는 6개여야 합니다")

        val productIdNode = nodes[0]
        assertEquals("상품 ID", productIdNode.name)
        assertEquals("productId", productIdNode.key)

        val productNameNode = nodes[1]
        assertEquals("상품명", productNameNode.name)
        assertEquals("productName", productNameNode.key)

        println("✅ 템플릿 구조가 올바릅니다")
    }

    @Test
    fun `중첩 리스트 구조 확인 테스트`() {
        // Given
        val sheet = ProductExcelSheet()
        val nodes = sheet.getNodes()
        val imageListNode = nodes[5] // 상품 이미지 리스트

        // When & Then
        assertTrue(imageListNode.hasChildren(), "자식 노드가 있어야 합니다")
        assertEquals("images", imageListNode.key)
        assertEquals(4, imageListNode.children.size, "이미지 노드는 4개의 자식을 가져야 합니다")

        val imageIdNode = imageListNode.children[0]
        assertEquals("imageId", imageIdNode.key)

        val imageNameNode = imageListNode.children[1]
        assertEquals("imageName", imageNameNode.key)

        val commentListNode = imageListNode.children[3]
        assertTrue(commentListNode.hasChildren(), "댓글 리스트는 자식 노드가 있어야 합니다")
        assertEquals("imageComments", commentListNode.key)
        assertEquals(2, commentListNode.children.size, "댓글 노드는 2개의 자식을 가져야 합니다")

        println("✅ 중첩 리스트 구조가 올바릅니다")
    }
}
