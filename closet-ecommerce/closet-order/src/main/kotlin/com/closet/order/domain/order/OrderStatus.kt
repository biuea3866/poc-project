package com.closet.order.domain.order

enum class OrderStatus {
    PENDING,
    STOCK_RESERVED,
    PAID,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CONFIRMED,
    RETURN_REQUESTED,
    EXCHANGE_REQUESTED,
    CANCELLED,
    FAILED;

    fun canTransitionTo(target: OrderStatus): Boolean {
        return when (this) {
            PENDING -> target in setOf(STOCK_RESERVED, CANCELLED, FAILED)
            STOCK_RESERVED -> target in setOf(PAID, CANCELLED, FAILED)
            PAID -> target in setOf(PREPARING, CANCELLED)
            PREPARING -> target in setOf(SHIPPED, CANCELLED)
            SHIPPED -> target in setOf(DELIVERED)
            DELIVERED -> target in setOf(CONFIRMED, RETURN_REQUESTED, EXCHANGE_REQUESTED)
            CONFIRMED -> false
            RETURN_REQUESTED -> false
            EXCHANGE_REQUESTED -> false
            CANCELLED -> false
            FAILED -> false
        }
    }

    fun validateTransitionTo(target: OrderStatus) {
        require(canTransitionTo(target)) {
            "주문 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(CONFIRMED, CANCELLED, FAILED)
}
