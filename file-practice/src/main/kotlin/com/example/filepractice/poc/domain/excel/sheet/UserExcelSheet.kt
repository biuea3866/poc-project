package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.SheetType
import com.example.filepractice.poc.domain.excel.UserData
import org.apache.poi.ss.usermodel.Sheet

class UserExcelSheet : AbstractExcelSheet<UserData>() {
    override val sheetType: SheetType
        get() = SheetType.USER

    override fun getSheetName(): String = "사용자 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "사용자 ID",
                key = "userId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "이름",
                key = "name",
                width = 20,
                order = 2,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "이메일",
                key = "email",
                width = 30,
                order = 3,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "전화번호",
                key = "phone",
                width = 20,
                order = 4,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "주소",
                key = "address",
                width = 40,
                order = 5,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "가입일",
                key = "joinedAt",
                width = 15,
                order = 6,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "회원 등급",
                key = "membershipLevel",
                width = 15,
                order = 7,
                alignment = Alignment.CENTER
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<UserData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { userData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "userId" -> CellWriter.writeNumericCell(
                        row, colIndex, userData.userId, style
                    )
                    "name" -> CellWriter.writeStringCell(
                        row, colIndex, "사용자 #${userData.userId}", style
                    )
                    "email" -> CellWriter.writeStringCell(
                        row, colIndex, "user${userData.userId}@example.com", style
                    )
                    "phone" -> CellWriter.writeStringCell(
                        row, colIndex, "010-${String.format("%04d", userData.userId % 10000)}-${String.format("%04d", userData.userId % 10000)}", style
                    )
                    "address" -> CellWriter.writeStringCell(
                        row, colIndex, "서울특별시 강남구 테헤란로 ${userData.userId % 500}번길", style
                    )
                    "joinedAt" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-01-01", style
                    )
                    "membershipLevel" -> CellWriter.writeStringCell(
                        row, colIndex, getMembershipLevel(userData.userId), style
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

    private fun getMembershipLevel(userId: Int): String {
        return when (userId % 4) {
            0 -> "BRONZE"
            1 -> "SILVER"
            2 -> "GOLD"
            else -> "PLATINUM"
        }
    }
}
