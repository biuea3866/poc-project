package com.closet.order.consumer.event

/**
 * event.closet.shipping 토픽의 배송 상태 변경 이벤트 페이로드.
 *
 * eventType: "ShippingStatusChanged"
 * 배송 상태 변경 시 주문 상태를 동기화하기 위한 정보를 담는다.
 */
data class ShippingStatusChangedEvent(
    val eventId: String,
    val orderId: Long,
    val shippingStatus: String,
)
