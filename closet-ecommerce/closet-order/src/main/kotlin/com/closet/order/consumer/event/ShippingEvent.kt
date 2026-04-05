package com.closet.order.consumer.event

/**
 * event.closet.shipping 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - ShippingStatusChanged: 배송 상태 변경 시 주문 상태 동기화
 */
data class ShippingEvent(
    val eventType: String,
    val eventId: String? = null,
    val orderId: Long? = null,
    val shippingStatus: String? = null,
) {

    fun toStatusChangedEvent(): ShippingStatusChangedEvent {
        return ShippingStatusChangedEvent(
            eventId = eventId ?: "",
            orderId = orderId ?: 0L,
            shippingStatus = shippingStatus ?: "",
        )
    }
}
