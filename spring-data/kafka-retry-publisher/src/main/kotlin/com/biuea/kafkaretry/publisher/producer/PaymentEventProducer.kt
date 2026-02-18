package com.biuea.kafkaretry.publisher.producer

import com.biuea.kafkaretry.common.constant.RetryTopics
import com.biuea.kafkaretry.common.dto.PaymentEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class PaymentEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(event: PaymentEvent) {
        kafkaTemplate.send(RetryTopics.PAYMENT_TOPIC, event.id, event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("Failed to send payment event {}: {}", event.id, ex.message)
                } else {
                    log.info("Payment event sent: {} -> partition: {}, offset: {}",
                        event.id, result.recordMetadata.partition(), result.recordMetadata.offset())
                }
            }
    }
}
