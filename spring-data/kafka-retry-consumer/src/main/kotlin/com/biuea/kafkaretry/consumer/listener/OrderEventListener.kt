package com.biuea.kafkaretry.consumer.listener

import com.biuea.kafkaretry.common.constant.RetryTopics
import com.biuea.kafkaretry.common.dto.OrderEvent
import com.biuea.kafkaretry.consumer.service.NotificationService
import com.biuea.kafkaretry.consumer.service.OrderProcessingService
import com.biuea.kafkaretry.consumer.service.OutboxService
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.stereotype.Component

@Component
class OrderEventListener(
    private val orderProcessingService: OrderProcessingService,
    private val outboxService: OutboxService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RetryableTopic(
        attempts = "4",
        backOff = BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlt"
    )
    @KafkaListener(
        topics = [RetryTopics.ORDER_TOPIC],
        groupId = RetryTopics.GROUP_ORDER,
        containerFactory = "orderKafkaListenerContainerFactory"
    )
    fun listen(record: ConsumerRecord<String, String>) {
        val orderEvent = parseOrderEvent(record.value())
        log.info("[RetryableTopic] Processing order: {} from topic: {}, partition: {}, offset: {}",
            orderEvent.id, record.topic(), record.partition(), record.offset())
        orderProcessingService.processOrder(orderEvent)
    }

    @DltHandler
    fun handleDlt(record: ConsumerRecord<String, String>) {
        log.error("[RetryableTopic DLT] Order sent to DLT after all retries exhausted. topic={}, partition={}, offset={}",
            record.topic(), record.partition(), record.offset())

        outboxService.saveFailedMessage(
            record,
            RuntimeException("All retries exhausted for order event")
        )
        notificationService.notifyFailure(
            record,
            RuntimeException("Order processing failed permanently")
        )
    }

    private fun parseOrderEvent(rawValue: String): OrderEvent {
        var jsonString = rawValue
        while (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
            jsonString = objectMapper.readValue(jsonString, String::class.java)
        }
        return objectMapper.readValue(jsonString, OrderEvent::class.java)
    }
}
