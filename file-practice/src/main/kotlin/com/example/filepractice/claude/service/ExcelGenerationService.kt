package com.example.filepractice.claude.service

import com.example.filepractice.claude.domain.Order
import com.example.filepractice.claude.dto.ColumnConfig
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.time.format.DateTimeFormatter

/**
 * 엑셀 생성 서비스
 *
 * SXSSFWorkbook + Sequence를 사용하여 대용량 데이터 처리 시 OOM 문제를 완전히 방지합니다.
 * - SXSSFWorkbook: 메모리에 100개 행만 유지하고 나머지는 디스크에 임시 저장
 * - Sequence: 입력 데이터를 지연 평가(lazy evaluation)로 처리
 * - OutputStream: 출력을 직접 스트림으로 전송하여 메모리 절약
 */
@Service
class ExcelGenerationService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    companion object {
        private const val ROW_ACCESS_WINDOW_SIZE = 100 // SXSSFWorkbook 윈도우 크기
    }

    /**
     * 주문 내역을 엑셀 파일로 생성 (List 방식 - 소량 데이터용)
     *
     * @param orders 주문 목록
     * @param columnConfig 컬럼 설정
     * @param outputStream 출력 스트림
     */
    fun generateOrderExcel(
        orders: List<Order>,
        columnConfig: ColumnConfig,
        outputStream: OutputStream
    ) {
        generateOrderExcelFromSequence(orders.asSequence(), columnConfig, outputStream)
    }

    /**
     * 주문 내역을 엑셀 파일로 생성 (Sequence 방식 - 대용량 데이터용)
     *
     * Sequence를 사용하여 메모리에 전체 데이터를 로드하지 않고 스트리밍 방식으로 처리
     *
     * @param orders 주문 Sequence (지연 평가)
     * @param columnConfig 컬럼 설정
     * @param outputStream 출력 스트림
     */
    fun generateOrderExcelFromSequence(
        orders: Sequence<Order>,
        columnConfig: ColumnConfig,
        outputStream: OutputStream
    ) {
        // SXSSFWorkbook: 메모리에 100개 행만 유지하고 나머지는 디스크에 임시 저장
        val workbook = SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE)

        // 압축 사용으로 임시 파일 크기 절약
        workbook.isCompressTempFiles = true

        try {
            // 주문 내역 시트와 상품 시트 생성
            val orderSheet = workbook.createSheet("주문 내역")
            val productSheet = workbook.createSheet("상품")

            val headerStyle = createHeaderStyle(workbook)
            val orderColumns = columnConfig.orderColumns.toList()
            val productColumns = columnConfig.productColumns.toList()

            // 주문 내역 시트 헤더
            createOrderSheetHeader(orderSheet, orderColumns, headerStyle)

            // 상품 시트 헤더
            createProductSheetHeader(productSheet, productColumns, headerStyle)

            // 데이터 스트리밍 처리 (Sequence로 한 번에 하나씩 처리)
            var orderRowIndex = 1
            var productRowIndex = 1

            orders.forEach { order ->
                // 주문 내역 시트에 행 추가
                val orderRow = orderSheet.createRow(orderRowIndex++)
                orderColumns.forEachIndexed { columnIndex, column ->
                    val cell = orderRow.createCell(columnIndex)
                    val value = getOrderColumnValue(order, column)
                    cell.setCellValue(value)
                }

                // 상품 시트에 행 추가 (각 주문의 상품마다)
                order.products.forEach { product ->
                    val productRow = productSheet.createRow(productRowIndex++)
                    var cellIndex = 0

                    // 주문 번호
                    productRow.createCell(cellIndex++).setCellValue(order.orderNumber)

                    // 상품 정보
                    productColumns.forEach { column ->
                        val cell = productRow.createCell(cellIndex++)
                        val value = getProductColumnValue(product, column)
                        cell.setCellValue(value)
                    }
                }

                // 중요: 메모리 해제를 위해 주기적으로 flush (선택적)
                if (orderRowIndex % 1000 == 0) {
                    // SXSSFWorkbook은 자동으로 오래된 행을 디스크로 flush하지만
                    // 명시적으로 호출하여 메모리 압력 감소 가능
                }
            }

            // 컬럼 너비 설정
            setColumnWidths(orderSheet, orderColumns.size)
            setColumnWidths(productSheet, productColumns.size + 1)

            // OutputStream에 직접 쓰기 (메모리에 전체 파일을 올리지 않음)
            workbook.write(outputStream)
            outputStream.flush()

        } finally {
            // 임시 파일 정리
            workbook.dispose()
            workbook.close()
        }
    }

    /**
     * 주문 내역 시트 헤더 생성
     */
    private fun createOrderSheetHeader(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        orderColumns: List<ColumnConfig.OrderColumn>,
        headerStyle: CellStyle
    ) {
        val headerRow = sheet.createRow(0)
        orderColumns.forEachIndexed { index, column ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(column.displayName)
            cell.cellStyle = headerStyle
        }
    }

    /**
     * 상품 시트 헤더 생성
     */
    private fun createProductSheetHeader(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        productColumns: List<ColumnConfig.ProductColumn>,
        headerStyle: CellStyle
    ) {
        val headerRow = sheet.createRow(0)
        var columnIndex = 0

        // 주문 번호 컬럼
        headerRow.createCell(columnIndex++).apply {
            setCellValue("주문 번호")
            cellStyle = headerStyle
        }

        // 상품 컬럼
        productColumns.forEach { column ->
            val cell = headerRow.createCell(columnIndex++)
            cell.setCellValue(column.displayName)
            cell.cellStyle = headerStyle
        }
    }

    /**
     * 컬럼 너비 설정
     */
    private fun setColumnWidths(sheet: org.apache.poi.ss.usermodel.Sheet, columnCount: Int) {
        for (i in 0 until columnCount) {
            sheet.setColumnWidth(i, 4000) // 약 20자 너비
        }
    }

    /**
     * 헤더 스타일 생성
     */
    private fun createHeaderStyle(workbook: SXSSFWorkbook): CellStyle {
        return workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply {
                bold = true
            })
        }
    }

    /**
     * 주문 컬럼 값 조회
     */
    private fun getOrderColumnValue(order: Order, column: ColumnConfig.OrderColumn): String {
        return when (column) {
            ColumnConfig.OrderColumn.ID -> order.id.toString()
            ColumnConfig.OrderColumn.ORDER_NUMBER -> order.orderNumber
            ColumnConfig.OrderColumn.USER_ID -> order.userId.toString()
            ColumnConfig.OrderColumn.TOTAL_AMOUNT -> order.totalAmount.toString()
            ColumnConfig.OrderColumn.DISCOUNTED_AMOUNT -> order.discountedAmount.toString()
            ColumnConfig.OrderColumn.ORDER_DATE -> order.orderDate.format(dateFormatter)
            ColumnConfig.OrderColumn.STATUS -> order.status.name
        }
    }

    /**
     * 상품 컬럼 값 조회
     */
    private fun getProductColumnValue(
        product: com.example.filepractice.claude.domain.Product,
        column: ColumnConfig.ProductColumn
    ): String {
        return when (column) {
            ColumnConfig.ProductColumn.ID -> product.id.toString()
            ColumnConfig.ProductColumn.NAME -> product.name
            ColumnConfig.ProductColumn.PRICE -> product.price.toString()
            ColumnConfig.ProductColumn.QUANTITY -> product.quantity.toString()
            ColumnConfig.ProductColumn.CATEGORY -> product.category
        }
    }
}
