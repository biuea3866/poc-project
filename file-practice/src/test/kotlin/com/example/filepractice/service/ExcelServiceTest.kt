package com.example.filepractice.service

import com.example.filepractice.domain.Coupon
import com.example.filepractice.domain.Order
import com.example.filepractice.domain.Product
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class ExcelServiceTest {

    private val excelService = ExcelService()

    /**
     * 테스트용 더미 주문 데이터를 생성합니다.
     */
    private fun createDummyOrders(): List<Order> {
        val product1 = Product(1L, "노트북", BigDecimal("1500000"))
        val product2 = Product(2L, "마우스", BigDecimal("50000"))
        val coupon1 = Coupon(1L, "10% 할인 쿠폰", BigDecimal("0.1"))

        return listOf(
            Order(101L, LocalDateTime.now().minusDays(1), product1, coupon1),
            Order(102L, LocalDateTime.now(), product2, null)
        )
    }

    @Test
    fun `모든 열을 선택했을 때 엑셀 파일이 정상적으로 생성되는지 테스트`() {
        // given
        val orders = createDummyOrders()
        val allColumns = setOf("order_id", "order_at", "product_name", "coupon_name", "product_id", "product_price")

        // when
        val workbook = excelService.createOrderExcel(orders, allColumns)

        // then
        assertNotNull(workbook, "워크북은 null이 아니어야 합니다.")
        assertEquals(2, workbook.numberOfSheets, "시트는 2개여야 합니다: 주문 내역, 상품")

        // 주문 내역 시트 헤더 검증
        val orderSheet = workbook.getSheet("주문 내역")
        val orderHeaderRow = orderSheet.getRow(0)
        assertEquals("주문 ID", orderHeaderRow.getCell(0).stringCellValue)
        assertEquals("주문 일시", orderHeaderRow.getCell(1).stringCellValue)
        assertEquals("상품명", orderHeaderRow.getCell(2).stringCellValue)
        assertEquals("적용 쿠폰명", orderHeaderRow.getCell(3).stringCellValue)

        // 상품 시트 헤더 검증
        val productSheet = workbook.getSheet("상품")
        val productHeaderRow = productSheet.getRow(0)
        assertEquals("상품 ID", productHeaderRow.getCell(0).stringCellValue)
        assertEquals("상품명", productHeaderRow.getCell(1).stringCellValue)
        assertEquals("상품 가격", productHeaderRow.getCell(2).stringCellValue)
    }

    @Test
    fun `일부 열만 선택했을 때 엑셀 파일이 정상적으로 생성되는지 테스트`() {
        // given
        val orders = createDummyOrders()
        val selectedColumns = setOf("order_id", "product_name", "product_price")

        // when
        val workbook = excelService.createOrderExcel(orders, selectedColumns)

        // then
        assertNotNull(workbook)

        // 주문 내역 시트 헤더 검증 (선택된 열만 포함해야 함)
        val orderSheet = workbook.getSheet("주문 내역")
        val orderHeaderRow = orderSheet.getRow(0)
        assertEquals("주문 ID", orderHeaderRow.getCell(0).stringCellValue)
        assertEquals("상품명", orderHeaderRow.getCell(1).stringCellValue)
        assertNull(orderHeaderRow.getCell(2), "선택되지 않은 '주문 일시' 열은 없어야 합니다.")


        // 상품 시트 헤더 검증 (선택된 열만 포함해야 함)
        val productSheet = workbook.getSheet("상품")
        val productHeaderRow = productSheet.getRow(0)
        assertEquals("상품명", productHeaderRow.getCell(0).stringCellValue)
        assertEquals("상품 가격", productHeaderRow.getCell(1).stringCellValue)
        assertNull(productHeaderRow.getCell(2), "선택되지 않은 '상품 ID' 열은 없어야 합니다.")
    }
}
