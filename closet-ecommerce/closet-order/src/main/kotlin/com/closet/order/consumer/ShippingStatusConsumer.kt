package com.closet.order.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.application.OrderService
import com.closet.order.consumer.event.ShippingEvent
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * event.closet.shipping 토픽 Consumer.
 *
 * 배송 상태 변경(ShippingStatusChanged) 이벤트를 수신하여 주문 상태를 동기화한다.
 * eventType="ShippingStatusChanged"만 처리하고, 나머지는 무시한다.
 */
@Component
class ShippingStatusConsumer(
    private val orderService: OrderService,
    private val idempotencyChecker: IdempotencyChecker,
) {
    companion object {
        private const val CONSUMER_GROUP = "order-service"
    }

    @KafkaListener(topics = [ClosetTopics.SHIPPING], groupId = CONSUMER_GROUP)
    @Transactional
    fun handle(event: ShippingEvent) {
        logger.info { "${ClosetTopics.SHIPPING} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "ShippingStatusChanged" -> {
                val eventId = event.eventId ?: return
                val orderId = event.orderId ?: return
                val shippingStatus = event.shippingStatus ?: return

                logger.info { "ShippingStatusChanged 수신: orderId=$orderId, shippingStatus=$shippingStatus" }

                idempotencyChecker.process(eventId, ClosetTopics.SHIPPING, CONSUMER_GROUP) {
                    orderService.syncStatusFromShipping(orderId, shippingStatus)
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
