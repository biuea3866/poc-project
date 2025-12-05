package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/**
 * Composite 패턴을 활용한 추상 엑셀 시트
 * 복잡한 로직은 모두 HeaderNode에 위임하여 단순화
 */
abstract class AbstractExcelSheet<T : ExcelData> {
    abstract val sheetType: SheetType

    protected abstract fun getSheetName(): String

    /**
     * 템플릿 노드 목록을 반환합니다.
     */
    abstract fun getNodes(): List<HeaderNode>

    /**
     * Context를 적용하여 노드의 visibility와 repeat를 설정합니다.
     * 기본 구현은 재귀적으로 모든 노드에 적용합니다.
     */
    protected open fun processAttributes(excelHeaderContext: ExcelHeaderContext) {
        val contextMap = excelHeaderContext.headerNodeContexts.associateBy { it.key }
        getNodes().forEach { node ->
            node.applyContextRecursively(contextMap)
        }
    }

    /**
     * 데이터에서 값을 추출합니다.
     * 각 시트에서 when 문으로 명확하게 구현합니다.
     *
     * @param data 데이터 객체 (ProductExcelData, ProductImage 등)
     * @param key 필드 키 (예: "productId", "imageName", "images")
     * @return 추출된 값
     */
    protected abstract fun extractValue(data: Any, key: String): Any?

    fun createSheet(
        workbook: SXSSFWorkbook,
        data: List<T>,
        excelHeaderContext: ExcelHeaderContext
    ) {
        val sheet = workbook.createSheet(getSheetName())
        this.processAttributes(excelHeaderContext)
        this.createHeader(sheet, workbook)
        this.writeData(sheet, data)
    }

    fun createHeader(sheet: Sheet, workbook: SXSSFWorkbook) {
        val headerRow = sheet.createRow(0)
        val headerStyle = createHeaderStyle(workbook)
        var colIndex = 0

        // 각 노드가 자신의 헤더를 작성하고 다음 컬럼 인덱스 반환
        getNodes().forEach { node ->
            colIndex = node.writeHeader(headerRow, colIndex, headerStyle)
        }
    }

    protected fun writeData(
        sheet: Sheet,
        data: List<T>,
        currentRow: Int = 1
    ) {
        var rowIndex = currentRow

        data.forEach { item ->
            val row = sheet.createRow(rowIndex++)
            var colIndex = 0

            // 각 노드가 자신의 데이터를 작성하고 다음 컬럼 인덱스 반환
            getNodes().forEach { node ->
                colIndex = node.writeData(row, item, colIndex, ::extractValue)
            }
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
}
