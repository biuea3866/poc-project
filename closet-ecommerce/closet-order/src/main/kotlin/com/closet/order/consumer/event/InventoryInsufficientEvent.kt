package com.closet.order.consumer.event

/**
 * event.closet.inventory 토픽의 재고 부족 이벤트 페이로드.
 *
 * eventType: "InventoryInsufficient"
 * 재고 부족 시 주문을 FAILED 처리하기 위한 정보를 담는다.
 */
data class InventoryInsufficientEvent(
    val eventId: String,
    val orderId: Long,
    val reason: String,
)
