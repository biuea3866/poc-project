package com.example.filepractice.claude.service

import com.example.filepractice.claude.domain.Order
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.time.format.DateTimeFormatter

/**
 * PDF 생성 서비스
 *
 * iText7 라이브러리를 사용하여 주문 내역을 PDF로 생성합니다.
 * Sequence + OutputStream을 사용하여 대용량 데이터 처리 시 OOM 문제를 방지합니다.
 * - Sequence: 입력 데이터를 지연 평가(lazy evaluation)로 처리
 * - OutputStream: 출력을 직접 스트림으로 전송하여 메모리 절약
 * - 한글 폰트는 font-asian 패키지의 내장 폰트를 사용합니다.
 */
@Service
class PdfGenerationService {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * 주문 내역을 PDF 파일로 생성 (List 방식 - 소량 데이터용)
     *
     * @param orders 주문 목록
     * @param outputStream 출력 스트림
     */
    fun generateOrderPdf(orders: List<Order>, outputStream: OutputStream) {
        generateOrderPdfFromSequence(orders.asSequence(), outputStream)
    }

    /**
     * 주문 내역을 PDF 파일로 생성 (Sequence 방식 - 대용량 데이터용)
     *
     * Sequence를 사용하여 메모리에 전체 데이터를 로드하지 않고 스트리밍 방식으로 처리
     * iText는 내부적으로 페이지 단위로 flush하므로 메모리 효율적
     *
     * @param orders 주문 Sequence (지연 평가)
     * @param outputStream 출력 스트림
     */
    fun generateOrderPdfFromSequence(orders: Sequence<Order>, outputStream: OutputStream) {
        // OutputStream에 직접 쓰기 (메모리에 전체 파일을 올리지 않음)
        val writer = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)

        try {
            // 한글 폰트 설정 (iText font-asian 패키지의 내장 한글 폰트 사용)
            val koreanFont = PdfFontFactory.createFont("HYSMyeongJo-Medium", "UniKS-UCS2-H")

            // 제목 추가
            addTitle(document, koreanFont)

            // 주문별로 상세 정보 표시 (Sequence로 한 번에 하나씩 처리)
            var processedCount = 0
            orders.forEach { order ->
                addOrderSection(document, order, koreanFont)
                document.add(Paragraph("\n")) // 주문 간 여백

                processedCount++

                // 주기적으로 flush하여 메모리 압력 감소
                if (processedCount % 100 == 0) {
                    document.flush()
                }
            }

        } finally {
            // 문서 닫기 (자동으로 남은 내용을 flush)
            document.close()
        }
    }

    /**
     * 제목 추가
     */
    private fun addTitle(document: Document, font: PdfFont) {
        val title = Paragraph("주문 내역서")
            .setFont(font)
            .setFontSize(20f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(title)
        document.add(Paragraph("\n"))
    }

    /**
     * 주문 섹션 추가
     */
    private fun addOrderSection(document: Document, order: Order, font: PdfFont) {
        // 주문 정보 제목
        val orderTitle = Paragraph("주문 번호: ${order.orderNumber}")
            .setFont(font)
            .setFontSize(14f)
            .setBold()
        document.add(orderTitle)

        // 주문 기본 정보 테이블
        addOrderInfoTable(document, order, font)

        // 상품 목록 테이블
        addProductTable(document, order, font)

        // 쿠폰 정보 (있는 경우)
        order.coupon?.let { coupon ->
            addCouponInfo(document, coupon, font)
        }

        // 구분선
        document.add(Paragraph("─".repeat(80))
            .setFont(font)
            .setFontSize(10f))
    }

    /**
     * 주문 기본 정보 테이블 추가
     */
    private fun addOrderInfoTable(document: Document, order: Order, font: PdfFont) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // 헤더 스타일 적용 함수
        fun createHeaderCell(text: String): Cell {
            return Cell()
                .add(Paragraph(text).setFont(font).setFontSize(10f))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
        }

        fun createDataCell(text: String): Cell {
            return Cell()
                .add(Paragraph(text).setFont(font).setFontSize(10f))
        }

        table.addCell(createHeaderCell("주문 ID"))
        table.addCell(createDataCell(order.id.toString()))

        table.addCell(createHeaderCell("사용자 ID"))
        table.addCell(createDataCell(order.userId.toString()))

        table.addCell(createHeaderCell("주문 일시"))
        table.addCell(createDataCell(order.orderDate.format(dateFormatter)))

        table.addCell(createHeaderCell("주문 상태"))
        table.addCell(createDataCell(order.status.name))

        table.addCell(createHeaderCell("총 금액"))
        table.addCell(createDataCell("${order.totalAmount}원"))

        table.addCell(createHeaderCell("할인 후 금액"))
        table.addCell(createDataCell("${order.discountedAmount}원"))

        document.add(table)
        document.add(Paragraph("\n"))
    }

    /**
     * 상품 목록 테이블 추가
     */
    private fun addProductTable(document: Document, order: Order, font: PdfFont) {
        val sectionTitle = Paragraph("주문 상품 목록")
            .setFont(font)
            .setFontSize(12f)
            .setBold()
        document.add(sectionTitle)

        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 30f, 15f, 15f, 15f, 15f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // 헤더
        listOf("상품ID", "상품명", "가격", "수량", "카테고리", "합계").forEach { header ->
            table.addCell(
                Cell()
                    .add(Paragraph(header).setFont(font).setFontSize(9f))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        // 데이터
        order.products.forEach { product ->
            val subtotal = product.price.multiply(product.quantity.toBigDecimal())

            table.addCell(Cell().add(Paragraph(product.id.toString()).setFont(font).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(product.name).setFont(font).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("${product.price}원").setFont(font).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(product.quantity.toString()).setFont(font).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(product.category).setFont(font).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("${subtotal}원").setFont(font).setFontSize(9f)))
        }

        document.add(table)
        document.add(Paragraph("\n"))
    }

    /**
     * 쿠폰 정보 추가
     */
    private fun addCouponInfo(
        document: Document,
        coupon: com.example.filepractice.claude.domain.Coupon,
        font: PdfFont
    ) {
        val couponInfo = Paragraph("적용된 쿠폰: ${coupon.name} (${coupon.code}) - 할인: ${coupon.discountAmount}원")
            .setFont(font)
            .setFontSize(10f)
            .setItalic()
        document.add(couponInfo)
        document.add(Paragraph("\n"))
    }
}
