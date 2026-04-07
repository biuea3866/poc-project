package com.closet.order.domain.order

enum class OrderItemStatus {
    ORDERED,
    PREPARING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    EXCHANGE_REQUESTED,
    EXCHANGE_COMPLETED,
    ;

    fun canTransitionTo(target: OrderItemStatus): Boolean {
        return when (this) {
            ORDERED -> target in setOf(PREPARING, CANCELLED)
            PREPARING -> target in setOf(SHIPPED, CANCELLED)
            SHIPPED -> target in setOf(DELIVERED)
            DELIVERED -> target in setOf(RETURN_REQUESTED, EXCHANGE_REQUESTED)
            CANCELLED -> false
            RETURN_REQUESTED -> target in setOf(RETURNED)
            RETURNED -> false
            EXCHANGE_REQUESTED -> target in setOf(EXCHANGE_COMPLETED)
            EXCHANGE_COMPLETED -> false
        }
    }

    fun validateTransitionTo(target: OrderItemStatus) {
        require(canTransitionTo(target)) {
            "주문 항목 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }
}
