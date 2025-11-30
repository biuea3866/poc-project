package com.example.filepractice.poc.domain.excel

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

    fun writeStringCell(row: Row, columnIndex: Int, value: String?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeNumericCell(row: Row, columnIndex: Int, value: Number?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it.toDouble()) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeDateCell(row: Row, columnIndex: Int, value: LocalDate?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it.format(DATE_FORMATTER)) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeDateTimeCell(row: Row, columnIndex: Int, value: LocalDateTime?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it.format(DATE_TIME_FORMATTER)) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeYearMonthCell(row: Row, columnIndex: Int, value: LocalDate?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it.format(YEAR_MONTH_FORMATTER)) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeBooleanCell(row: Row, columnIndex: Int, value: Boolean?, style: CellStyle? = null): Cell {
        val cell = row.createCell(columnIndex)
        value?.let { cell.setCellValue(it) }
        style?.let { cell.cellStyle = it }
        return cell
    }

    fun writeJoinedCell(
        row: Row,
        columnIndex: Int,
        values: List<String?>,
        separator: String = ", ",
        style: CellStyle? = null
    ): Cell {
        val joinedValue = values.filterNotNull().joinToString(separator)
        return writeStringCell(row, columnIndex, joinedValue.ifEmpty { null }, style)
    }
}
