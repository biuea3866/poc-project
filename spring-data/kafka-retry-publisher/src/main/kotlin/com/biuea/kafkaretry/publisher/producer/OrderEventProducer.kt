package com.biuea.kafkaretry.publisher.producer

import com.biuea.kafkaretry.common.constant.RetryTopics
import com.biuea.kafkaretry.common.dto.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class OrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(event: OrderEvent) {
        kafkaTemplate.send(RetryTopics.ORDER_TOPIC, event.id, event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("Failed to send order event {}: {}", event.id, ex.message)
                } else {
                    log.info("Order event sent: {} -> partition: {}, offset: {}",
                        event.id, result.recordMetadata.partition(), result.recordMetadata.offset())
                }
            }
    }
}
