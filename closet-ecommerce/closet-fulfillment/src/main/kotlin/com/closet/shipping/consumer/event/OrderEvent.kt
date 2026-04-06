package com.closet.shipping.consumer.event

/**
 * event.closet.order 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - OrderCreated: 배송 준비 정보 사전 저장
 */
data class OrderEvent(
    val eventType: String,
    val orderId: Long? = null,
    val memberId: Long? = null,
    val sellerId: Long? = null,
    val receiverName: String? = null,
    val receiverPhone: String? = null,
    val zipCode: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
) {
    fun toOrderCreatedEvent(): OrderCreatedEvent {
        return OrderCreatedEvent(
            orderId = orderId ?: 0L,
            memberId = memberId ?: 0L,
            sellerId = sellerId ?: 0L,
            receiverName = receiverName ?: "",
            receiverPhone = receiverPhone ?: "",
            zipCode = zipCode ?: "",
            address = address ?: "",
            detailAddress = detailAddress ?: "",
        )
    }
}
