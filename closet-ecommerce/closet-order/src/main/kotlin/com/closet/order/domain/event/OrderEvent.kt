package com.closet.order.domain.event

import com.closet.common.vo.Money

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

data class OrderPaidEvent(
    val orderId: Long,
    val paymentAmount: Money,
)

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
