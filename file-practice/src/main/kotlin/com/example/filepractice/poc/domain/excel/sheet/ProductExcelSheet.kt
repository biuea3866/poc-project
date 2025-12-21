package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.ProductData
import com.example.filepractice.poc.domain.excel.SheetType
import org.apache.poi.ss.usermodel.Sheet

class ProductExcelSheet : AbstractExcelSheet<ProductData>() {
    override val sheetType: SheetType
        get() = SheetType.PRODUCT

    override fun getSheetName(): String = "제품 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "제품 ID",
                key = "productId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "제품명",
                key = "name",
                width = 30,
                order = 2,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "카테고리",
                key = "category",
                width = 20,
                order = 3,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "가격",
                key = "price",
                width = 15,
                order = 4,
                alignment = Alignment.RIGHT
            ),
            ColumnDefinition(
                name = "재고",
                key = "stock",
                width = 15,
                order = 5,
                alignment = Alignment.RIGHT
            ),
            ColumnDefinition(
                name = "제조사",
                key = "manufacturer",
                width = 25,
                order = 6,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "설명",
                key = "description",
                width = 40,
                order = 7,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "등록일",
                key = "registeredAt",
                width = 15,
                order = 8,
                alignment = Alignment.CENTER
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<ProductData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { productData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "productId" -> CellWriter.writeNumericCell(
                        row, colIndex, productData.productId, style
                    )
                    "name" -> CellWriter.writeStringCell(
                        row, colIndex, "제품 #${productData.productId}", style
                    )
                    "category" -> CellWriter.writeStringCell(
                        row, colIndex, "전자제품", style
                    )
                    "price" -> CellWriter.writeNumericCell(
                        row, colIndex, 100000 + productData.productId * 1000, style
                    )
                    "stock" -> CellWriter.writeNumericCell(
                        row, colIndex, 50 + productData.productId % 100, style
                    )
                    "manufacturer" -> CellWriter.writeStringCell(
                        row, colIndex, "제조사 ${productData.productId % 5}", style
                    )
                    "description" -> CellWriter.writeStringCell(
                        row, colIndex, "제품 설명 #${productData.productId}", style
                    )
                    "registeredAt" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-01-01", style
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
}
