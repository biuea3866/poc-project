package com.example.filepractice.poc.domain.newexcel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CellWriter {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")

    fun writeCell(row: Row, columnIndex: Int, value: Any?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let {
            when (it) {
                is Number -> cell.setCellValue(it.toDouble())
                else -> cell.setCellValue(it.toString())
            }
        }
        style?.let { cell.cellStyle = it }
        return cell
    }
}
