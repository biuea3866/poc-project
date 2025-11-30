package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.OrderData
import com.example.filepractice.poc.domain.excel.SheetType
import org.apache.poi.ss.usermodel.Sheet

class OrderExcelSheet : AbstractExcelSheet<OrderData>() {
    override val sheetType: SheetType
        get() = SheetType.ORDER

    override fun getSheetName(): String = "주문 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "주문 ID",
                key = "orderId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "사용자 ID",
                key = "userId",
                width = 15,
                order = 2,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "사용자 이름",
                key = "userName",
                width = 20,
                order = 3,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "제품 ID",
                key = "productId",
                width = 15,
                order = 4,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "제품명",
                key = "productName",
                width = 30,
                order = 5,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "수량",
                key = "quantity",
                width = 10,
                order = 6,
                alignment = Alignment.RIGHT
            ),
            ColumnDefinition(
                name = "총 금액",
                key = "totalPrice",
                width = 15,
                order = 7,
                alignment = Alignment.RIGHT
            ),
            ColumnDefinition(
                name = "상태",
                key = "status",
                width = 15,
                order = 8,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "주문일시",
                key = "orderDate",
                width = 20,
                order = 9,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "배송지",
                key = "shippingAddress",
                width = 40,
                order = 10,
                alignment = Alignment.LEFT
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<OrderData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { orderData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "orderId" -> CellWriter.writeNumericCell(
                        row, colIndex, orderData.orderId, style
                    )
                    "userId" -> CellWriter.writeNumericCell(
                        row, colIndex, orderData.orderId % 100, style
                    )
                    "userName" -> CellWriter.writeStringCell(
                        row, colIndex, "사용자 #${orderData.orderId % 100}", style
                    )
                    "productId" -> CellWriter.writeNumericCell(
                        row, colIndex, orderData.orderId % 50, style
                    )
                    "productName" -> CellWriter.writeStringCell(
                        row, colIndex, "제품 #${orderData.orderId % 50}", style
                    )
                    "quantity" -> CellWriter.writeNumericCell(
                        row, colIndex, 1 + orderData.orderId % 5, style
                    )
                    "totalPrice" -> CellWriter.writeNumericCell(
                        row, colIndex, 100000 + orderData.orderId * 1000, style
                    )
                    "status" -> CellWriter.writeStringCell(
                        row, colIndex, getOrderStatus(orderData.orderId), style
                    )
                    "orderDate" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-11-29 10:00:00", style
                    )
                    "shippingAddress" -> CellWriter.writeStringCell(
                        row, colIndex, "서울특별시 강남구 테헤란로 ${orderData.orderId % 500}번길", style
                    )
                }
            }
            this.flushRows(sheet, currentRow)
        }

        return rowIndex
    }

    override fun applyStyle(sheet: Sheet) {
        // 헤더 행 고정
        sheet.createFreezePane(0, 1)

        // 필터 자동 추가
        val lastColumn = getColumnDefinitions().filter { it.isVisible }.size - 1
        if (lastColumn >= 0 && sheet.lastRowNum > 0) {
            sheet.setAutoFilter(
                org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, lastColumn)
            )
        }
    }

    private fun getOrderStatus(orderId: Int): String {
        return when (orderId % 5) {
            0 -> "대기중"
            1 -> "확인됨"
            2 -> "배송중"
            3 -> "배송완료"
            else -> "취소됨"
        }
    }
}
