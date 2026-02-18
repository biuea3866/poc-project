package com.biuea.kafkaretry.consumer.service

import com.biuea.kafkaretry.common.dto.PaymentEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun processPayment(event: PaymentEvent) {
        if (event.simulateFailure) {
            throw RuntimeException("Simulated payment processing failure for payment: ${event.id}, order: ${event.orderId}")
        }
        log.info("Payment processed successfully: id={}, orderId={}, amount={} {}",
            event.id, event.orderId, event.amount, event.currency)
    }
}
