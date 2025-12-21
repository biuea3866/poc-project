package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.SheetType
import com.example.filepractice.poc.domain.excel.ShipmentData
import org.apache.poi.ss.usermodel.Sheet

class ShipmentExcelSheet : AbstractExcelSheet<ShipmentData>() {
    override val sheetType: SheetType
        get() = SheetType.SHIPMENT

    override fun getSheetName(): String = "배송 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "배송 ID",
                key = "shipmentId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "주문 ID",
                key = "orderId",
                width = 15,
                order = 2,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "송장 번호",
                key = "trackingNumber",
                width = 25,
                order = 3,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "택배사",
                key = "carrier",
                width = 20,
                order = 4,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "배송 상태",
                key = "status",
                width = 15,
                order = 5,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "발송일시",
                key = "shippedAt",
                width = 20,
                order = 6,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "예상 도착일시",
                key = "estimatedDeliveryAt",
                width = 20,
                order = 7,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "실제 배송일시",
                key = "deliveredAt",
                width = 20,
                order = 8,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "수령인",
                key = "recipientName",
                width = 20,
                order = 9,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "수령인 연락처",
                key = "recipientPhone",
                width = 20,
                order = 10,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "배송지",
                key = "recipientAddress",
                width = 45,
                order = 11,
                alignment = Alignment.LEFT
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<ShipmentData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { shipmentData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "shipmentId" -> CellWriter.writeNumericCell(
                        row, colIndex, shipmentData.shipmentId, style
                    )
                    "orderId" -> CellWriter.writeNumericCell(
                        row, colIndex, shipmentData.shipmentId % 1000, style
                    )
                    "trackingNumber" -> CellWriter.writeStringCell(
                        row, colIndex, generateTrackingNumber(shipmentData.shipmentId), style
                    )
                    "carrier" -> CellWriter.writeStringCell(
                        row, colIndex, getCarrier(shipmentData.shipmentId), style
                    )
                    "status" -> CellWriter.writeStringCell(
                        row, colIndex, getShipmentStatus(shipmentData.shipmentId), style
                    )
                    "shippedAt" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-11-28 14:00:00", style
                    )
                    "estimatedDeliveryAt" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-11-30 18:00:00", style
                    )
                    "deliveredAt" -> CellWriter.writeStringCell(
                        row, colIndex, if (shipmentData.shipmentId % 3 == 0) "2024-11-29 16:30:00" else null, style
                    )
                    "recipientName" -> CellWriter.writeStringCell(
                        row, colIndex, "수령인 #${shipmentData.shipmentId % 100}", style
                    )
                    "recipientPhone" -> CellWriter.writeStringCell(
                        row, colIndex, "010-${String.format("%04d", shipmentData.shipmentId % 10000)}-${String.format("%04d", shipmentData.shipmentId % 10000)}", style
                    )
                    "recipientAddress" -> CellWriter.writeStringCell(
                        row, colIndex, "서울특별시 강남구 테헤란로 ${shipmentData.shipmentId % 500}번길 ${shipmentData.shipmentId % 100}호", style
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

    private fun generateTrackingNumber(shipmentId: Int): String {
        return String.format("%013d", shipmentId * 1000000L + 123456789)
    }

    private fun getCarrier(shipmentId: Int): String {
        val carriers = listOf("CJ대한통운", "롯데택배", "우체국택배", "한진택배", "로젠택배")
        return carriers[shipmentId % carriers.size]
    }

    private fun getShipmentStatus(shipmentId: Int): String {
        return when (shipmentId % 7) {
            0 -> "준비중"
            1 -> "발송됨"
            2 -> "배송중"
            3 -> "배송완료"
            4 -> "배송출발"
            5 -> "배송실패"
            else -> "반송"
        }
    }
}
