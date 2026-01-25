package com.biuea.springdata.kafka.service

import com.biuea.springdata.kafka.dto.Event
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class EventProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    fun process(event: Event) {
        // Simulate processing
        log.trace("Processing event: id={}, type={}", event.id, event.type)

        // Add your business logic here
        when (event.type) {
            com.biuea.springdata.kafka.dto.EventType.USER_CREATED -> handleUserCreated(event)
            com.biuea.springdata.kafka.dto.EventType.USER_UPDATED -> handleUserUpdated(event)
            com.biuea.springdata.kafka.dto.EventType.USER_DELETED -> handleUserDeleted(event)
            com.biuea.springdata.kafka.dto.EventType.ORDER_PLACED -> handleOrderPlaced(event)
            com.biuea.springdata.kafka.dto.EventType.ORDER_SHIPPED -> handleOrderShipped(event)
            com.biuea.springdata.kafka.dto.EventType.PAYMENT_PROCESSED -> handlePaymentProcessed(event)
            com.biuea.springdata.kafka.dto.EventType.NOTIFICATION_SENT -> handleNotificationSent(event)
        }
    }

    fun processBatch(events: List<Event>) {
        log.trace("Processing batch of {} events", events.size)
        events.forEach { process(it) }
    }

    private fun handleUserCreated(event: Event) {
        log.trace("Handling USER_CREATED: {}", event.id)
    }

    private fun handleUserUpdated(event: Event) {
        log.trace("Handling USER_UPDATED: {}", event.id)
    }

    private fun handleUserDeleted(event: Event) {
        log.trace("Handling USER_DELETED: {}", event.id)
    }

    private fun handleOrderPlaced(event: Event) {
        log.trace("Handling ORDER_PLACED: {}", event.id)
    }

    private fun handleOrderShipped(event: Event) {
        log.trace("Handling ORDER_SHIPPED: {}", event.id)
    }

    private fun handlePaymentProcessed(event: Event) {
        log.trace("Handling PAYMENT_PROCESSED: {}", event.id)
    }

    private fun handleNotificationSent(event: Event) {
        log.trace("Handling NOTIFICATION_SENT: {}", event.id)
    }
}
