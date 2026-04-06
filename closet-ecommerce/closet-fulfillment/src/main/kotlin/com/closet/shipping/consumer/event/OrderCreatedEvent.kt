package com.closet.shipping.consumer.event

/**
 * event.closet.order 토픽의 주문 생성 이벤트 페이로드.
 *
 * eventType: "OrderCreated"
 * 배송 준비 정보를 사전 저장하기 위한 정보를 담는다.
 */
data class OrderCreatedEvent(
    val orderId: Long,
    val memberId: Long,
    val sellerId: Long,
    val receiverName: String,
    val receiverPhone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
)
