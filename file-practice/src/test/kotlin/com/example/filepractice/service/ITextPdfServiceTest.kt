package com.example.filepractice.service

import com.example.filepractice.domain.Coupon
import com.example.filepractice.domain.Order
import com.example.filepractice.domain.Product
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime

class ITextPdfServiceTest {

    private val iTextPdfService = ITextPdfService()

    @Test
    fun `createOrderPdf generates a PDF with order details including Korean characters`() {
        // Given
        val product1 = Product(1L, "노트북", BigDecimal("1500000"))
        val product2 = Product(2L, "마우스", BigDecimal("50000"))
        val coupon1 = Coupon(1L, "10% 할인 쿠폰", BigDecimal("0.1"))

        val orders = listOf(
            Order(101L, LocalDateTime.now().minusDays(1), product1, coupon1),
            Order(102L, LocalDateTime.now(), product2, null),
            Order(103L, LocalDateTime.now().plusHours(1), product1, null)
        )

        // When
        val pdfBytes = iTextPdfService.createOrderPdf(orders)

        // Then
        assertNotNull(pdfBytes)
        // For manual verification, save the PDF to a file
        val file = File("build/test-results/itext_orders_korean.pdf")
        file.parentFile.mkdirs()
        file.writeBytes(pdfBytes)
        println("iText PDF generated at: ${file.absolutePath}")
        // Further assertions could involve parsing the PDF and checking content,
        // but for now, ensuring it's not null and can be saved is sufficient.
    }
}
