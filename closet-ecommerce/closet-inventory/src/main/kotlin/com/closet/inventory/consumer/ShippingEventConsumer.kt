package com.closet.inventory.consumer

import com.closet.common.event.ClosetTopics
import com.closet.inventory.application.facade.InventoryFacade
import com.closet.inventory.consumer.event.ShippingEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.shipping 토픽 Consumer.
 *
 * 배송 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ReturnApproved: 재고 양품 복구
 *
 * FeatureToggle: Phase2FeatureKey.INVENTORY_KAFKA_ENABLED
 */
@Component
@ConditionalOnProperty(name = ["feature.inventory-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class ShippingEventConsumer(
    private val inventoryFacade: InventoryFacade,
) {
    @KafkaListener(topics = [ClosetTopics.SHIPPING], groupId = "inventory-service")
    fun handle(event: ShippingEvent) {
        logger.info { "${ClosetTopics.SHIPPING} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "ReturnApproved" -> inventoryFacade.handleReturnApproved(event.toReturnApprovedEvent())
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
