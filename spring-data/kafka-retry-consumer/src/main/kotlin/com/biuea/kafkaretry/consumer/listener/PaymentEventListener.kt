package com.biuea.kafkaretry.consumer.listener

import com.biuea.kafkaretry.common.constant.RetryTopics
import com.biuea.kafkaretry.common.dto.PaymentEvent
import com.biuea.kafkaretry.consumer.service.PaymentProcessingService
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PaymentEventListener(
    private val paymentProcessingService: PaymentProcessingService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [RetryTopics.PAYMENT_TOPIC],
        groupId = RetryTopics.GROUP_PAYMENT,
        containerFactory = "paymentKafkaListenerContainerFactory"
    )
    fun listen(record: ConsumerRecord<String, String>) {
        val payment = objectMapper.readValue(record.value(), PaymentEvent::class.java)
        log.info("[ErrorHandler] Processing payment: {} for order: {} (attempt will be retried by DefaultErrorHandler on failure)",
            payment.id, payment.orderId)
        paymentProcessingService.processPayment(payment)
    }
}
