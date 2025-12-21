package com.example.filepractice.service

import com.example.filepractice.domain.Order
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Service

@Service
class ExcelService {

    /**
     * 주문 목록과 사용자가 선택한 열을 기반으로 엑셀 워크북을 생성합니다.
     * 대용량 데이터 처리를 위해 스트리밍 방식(SXSSF)을 사용합니다.
     *
     * @param orders 주문 데이터 리스트
     * @param selectedColumns 사용자가 다운로드하기 원하는 열의 집합
     * @return 생성된 엑셀 워크북
     */
    fun createOrderExcel(orders: List<Order>, selectedColumns: Set<String>): Workbook {
        // 메모리에 유지할 행 수를 100으로 설정, 초과 시 디스크에 임시 파일로 저장
        val workbook = SXSSFWorkbook(100)

        try {
            // 주문 내역 시트 생성
            createOrderSheet(workbook, orders, selectedColumns)

            // 상품 시트 생성
            createProductSheet(workbook, orders, selectedColumns)
        } catch (e: Exception) {
            // 에러 발생 시 임시 파일 정리
            workbook.dispose()
            throw e
        }

        return workbook
    }

    private fun createOrderSheet(workbook: SXSSFWorkbook, orders: List<Order>, selectedColumns: Set<String>) {
        val sheet = workbook.createSheet("주문 내역")

        // 전체 가능한 헤더 정의
        val allHeaders = linkedMapOf(
            "order_id" to "주문 ID",
            "order_at" to "주문 일시",
            "product_name" to "상품명",
            "coupon_name" to "적용 쿠폰명"
        )

        // 선택된 헤더만 필터링
        val headers = allHeaders.filterKeys { it in selectedColumns }
        val headerRow = sheet.createRow(0)
        headers.values.forEachIndexed { index, headerName ->
            headerRow.createCell(index).setCellValue(headerName)
        }

        // 데이터 행 생성
        orders.forEachIndexed { index, order ->
            val row = sheet.createRow(index + 1)
            var cellIndex = 0
            if ("order_id" in headers) row.createCell(cellIndex++).setCellValue(order.id.toDouble())
            if ("order_at" in headers) row.createCell(cellIndex++).setCellValue(order.orderAt.toString())
            if ("product_name" in headers) row.createCell(cellIndex++).setCellValue(order.product.name)
            if ("coupon_name" in headers) row.createCell(cellIndex++).setCellValue(order.coupon?.name ?: "N/A")
        }
    }

    private fun createProductSheet(workbook: SXSSFWorkbook, orders: List<Order>, selectedColumns: Set<String>) {
        val sheet = workbook.createSheet("상품")

        val allHeaders = linkedMapOf(
            "product_id" to "상품 ID",
            "product_name" to "상품명",
            "product_price" to "상품 가격"
        )

        val headers = allHeaders.filterKeys { it in selectedColumns }
        val headerRow = sheet.createRow(0)
        headers.values.forEachIndexed { index, headerName ->
            headerRow.createCell(index).setCellValue(headerName)
        }

        // 중복을 제거한 상품 목록
        val products = orders.map { it.product }.distinctBy { it.id }

        products.forEachIndexed { index, product ->
            val row = sheet.createRow(index + 1)
            var cellIndex = 0
            if ("product_id" in headers) row.createCell(cellIndex++).setCellValue(product.id.toDouble())
            if ("product_name" in headers) row.createCell(cellIndex++).setCellValue(product.name)
            if ("product_price" in headers) row.createCell(cellIndex++).setCellValue(product.price.toDouble())
        }
    }
}
