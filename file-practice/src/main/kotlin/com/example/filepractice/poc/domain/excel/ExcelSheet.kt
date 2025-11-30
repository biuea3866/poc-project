package com.example.filepractice.poc.domain.excel

import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook

interface ExcelSheet<T : ExcelData> {
    val sheetType: SheetType

    fun getSheetName(): String

    fun getColumnDefinitions(): List<ColumnDefinition>

    fun createSheet(workbook: SXSSFWorkbook): Sheet

    fun writeData(sheet: Sheet, data: List<T>, currentRow: Int = 1): Int

    fun applyStyle(sheet: Sheet)
}

abstract class AbstractExcelSheet<T : ExcelData> : ExcelSheet<T> {
    // 스타일 캐시: 시트당 한 번만 생성하여 재사용
    private var cachedStyleMap: Map<String, org.apache.poi.ss.usermodel.CellStyle>? = null
    private var cachedVisibleColumns: List<ColumnDefinition>? = null

    override fun createSheet(workbook: SXSSFWorkbook): Sheet {
        val sheet = workbook.createSheet(getSheetName())

        // 스타일 캐시 초기화 (시트 생성 시 한 번만)
        cachedVisibleColumns = getColumnDefinitions().filter { it.isVisible }
        cachedStyleMap = cachedVisibleColumns!!.associate { column ->
            column.key to createDataCellStyle(workbook, column.alignment)
        }

        createHeader(sheet, workbook)
        applyStyle(sheet)
        return sheet
    }

    protected open fun createHeader(sheet: Sheet, workbook: SXSSFWorkbook) {
        val headerRow = sheet.createRow(0)
        val headerStyle = createHeaderStyle(workbook)

        cachedVisibleColumns!!.forEachIndexed { index, columnDef ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(columnDef.name)
            cell.cellStyle = headerStyle

            sheet.setColumnWidth(index, columnDef.width * POI_COLUMN_WIDTH_FACTOR)
        }
    }

    protected open fun createHeaderStyle(workbook: SXSSFWorkbook) =
        workbook.createCellStyle().apply {
            val font = workbook.createFont()
            font.bold = true
            setFont(font)

            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

    protected open fun createDataCellStyle(
        workbook: SXSSFWorkbook,
        alignment: Alignment
    ) = workbook.createCellStyle().apply {
        this.alignment = when (alignment) {
            Alignment.LEFT -> HorizontalAlignment.LEFT
            Alignment.CENTER -> HorizontalAlignment.CENTER
            Alignment.RIGHT -> HorizontalAlignment.RIGHT
        }
        verticalAlignment = VerticalAlignment.CENTER
    }

    protected fun flushRows(
        sheet: Sheet,
        currentRow: Int
    ) {
        if (sheet is SXSSFSheet && currentRow % 100 == 0) {
            sheet.flushRows()
        }
    }

    /**
     * 캐시된 스타일과 컬럼 정의를 반환
     * writeData 구현체에서 사용
     */
    protected fun getCachedStyleMap() = cachedStyleMap!!
    protected fun getCachedVisibleColumns() = cachedVisibleColumns!!

    companion object {
        private const val POI_COLUMN_WIDTH_FACTOR = 256
    }
}

enum class SheetType {
    PRODUCT,
    ORDER,
    USER,
    CATEGORY,
    REVIEW,
    SHIPMENT
    ;

    companion object {
        fun getSheetTypesBy(workbookType: WorkbookType) = when (workbookType) {
            WorkbookType.FULL -> FULL_WORKBOOK_SHEET_TYPE
            WorkbookType.SIMPLE -> SIMPLE_WORKBOOK_SHEET_TYPE
        }

        private val FULL_WORKBOOK_SHEET_TYPE = listOf(
            PRODUCT,
            ORDER,
            USER,
            CATEGORY,
            REVIEW,
            SHIPMENT
        )

        private val SIMPLE_WORKBOOK_SHEET_TYPE = listOf(
            ORDER
        )
    }
}

enum class WorkbookType {
    FULL,
    SIMPLE
}
