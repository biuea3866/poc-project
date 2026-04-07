package com.closet.order.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.application.OrderService
import com.closet.order.consumer.event.InventoryEvent
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * event.closet.inventory 토픽 Consumer.
 *
 * 재고 부족(INSUFFICIENT) 이벤트를 수신하여 주문을 FAILED 처리한다.
 * eventType="InventoryInsufficient"만 처리하고, 나머지는 무시한다.
 */
@Component
class InventoryInsufficientConsumer(
    private val orderService: OrderService,
    private val idempotencyChecker: IdempotencyChecker,
) {
    companion object {
        private const val CONSUMER_GROUP = "order-service"
    }

    @KafkaListener(topics = [ClosetTopics.INVENTORY], groupId = CONSUMER_GROUP)
    @Transactional
    fun handle(event: InventoryEvent) {
        logger.info { "${ClosetTopics.INVENTORY} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "InventoryInsufficient" -> {
                val eventId = event.eventId ?: return
                val orderId = event.orderId ?: return

                logger.info { "InventoryInsufficient 수신: orderId=$orderId, reason=${event.reason}" }

                idempotencyChecker.process(eventId, ClosetTopics.INVENTORY, CONSUMER_GROUP) {
                    orderService.failByInventoryInsufficient(orderId, event.reason)
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
