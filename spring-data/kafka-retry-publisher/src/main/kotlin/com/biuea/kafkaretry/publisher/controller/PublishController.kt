package com.biuea.kafkaretry.publisher.controller

import com.biuea.kafkaretry.common.dto.OrderEvent
import com.biuea.kafkaretry.common.dto.PaymentEvent
import com.biuea.kafkaretry.publisher.producer.OrderEventProducer
import com.biuea.kafkaretry.publisher.producer.PaymentEventProducer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/publish")
class PublishController(
    private val paymentEventProducer: PaymentEventProducer,
    private val orderEventProducer: OrderEventProducer
) {
    @PostMapping("/payment")
    fun publishPayment(
        @RequestParam(defaultValue = "false") simulateFailure: Boolean,
        @RequestParam(defaultValue = "1") count: Int
    ): ResponseEntity<Map<String, Any>> {
        repeat(count) { i ->
            val event = PaymentEvent(
                orderId = "order-${System.currentTimeMillis()}-$i",
                amount = BigDecimal("10000"),
                simulateFailure = simulateFailure
            )
            paymentEventProducer.send(event)
        }
        return ResponseEntity.ok(mapOf(
            "published" to count,
            "topic" to "payment-topic",
            "simulateFailure" to simulateFailure
        ))
    }

    @PostMapping("/order")
    fun publishOrder(
        @RequestParam(defaultValue = "false") simulateFailure: Boolean,
        @RequestParam(defaultValue = "1") count: Int
    ): ResponseEntity<Map<String, Any>> {
        repeat(count) { i ->
            val event = OrderEvent(
                userId = "user-${System.currentTimeMillis()}-$i",
                productName = "Product-$i",
                quantity = 1,
                totalAmount = BigDecimal("50000"),
                simulateFailure = simulateFailure
            )
            orderEventProducer.send(event)
        }
        return ResponseEntity.ok(mapOf(
            "published" to count,
            "topic" to "order-topic",
            "simulateFailure" to simulateFailure
        ))
    }
}
