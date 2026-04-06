package com.closet.shipping.domain

/**
 * 배송 상태 enum.
 *
 * PRD 3단계 + Mock 서버 매핑 (PD-10).
 * - READY: 송장 등록 완료, 택배사 인수 대기 (Mock: ACCEPTED)
 * - IN_TRANSIT: 배송 중 (Mock: IN_TRANSIT, OUT_FOR_DELIVERY)
 * - DELIVERED: 배송 완료 (Mock: DELIVERED)
 */
enum class ShippingStatus {
    READY,
    IN_TRANSIT,
    DELIVERED,
    ;

    fun canTransitionTo(target: ShippingStatus): Boolean {
        return when (this) {
            READY -> target == IN_TRANSIT
            IN_TRANSIT -> target == DELIVERED
            DELIVERED -> false
        }
    }

    fun validateTransitionTo(target: ShippingStatus) {
        require(canTransitionTo(target)) {
            "배송 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    companion object {
        /**
         * Mock 택배사 상태 -> ShippingStatus 매핑.
         * carrierStatus: ACCEPTED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
         */
        fun fromCarrierStatus(carrierStatus: String): ShippingStatus {
            return when (carrierStatus) {
                "ACCEPTED" -> READY
                "IN_TRANSIT", "OUT_FOR_DELIVERY" -> IN_TRANSIT
                "DELIVERED" -> DELIVERED
                else -> throw IllegalArgumentException("Unknown carrier status: $carrierStatus")
            }
        }
    }
}
