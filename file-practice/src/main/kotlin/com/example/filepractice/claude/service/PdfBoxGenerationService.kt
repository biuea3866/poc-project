package com.example.filepractice.claude.service

import com.example.filepractice.claude.domain.Order
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.time.format.DateTimeFormatter

/**
 * PDFBox 기반 PDF 생성 서비스
 *
 * Apache PDFBox 라이브러리를 사용하여 주문 내역을 PDF로 생성합니다.
 * Sequence + OutputStream을 사용하여 대용량 데이터 처리 시 OOM 문제를 방지합니다.
 *
 * Note: PDFBox는 한글을 지원하지 않는 표준 폰트를 사용합니다.
 * 한글 지원이 필요한 경우 iText를 사용하세요.
 */
@Service
class PdfBoxGenerationService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    companion object {
        private const val MARGIN = 50f
        private const val LINE_HEIGHT = 15f
        private const val TITLE_FONT_SIZE = 20f
        private const val SECTION_TITLE_FONT_SIZE = 14f
        private const val NORMAL_FONT_SIZE = 10f
        private const val TABLE_FONT_SIZE = 9f
    }

    /**
     * 주문 내역을 PDF 파일로 생성 (List 방식)
     */
    fun generateOrderPdf(orders: List<Order>, outputStream: OutputStream) {
        generateOrderPdfFromSequence(orders.asSequence(), outputStream)
    }

    /**
     * 주문 내역을 PDF 파일로 생성 (Sequence 방식 - 대용량 데이터용)
     */
    fun generateOrderPdfFromSequence(orders: Sequence<Order>, outputStream: OutputStream) {
        val document = PDDocument()

        try {
            // PDFBox 표준 폰트 사용 (한글 미지원)
            val font = PDType1Font.HELVETICA

            var currentPage: PDPage? = null
            var contentStream: PDPageContentStream? = null
            var yPosition = 0f

            // 제목 추가
            val titlePage = PDPage(PDRectangle.A4)
            document.addPage(titlePage)
            currentPage = titlePage

            contentStream = PDPageContentStream(document, currentPage)
            yPosition = addTitle(contentStream, font)

            // 주문별로 처리
            var processedCount = 0
            orders.forEach { order ->
                // 페이지 공간 확인 (주문 하나가 최소 200 포인트 필요)
                if (yPosition < 200f) {
                    // 새 페이지 필요
                    contentStream?.close()
                    val newPage = PDPage(PDRectangle.A4)
                    document.addPage(newPage)
                    currentPage = newPage
                    contentStream = PDPageContentStream(document, currentPage)
                    yPosition = PDRectangle.A4.height - MARGIN
                }

                yPosition = addOrderSection(contentStream!!, order, font, yPosition)

                processedCount++

                // 주기적으로 페이지 flush (메모리 관리)
                if (processedCount % 10 == 0) {
                    contentStream?.close()
                    if (processedCount < orders.count()) {
                        val newPage = PDPage(PDRectangle.A4)
                        document.addPage(newPage)
                        currentPage = newPage
                        contentStream = PDPageContentStream(document, currentPage)
                        yPosition = PDRectangle.A4.height - MARGIN
                    }
                }
            }

            contentStream?.close()

            // OutputStream에 쓰기
            document.save(outputStream)

        } finally {
            document.close()
        }
    }

    /**
     * 제목 추가
     */
    private fun addTitle(stream: PDPageContentStream, font: PDType1Font): Float {
        var y = PDRectangle.A4.height - MARGIN

        stream.beginText()
        stream.setFont(font, TITLE_FONT_SIZE)
        stream.newLineAtOffset(MARGIN, y)
        stream.showText("Order History Report")
        stream.endText()

        y -= LINE_HEIGHT * 3
        return y
    }

    /**
     * 주문 섹션 추가
     */
    private fun addOrderSection(
        stream: PDPageContentStream,
        order: Order,
        font: PDType1Font,
        startY: Float
    ): Float {
        var y = startY

        // 주문 번호 (섹션 제목)
        stream.beginText()
        stream.setFont(font, SECTION_TITLE_FONT_SIZE)
        stream.newLineAtOffset(MARGIN, y)
        stream.showText("Order #: ${order.orderNumber}")
        stream.endText()
        y -= LINE_HEIGHT * 2

        // 주문 기본 정보
        y = addOrderInfo(stream, order, font, y)

        // 상품 목록
        y = addProductList(stream, order, font, y)

        // 쿠폰 정보
        order.coupon?.let { coupon ->
            y = addCouponInfo(stream, coupon, font, y)
        }

        // 구분선
        stream.setLineWidth(0.5f)
        stream.moveTo(MARGIN, y)
        stream.lineTo(PDRectangle.A4.width - MARGIN, y)
        stream.stroke()
        y -= LINE_HEIGHT * 2

        return y
    }

    /**
     * 주문 기본 정보 추가
     */
    private fun addOrderInfo(
        stream: PDPageContentStream,
        order: Order,
        font: PDType1Font,
        startY: Float
    ): Float {
        var y = startY

        val info = listOf(
            "Order ID: ${order.id}",
            "User ID: ${order.userId}",
            "Order Date: ${order.orderDate.format(dateFormatter)}",
            "Status: ${order.status.name}",
            "Total: ${order.totalAmount} KRW",
            "Discounted: ${order.discountedAmount} KRW"
        )

        stream.beginText()
        stream.setFont(font, NORMAL_FONT_SIZE)
        stream.newLineAtOffset(MARGIN + 10, y)

        info.forEach { line ->
            stream.showText(line)
            stream.newLineAtOffset(0f, -LINE_HEIGHT)
            y -= LINE_HEIGHT
        }

        stream.endText()
        y -= LINE_HEIGHT

        return y
    }

    /**
     * 상품 목록 추가
     */
    private fun addProductList(
        stream: PDPageContentStream,
        order: Order,
        font: PDType1Font,
        startY: Float
    ): Float {
        var y = startY

        // 섹션 제목
        stream.beginText()
        stream.setFont(font, NORMAL_FONT_SIZE + 2)
        stream.newLineAtOffset(MARGIN + 10, y)
        stream.showText("Product List")
        stream.endText()
        y -= LINE_HEIGHT * 1.5f

        // 테이블 헤더
        val headers = listOf("ID", "Name", "Price", "Qty", "Category", "Subtotal")
        val columnWidths = listOf(50f, 120f, 80f, 50f, 80f, 80f)
        val tableWidth = columnWidths.sum()
        val startX = MARGIN + 10

        // 헤더 배경
        stream.setNonStrokingColor(0.9f, 0.9f, 0.9f)
        stream.addRect(startX, y - 12, tableWidth, 14f)
        stream.fill()
        stream.setNonStrokingColor(0f, 0f, 0f)

        // 헤더 텍스트
        stream.beginText()
        stream.setFont(font, TABLE_FONT_SIZE)
        var xPos = startX + 2
        stream.newLineAtOffset(xPos, y - 10)
        headers.forEachIndexed { index, header ->
            stream.showText(header)
            if (index < headers.size - 1) {
                xPos += columnWidths[index]
                stream.newLineAtOffset(columnWidths[index], 0f)
            }
        }
        stream.endText()
        y -= 15f

        // 데이터 행
        order.products.forEach { product ->
            val subtotal = product.price.multiply(product.quantity.toBigDecimal())
            val row = listOf(
                product.id.toString(),
                "Product ${product.id}",
                "${product.price}",
                product.quantity.toString(),
                product.category,
                "${subtotal}"
            )

            stream.beginText()
            stream.setFont(font, TABLE_FONT_SIZE)
            xPos = startX + 2
            stream.newLineAtOffset(xPos, y - 10)
            row.forEachIndexed { index, cell ->
                val displayText = if (cell.length > 15) cell.substring(0, 12) + "..." else cell
                stream.showText(displayText)
                if (index < row.size - 1) {
                    xPos += columnWidths[index]
                    stream.newLineAtOffset(columnWidths[index], 0f)
                }
            }
            stream.endText()
            y -= 12f
        }

        y -= LINE_HEIGHT

        return y
    }

    /**
     * 쿠폰 정보 추가
     */
    private fun addCouponInfo(
        stream: PDPageContentStream,
        coupon: com.example.filepractice.claude.domain.Coupon,
        font: PDType1Font,
        startY: Float
    ): Float {
        var y = startY

        stream.beginText()
        stream.setFont(font, NORMAL_FONT_SIZE)
        stream.newLineAtOffset(MARGIN + 10, y)
        stream.showText("Coupon: ${coupon.code} - Discount: ${coupon.discountAmount} KRW")
        stream.endText()
        y -= LINE_HEIGHT * 2

        return y
    }
}
