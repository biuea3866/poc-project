package com.example.filepractice.poc.domain.excel.sheet

import com.example.filepractice.poc.domain.excel.AbstractExcelSheet
import com.example.filepractice.poc.domain.excel.Alignment
import com.example.filepractice.poc.domain.excel.CellWriter
import com.example.filepractice.poc.domain.excel.ColumnDefinition
import com.example.filepractice.poc.domain.excel.ReviewData
import com.example.filepractice.poc.domain.excel.SheetType
import org.apache.poi.ss.usermodel.Sheet

class ReviewExcelSheet : AbstractExcelSheet<ReviewData>() {
    override val sheetType: SheetType
        get() = SheetType.REVIEW

    override fun getSheetName(): String = "리뷰 목록"

    override fun getColumnDefinitions(): List<ColumnDefinition> {
        return listOf(
            ColumnDefinition(
                name = "리뷰 ID",
                key = "reviewId",
                width = 15,
                order = 1,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "제품 ID",
                key = "productId",
                width = 15,
                order = 2,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "제품명",
                key = "productName",
                width = 30,
                order = 3,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "사용자 ID",
                key = "userId",
                width = 15,
                order = 4,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "사용자 이름",
                key = "userName",
                width = 20,
                order = 5,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "평점",
                key = "rating",
                width = 10,
                order = 6,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "제목",
                key = "title",
                width = 35,
                order = 7,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "내용",
                key = "content",
                width = 50,
                order = 8,
                alignment = Alignment.LEFT
            ),
            ColumnDefinition(
                name = "구매 확인",
                key = "isVerifiedPurchase",
                width = 12,
                order = 9,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "작성일시",
                key = "createdAt",
                width = 20,
                order = 10,
                alignment = Alignment.CENTER
            ),
            ColumnDefinition(
                name = "도움됨 수",
                key = "helpfulCount",
                width = 12,
                order = 11,
                alignment = Alignment.RIGHT
            )
        )
    }

    override fun writeData(sheet: Sheet, data: List<ReviewData>, currentRow: Int): Int {
        val visibleColumns = getCachedVisibleColumns()
        val styles = getCachedStyleMap()

        var rowIndex = currentRow
        data.forEach { reviewData ->
            val row = sheet.createRow(rowIndex++)

            visibleColumns.forEachIndexed { colIndex, column ->
                val style = styles[column.key]

                when (column.key) {
                    "reviewId" -> CellWriter.writeNumericCell(
                        row, colIndex, reviewData.reviewId, style
                    )
                    "productId" -> CellWriter.writeNumericCell(
                        row, colIndex, reviewData.reviewId % 100, style
                    )
                    "productName" -> CellWriter.writeStringCell(
                        row, colIndex, "제품 #${reviewData.reviewId % 100}", style
                    )
                    "userId" -> CellWriter.writeNumericCell(
                        row, colIndex, reviewData.reviewId % 200, style
                    )
                    "userName" -> CellWriter.writeStringCell(
                        row, colIndex, "사용자 #${reviewData.reviewId % 200}", style
                    )
                    "rating" -> CellWriter.writeNumericCell(
                        row, colIndex, (reviewData.reviewId % 5) + 1, style
                    )
                    "title" -> CellWriter.writeStringCell(
                        row, colIndex, getReviewTitle(reviewData.reviewId), style
                    )
                    "content" -> CellWriter.writeStringCell(
                        row, colIndex, getReviewContent(reviewData.reviewId), style
                    )
                    "isVerifiedPurchase" -> CellWriter.writeStringCell(
                        row, colIndex, if (reviewData.reviewId % 3 != 0) "확인됨" else "미확인", style
                    )
                    "createdAt" -> CellWriter.writeStringCell(
                        row, colIndex, "2024-11-29 10:00:00", style
                    )
                    "helpfulCount" -> CellWriter.writeNumericCell(
                        row, colIndex, reviewData.reviewId % 50, style
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

    private fun getReviewTitle(reviewId: Int): String {
        val titles = listOf(
            "정말 만족스러운 제품입니다",
            "가성비가 좋아요",
            "배송이 빨라서 좋았어요",
            "품질이 기대 이상입니다",
            "재구매 의향 있어요"
        )
        return titles[reviewId % titles.size]
    }

    private fun getReviewContent(reviewId: Int): String {
        return "리뷰 내용 #${reviewId} - 이 제품을 사용해보니 매우 만족스럽습니다. 품질도 좋고 가격 대비 성능이 우수합니다."
    }
}
