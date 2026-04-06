package com.closet.inventory.consumer

import com.closet.common.event.ClosetTopics
import com.closet.inventory.application.facade.InventoryFacade
import com.closet.inventory.consumer.event.OrderEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.order 토픽 Consumer.
 *
 * 주문 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - OrderCreated: 재고 예약
 * - OrderCancelled: 재고 해제
 *
 * FeatureToggle: Phase2FeatureKey.INVENTORY_KAFKA_ENABLED
 */
@Component
@ConditionalOnProperty(name = ["feature.inventory-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderEventConsumer(
    private val inventoryFacade: InventoryFacade,
) {
    @KafkaListener(topics = [ClosetTopics.ORDER], groupId = "inventory-service")
    fun handle(event: OrderEvent) {
        logger.info { "${ClosetTopics.ORDER} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "OrderCreated" -> inventoryFacade.handleOrderCreated(event.toCreatedEvent())
            "OrderCancelled" -> inventoryFacade.handleOrderCancelled(event.toCancelledEvent())
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
