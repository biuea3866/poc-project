package com.example.filepractice.service

import com.example.filepractice.domain.Order
import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class ITextPdfService {

    fun createOrderPdf(orders: List<Order>): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = PdfWriter(baos)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        // Register Korean font
        val fontStream = ITextPdfService::class.java.classLoader.getResourceAsStream("fonts/NanumGothic.ttf")
            ?: throw IllegalStateException("Korean font file not found in resources: fonts/NanumGothic.ttf")
        val fontBytes = fontStream.readBytes()
        fontStream.close() // Explicitly close the stream
        val fontProgram = FontProgramFactory.createFont(fontBytes)
        val font = PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H)

        document.add(Paragraph("주문 목록").setFont(font).setFontSize(20f).setTextAlignment(TextAlignment.CENTER))
        document.add(Paragraph("\n"))

        orders.forEach { order ->
            document.add(Paragraph("주문 ID: ${order.id}").setFont(font))
            document.add(Paragraph("주문 시간: ${order.orderAt}").setFont(font))
            document.add(Paragraph("상품명: ${order.product.name}").setFont(font))
            document.add(Paragraph("상품 가격: ${order.product.price}").setFont(font))
            order.coupon?.let { coupon ->
                document.add(Paragraph("쿠폰명: ${coupon.name}").setFont(font))
                document.add(Paragraph("할인율: ${coupon.discountRate}").setFont(font))
            }
            document.add(Paragraph("\n"))
        }

        document.close()
        return baos.toByteArray()
    }
}
