package com.closet.inventory.application.facade

import com.closet.common.event.ClosetTopics
import com.closet.common.idempotency.IdempotencyChecker
import com.closet.inventory.application.InventoryService
import com.closet.inventory.application.ReleaseItemRequest
import com.closet.inventory.application.ReserveItemRequest
import com.closet.inventory.consumer.event.OrderCancelledEvent
import com.closet.inventory.consumer.event.OrderCreatedEvent
import com.closet.inventory.consumer.event.ReturnApprovedEvent
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 재고 Facade.
 *
 * Kafka Consumer에서 호출되는 진입점. 멱등성 보장, Service 호출 등의
 * 오케스트레이션을 담당한다.
 */
@Component
class InventoryFacade(
    private val inventoryService: InventoryService,
    private val idempotencyChecker: IdempotencyChecker,
) {

    companion object {
        private const val CONSUMER_GROUP = "inventory-service"
    }

    /**
     * 주문 생성 이벤트 처리 -> 재고 예약.
     */
    fun handleOrderCreated(event: OrderCreatedEvent) {
        val eventId = "order-created-${event.orderId}"
        logger.info { "주문 생성 이벤트 처리 시작: orderId=${event.orderId}, items=${event.items.size}" }

        idempotencyChecker.process(eventId, ClosetTopics.ORDER, CONSUMER_GROUP) {
            val reserveItems = event.items.map {
                ReserveItemRequest(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            }

            inventoryService.reserveAll(event.orderId, reserveItems)
        }
    }

    /**
     * 주문 취소 이벤트 처리 -> 재고 해제.
     */
    fun handleOrderCancelled(event: OrderCancelledEvent) {
        val eventId = "order-cancelled-${event.orderId}"
        logger.info { "주문 취소 이벤트 처리 시작: orderId=${event.orderId}, reason=${event.reason}" }

        idempotencyChecker.process(eventId, ClosetTopics.ORDER, CONSUMER_GROUP) {
            val releaseItems = event.items.map {
                ReleaseItemRequest(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            }

            inventoryService.releaseAll(event.orderId, releaseItems, event.reason)
        }
    }

    /**
     * 반품 승인 이벤트 처리 -> 재고 양품 복구.
     */
    fun handleReturnApproved(event: ReturnApprovedEvent) {
        val eventId = "return-approved-${event.orderId}"
        logger.info { "반품 승인 이벤트 처리 시작: orderId=${event.orderId}, items=${event.items.size}" }

        idempotencyChecker.process(eventId, ClosetTopics.SHIPPING, CONSUMER_GROUP) {
            for (item in event.items) {
                inventoryService.returnRestore(
                    productOptionId = item.productOptionId,
                    quantity = item.quantity,
                    orderId = event.orderId,
                )
            }
        }
    }
}
