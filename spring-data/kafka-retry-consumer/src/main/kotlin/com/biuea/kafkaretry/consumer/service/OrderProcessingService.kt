package com.biuea.kafkaretry.consumer.service

import com.biuea.kafkaretry.common.dto.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderProcessingService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun processOrder(event: OrderEvent) {
        if (event.simulateFailure) {
            throw RuntimeException("Simulated order processing failure for order: ${event.id}, user: ${event.userId}")
        }
        log.info("Order processed successfully: id={}, userId={}, product={}, quantity={}, total={}",
            event.id, event.userId, event.productName, event.quantity, event.totalAmount)
    }
}
