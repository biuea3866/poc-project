package com.closet.order.consumer.event

/**
 * event.closet.inventory 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - InventoryInsufficient: 재고 부족 시 주문 FAILED 처리
 */
data class InventoryEvent(
    val eventType: String,
    val eventId: String? = null,
    val orderId: Long? = null,
    val reason: String = "재고 부족",
) {

    fun toInsufficientEvent(): InventoryInsufficientEvent {
        return InventoryInsufficientEvent(
            eventId = eventId ?: "",
            orderId = orderId ?: 0L,
            reason = reason,
        )
    }
}
