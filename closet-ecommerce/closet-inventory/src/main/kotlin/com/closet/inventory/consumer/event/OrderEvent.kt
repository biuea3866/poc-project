package com.closet.inventory.consumer.event

/**
 * event.closet.order 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분하고, 각 타입별 필드를 포함한다.
 * - CREATED: orderId, memberId, items
 * - CANCELLED: orderId, reason, items
 */
data class OrderEvent(
    val eventType: String,
    val orderId: Long,
    val memberId: Long? = null,
    val reason: String? = null,
    val items: List<OrderEventItem> = emptyList(),
) {
    data class OrderEventItem(
        val productOptionId: Long,
        val quantity: Int,
    )

    fun toCreatedEvent(): OrderCreatedEvent {
        return OrderCreatedEvent(
            orderId = orderId,
            memberId = memberId ?: 0L,
            items = items.map {
                OrderCreatedEvent.OrderItemInfo(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            },
        )
    }

    fun toCancelledEvent(): OrderCancelledEvent {
        return OrderCancelledEvent(
            orderId = orderId,
            reason = reason ?: "",
            items = items.map {
                OrderCancelledEvent.OrderItemInfo(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            },
        )
    }
}
