package com.closet.order.domain.event

import com.closet.common.outbox.OutboxEventPublisher
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val logger = KotlinLogging.logger {}

@Component
class OrderOutboxListener(
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private const val AGGREGATE_TYPE = "Order"
        private const val TOPIC_ORDER_CREATED = "order.created"
        private const val TOPIC_ORDER_CANCELLED = "order.cancelled"
        private const val TOPIC_ORDER_PAID = "order.paid"
        private const val TOPIC_ORDER_CONFIRMED = "order.confirmed"
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderCreated",
            topic = TOPIC_ORDER_CREATED,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=$TOPIC_ORDER_CREATED, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCancelled(event: OrderCancelledEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderCancelled",
            topic = TOPIC_ORDER_CANCELLED,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=$TOPIC_ORDER_CANCELLED, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderPaid(event: OrderPaidEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderPaid",
            topic = TOPIC_ORDER_PAID,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=$TOPIC_ORDER_PAID, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderConfirmed(event: OrderConfirmedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderConfirmed",
            topic = TOPIC_ORDER_CONFIRMED,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=$TOPIC_ORDER_CONFIRMED, orderId=${event.orderId}" }
    }
}
