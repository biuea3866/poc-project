package com.closet.order.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.consumer.event.ShippingEvent
import com.closet.order.domain.order.OrderStatus
import com.closet.order.domain.order.OrderStatusHistory
import com.closet.order.repository.OrderRepository
import com.closet.order.repository.OrderStatusHistoryRepository
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
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
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
                    val order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                    if (order == null) {
                        logger.warn { "주문을 찾을 수 없습니다. orderId=$orderId" }
                        return@process
                    }

                    if (order.status.isTerminal()) {
                        logger.info { "터미널 상태 주문은 무시합니다. orderId=$orderId, status=${order.status}" }
                        return@process
                    }

                    val targetStatus = mapShippingStatusToOrderStatus(shippingStatus)
                    if (targetStatus == null) {
                        logger.warn { "알 수 없는 배송 상태: $shippingStatus" }
                        return@process
                    }

                    if (!order.status.canTransitionTo(targetStatus)) {
                        logger.warn { "잘못된 상태 전이 시도: ${order.status} -> $targetStatus, orderId=$orderId" }
                        return@process
                    }

                    val previousStatus = order.status
                    when (targetStatus) {
                        OrderStatus.PREPARING -> order.prepare()
                        OrderStatus.SHIPPED -> order.ship()
                        OrderStatus.DELIVERED -> order.deliver()
                        else -> {
                            logger.warn { "처리할 수 없는 대상 상태: $targetStatus" }
                            return@process
                        }
                    }

                    orderStatusHistoryRepository.save(
                        OrderStatusHistory.create(
                            orderId = order.id,
                            fromStatus = previousStatus,
                            toStatus = order.status,
                            changedBy = "shipping-service",
                        )
                    )

                    logger.info { "주문 상태 동기화 완료: orderId=$orderId, ${previousStatus} -> ${order.status}" }
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }

    private fun mapShippingStatusToOrderStatus(shippingStatus: String): OrderStatus? {
        return when (shippingStatus) {
            "READY" -> OrderStatus.PREPARING
            "IN_TRANSIT" -> OrderStatus.SHIPPED
            "DELIVERED" -> OrderStatus.DELIVERED
            else -> null
        }
    }
}
