package com.closet.inventory.consumer.event

/**
 * event.closet.order 토픽의 주문 생성 이벤트 페이로드.
 *
 * eventType: "CREATED"
 */
data class OrderCreatedEvent(
    val orderId: Long,
    val memberId: Long,
    val items: List<OrderItemInfo>,
) {
    data class OrderItemInfo(
        val productOptionId: Long,
        val quantity: Int,
    )
}
