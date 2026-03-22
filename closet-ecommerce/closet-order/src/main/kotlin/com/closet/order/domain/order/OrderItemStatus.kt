package com.closet.order.domain.order

enum class OrderItemStatus {
    ORDERED,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}
