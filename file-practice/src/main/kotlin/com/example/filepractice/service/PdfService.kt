package com.example.filepractice.service

import com.example.filepractice.domain.Order
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal

@Service
class PdfService {

    /**
     * 주문 목록을 기반으로 PDF 문서를 생성합니다.
     *
     * @param orders 주문 데이터 리스트
     * @return 생성된 PDF 문서의 바이트 배열
     */
    fun createOrderPdf(orders: List<Order>): ByteArray {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        val contentStream = PDPageContentStream(document, page)

        // 표준 폰트 사용 (한글 미지원)
        val font = PDType1Font.HELVETICA

        contentStream.beginText()
        contentStream.setFont(font, 12f)
        contentStream.setLeading(14.5f) // Line spacing

        contentStream.newLineAtOffset(50f, 750f) // Starting position

        contentStream.showText("Test PDF Output")
        contentStream.newLine()
        contentStream.newLine()

        orders.forEachIndexed { index, order ->
            contentStream.showText("Order: ${order.id}")
            contentStream.newLine()
        }

        contentStream.endText()
        contentStream.close()

        val byteArrayOutputStream = ByteArrayOutputStream()
        document.save(byteArrayOutputStream)
        document.close()

        return byteArrayOutputStream.toByteArray()
    }
}
