package com.example.filepractice.claude.service

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.ExcelWriter
import com.alibaba.excel.write.metadata.WriteSheet
import com.alibaba.excel.write.metadata.WriteTable
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy
import com.example.filepractice.claude.domain.Order
import com.example.filepractice.claude.dto.ColumnConfig
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.time.format.DateTimeFormatter

/**
 * EasyExcel 기반 엑셀 생성 서비스
 *
 * EasyExcel 라이브러리를 사용하여 대용량 데이터 처리 시 메모리 효율을 극대화합니다.
 * EasyExcel은 내부적으로 SAX 파싱 방식을 사용하여 POI보다 메모리 효율이 우수합니다.
 */
@Service
class EasyExcelGenerationService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * 주문 내역을 엑셀 파일로 생성 (List 방식)
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
     */
    fun generateOrderExcelFromSequence(
        orders: Sequence<Order>,
        columnConfig: ColumnConfig,
        outputStream: OutputStream
    ) {
        val excelWriter: ExcelWriter = EasyExcel.write(outputStream)
            .autoCloseStream(false)
            .registerWriteHandler(LongestMatchColumnWidthStyleStrategy())
            .build()

        try {
            // 주문 내역 시트 생성
            val orderSheet = EasyExcel.writerSheet(0, "주문 내역").build()
            writeOrderSheet(excelWriter, orderSheet, orders, columnConfig.orderColumns.toList())

            // 상품 시트 생성
            val productSheet = EasyExcel.writerSheet(1, "상품").build()
            writeProductSheet(excelWriter, productSheet, orders, columnConfig.productColumns.toList())

        } finally {
            excelWriter.finish()
        }
    }

    /**
     * 주문 내역 시트 작성
     */
    private fun writeOrderSheet(
        excelWriter: ExcelWriter,
        writeSheet: WriteSheet,
        orders: Sequence<Order>,
        orderColumns: List<ColumnConfig.OrderColumn>
    ) {
        // 헤더 작성
        val headers = listOf(orderColumns.map { it.displayName })
        val table = EasyExcel.writerTable(0).head(headers).needHead(true).build()

        // 데이터 작성 - 청크 단위로 처리
        val chunkSize = 1000
        orders.chunked(chunkSize).forEach { chunk ->
            val data = chunk.map { order ->
                orderColumns.map { column ->
                    getOrderColumnValue(order, column)
                }
            }
            excelWriter.write(data, writeSheet, table)
        }
    }

    /**
     * 상품 시트 작성
     */
    private fun writeProductSheet(
        excelWriter: ExcelWriter,
        writeSheet: WriteSheet,
        orders: Sequence<Order>,
        productColumns: List<ColumnConfig.ProductColumn>
    ) {
        // 헤더 작성
        val headers = listOf(listOf("주문 번호") + productColumns.map { it.displayName })
        val table = EasyExcel.writerTable(0).head(headers).needHead(true).build()

        // 데이터 작성 - 청크 단위로 처리
        val chunkSize = 1000
        val productData = mutableListOf<List<String>>()

        orders.forEach { order ->
            order.products.forEach { product ->
                val row = listOf(order.orderNumber) + productColumns.map { column ->
                    getProductColumnValue(product, column)
                }
                productData.add(row)

                // 청크 크기만큼 모이면 쓰기
                if (productData.size >= chunkSize) {
                    excelWriter.write(productData, writeSheet, table)
                    productData.clear()
                }
            }
        }

        // 남은 데이터 쓰기
        if (productData.isNotEmpty()) {
            excelWriter.write(productData, writeSheet, table)
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
