package com.closet.shipping.domain

/**
 * 반품 상태 enum.
 *
 * REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> INSPECTING -> APPROVED/REJECTED
 * APPROVED -> COMPLETED (환불 완료)
 * PD-13: 3영업일 검수, 초과시 자동 승인
 */
enum class ReturnStatus {
    REQUESTED,
    PICKUP_SCHEDULED,
    PICKUP_COMPLETED,
    INSPECTING,
    APPROVED,
    REJECTED,
    COMPLETED;

    fun canTransitionTo(target: ReturnStatus): Boolean {
        return when (this) {
            REQUESTED -> target in setOf(PICKUP_SCHEDULED, REJECTED)
            PICKUP_SCHEDULED -> target in setOf(PICKUP_COMPLETED, REJECTED)
            PICKUP_COMPLETED -> target == INSPECTING
            INSPECTING -> target in setOf(APPROVED, REJECTED)
            APPROVED -> target == COMPLETED
            REJECTED -> false
            COMPLETED -> false
        }
    }

    fun validateTransitionTo(target: ReturnStatus) {
        require(canTransitionTo(target)) {
            "반품 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(COMPLETED, REJECTED)
}
