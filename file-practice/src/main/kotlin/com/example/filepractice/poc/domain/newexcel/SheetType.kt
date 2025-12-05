package com.example.filepractice.poc.domain.newexcel

enum class SheetType {
    PRODUCT,
    ORDER,
    USER,
    CATEGORY,
    REVIEW,
    SHIPMENT
    ;

    companion object {
        fun getSheetTypesBy(workbookType: WorkbookType) = when (workbookType) {
            WorkbookType.FULL -> FULL_WORKBOOK_SHEET_TYPE
            WorkbookType.SIMPLE -> SIMPLE_WORKBOOK_SHEET_TYPE
        }

        private val FULL_WORKBOOK_SHEET_TYPE = listOf(
            PRODUCT,
            ORDER,
            USER,
            CATEGORY,
            REVIEW,
            SHIPMENT
        )

        private val SIMPLE_WORKBOOK_SHEET_TYPE = listOf(
            ORDER
        )
    }
}