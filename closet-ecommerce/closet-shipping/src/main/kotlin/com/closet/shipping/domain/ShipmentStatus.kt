package com.closet.shipping.domain

enum class ShipmentStatus {
    PENDING,
    READY,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED;

    fun canTransitionTo(target: ShipmentStatus): Boolean {
        return when (this) {
            PENDING -> target in setOf(READY)
            READY -> target in setOf(PICKED_UP)
            PICKED_UP -> target in setOf(IN_TRANSIT)
            IN_TRANSIT -> target in setOf(OUT_FOR_DELIVERY)
            OUT_FOR_DELIVERY -> target in setOf(DELIVERED)
            DELIVERED -> false
        }
    }

    fun validateTransitionTo(target: ShipmentStatus) {
        require(canTransitionTo(target)) {
            "배송 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this == DELIVERED
}
