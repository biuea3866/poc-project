package com.example.filepractice.poc.domain.excel

data class ColumnDefinition(
    val name: String,
    val key: String = name,
    val width: Int = 15,
    val order: Int = 0,
    val isVisible: Boolean = true,
    val isMergeable: Boolean = false,
    val alignment: Alignment = Alignment.LEFT
) {
    fun exclude() = this.copy(isVisible = false)
}

enum class Alignment {
    LEFT,
    CENTER,
    RIGHT
}

interface SheetDefinition {
    val sheetName: String
    val columns: List<ColumnDefinition>
}
