package com.closet.inventory.consumer.event

/**
 * event.closet.shipping 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - RETURN_APPROVED: 반품 승인 시 재고 복구
 */
data class ShippingEvent(
    val eventType: String,
    val orderId: Long,
    val items: List<ShippingEventItem> = emptyList(),
) {
    data class ShippingEventItem(
        val productOptionId: Long,
        val quantity: Int,
    )

    fun toReturnApprovedEvent(): ReturnApprovedEvent {
        return ReturnApprovedEvent(
            orderId = orderId,
            items = items.map {
                ReturnApprovedEvent.ReturnItemInfo(
                    productOptionId = it.productOptionId,
                    quantity = it.quantity,
                )
            },
        )
    }
}
