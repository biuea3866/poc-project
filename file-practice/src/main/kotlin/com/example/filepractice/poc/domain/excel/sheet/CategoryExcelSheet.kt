package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CategoryData
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.SheetType
import org.apache.poi.ss.usermodel.Sheet

class CategoryExcelSheet : AbstractExcelSheet<CategoryData>() {
    override val sheetType: SheetType
        get() = SheetType.CATEGORY

    override fun getSheetName(): String = "카테고리 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "카테고리 ID",
                key = "categoryId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "카테고리명",
                key = "name",
                width = 25,
                order = 2,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "상위 카테고리 ID",
                key = "parentCategoryId",
                width = 18,
                order = 3,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "설명",
                key = "description",
                width = 40,
                order = 4,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "표시 순서",
                key = "displayOrder",
                width = 12,
                order = 5,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "활성화 여부",
                key = "isActive",
                width = 12,
                order = 6,
                alignment = Alignment.CENTER
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<CategoryData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { categoryData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "categoryId" -> CellWriter.writeNumericCell(
                        row, colIndex, categoryData.categoryId, style
                    )
                    "name" -> CellWriter.writeStringCell(
                        row, colIndex, getCategoryName(categoryData.categoryId), style
                    )
                    "parentCategoryId" -> {
                        val parentId = if (categoryData.categoryId % 10 == 0) null else categoryData.categoryId / 10
                        CellWriter.writeNumericCell(row, colIndex, parentId, style)
                    }
                    "description" -> CellWriter.writeStringCell(
                        row, colIndex, "${getCategoryName(categoryData.categoryId)} 카테고리 설명", style
                    )
                    "displayOrder" -> CellWriter.writeNumericCell(
                        row, colIndex, categoryData.categoryId % 100, style
                    )
                    "isActive" -> CellWriter.writeStringCell(
                        row, colIndex, if (categoryData.categoryId % 5 != 0) "활성" else "비활성", style
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

    private fun getCategoryName(categoryId: Int): String {
        val categories = listOf("전자제품", "의류", "식품", "도서", "가구", "스포츠", "뷰티", "완구", "생활용품", "자동차")
        return categories[categoryId % categories.size]
    }
}
