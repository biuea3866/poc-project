package com.closet.shipping.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.shipping.application.PrepareShipmentRequest
import com.closet.shipping.application.ShippingService
import com.closet.shipping.consumer.event.OrderEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.order 토픽 Consumer.
 *
 * 주문 생성(OrderCreated) 이벤트를 수신하여 배송 준비 정보를 사전 저장한다.
 * eventType="OrderCreated"만 처리하고, 나머지는 무시한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.shipping-kafka-enabled"], havingValue = "true", matchIfMissing = true)
class OrderCreatedConsumer(
    private val shippingService: ShippingService,
    private val idempotencyChecker: IdempotencyChecker,
) {

    companion object {
        private const val CONSUMER_GROUP = "shipping-service"
    }

    @KafkaListener(topics = [ClosetTopics.ORDER], groupId = CONSUMER_GROUP)
    fun handle(event: OrderEvent) {
        logger.info { "${ClosetTopics.ORDER} 수신: eventType=${event.eventType}, orderId=${event.orderId}" }

        when (event.eventType) {
            "OrderCreated" -> {
                val orderCreated = event.toOrderCreatedEvent()
                val eventId = "shipping-order-created-${orderCreated.orderId}"

                idempotencyChecker.process(eventId, ClosetTopics.ORDER, CONSUMER_GROUP) {
                    shippingService.prepareShipment(
                        PrepareShipmentRequest(
                            orderId = orderCreated.orderId,
                            sellerId = orderCreated.sellerId,
                            memberId = orderCreated.memberId,
                            receiverName = orderCreated.receiverName,
                            receiverPhone = orderCreated.receiverPhone,
                            zipCode = orderCreated.zipCode,
                            address = orderCreated.address,
                            detailAddress = orderCreated.detailAddress,
                        )
                    )
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
