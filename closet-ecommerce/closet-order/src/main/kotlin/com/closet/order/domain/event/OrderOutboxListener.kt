package com.closet.order.domain.event

import com.closet.common.event.ClosetTopics
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
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderCreated",
            topic = ClosetTopics.ORDER,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=${ClosetTopics.ORDER}, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCancelled(event: OrderCancelledEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderCancelled",
            topic = ClosetTopics.ORDER,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=${ClosetTopics.ORDER}, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderPaid(event: OrderPaidEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderPaid",
            topic = ClosetTopics.ORDER,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=${ClosetTopics.ORDER}, orderId=${event.orderId}" }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderConfirmed(event: OrderConfirmedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxEventPublisher.publish(
            aggregateType = AGGREGATE_TYPE,
            aggregateId = event.orderId.toString(),
            eventType = "OrderConfirmed",
            topic = ClosetTopics.ORDER,
            partitionKey = event.orderId.toString(),
            payload = payload,
        )
        logger.info { "Outbox 이벤트 저장: topic=${ClosetTopics.ORDER}, orderId=${event.orderId}" }
    }
}
