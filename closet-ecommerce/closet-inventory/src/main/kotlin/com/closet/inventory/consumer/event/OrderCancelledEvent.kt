package com.closet.inventory.consumer.event

/**
 * event.closet.order 토픽의 주문 취소 이벤트 페이로드.
 *
 * eventType: "CANCELLED"
 */
data class OrderCancelledEvent(
    val orderId: Long,
    val reason: String,
    val items: List<OrderItemInfo>,
) {
    data class OrderItemInfo(
        val productOptionId: Long,
        val quantity: Int,
    )
}
