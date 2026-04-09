package com.closet.order.consumer

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.order.consumer.event.InventoryEvent
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
 * event.closet.inventory 토픽 Consumer.
 *
 * 재고 부족(INSUFFICIENT) 이벤트를 수신하여 주문을 FAILED 처리한다.
 * eventType="InventoryInsufficient"만 처리하고, 나머지는 무시한다.
 */
@Component
class InventoryInsufficientConsumer(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
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
                    val order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                    if (order == null) {
                        logger.warn { "주문을 찾을 수 없습니다. orderId=$orderId" }
                        return@process
                    }

                    if (order.status.isTerminal()) {
                        logger.info { "터미널 상태 주문은 무시합니다. orderId=$orderId, status=${order.status}" }
                        return@process
                    }

                    if (order.status != OrderStatus.STOCK_RESERVED) {
                        logger.warn { "STOCK_RESERVED 상태가 아닌 주문에 재고 부족 이벤트 수신: orderId=$orderId, status=${order.status}" }
                        return@process
                    }

                    val previousStatus = order.status
                    order.fail()

                    orderStatusHistoryRepository.save(
                        OrderStatusHistory.create(
                            orderId = order.id,
                            fromStatus = previousStatus,
                            toStatus = order.status,
                            reason = event.reason,
                            changedBy = "inventory-service",
                        ),
                    )

                    logger.info { "주문 FAILED 처리 완료: orderId=$orderId, reason=${event.reason}" }
                }
            }
            else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
        }
    }
}
