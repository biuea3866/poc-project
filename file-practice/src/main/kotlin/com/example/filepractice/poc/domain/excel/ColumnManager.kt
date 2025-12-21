package com.example.filepractice.poc.domain.excel

class ColumnManager {
    fun applyConfig(
        columns: List<ColumnDefinition>,
        config: ColumnConfig?
    ): List<ColumnDefinition> {
        if (config == null) return columns

        return config.processVisibleColumns(columns).sortedBy { it.order }
    }
}

data class ColumnConfig(val excludedColumns: Set<String> = emptySet()) {
    private fun isExcludeTarget(columnName: String) = excludedColumns.contains(columnName)

    fun processVisibleColumns(columnDefinition: List<ColumnDefinition>): List<ColumnDefinition> {
        if (this.excludedColumns.isEmpty()) return columnDefinition

        return columnDefinition.filter { !isExcludeTarget(it.name) }
            .map { it.exclude() }
    }
}
