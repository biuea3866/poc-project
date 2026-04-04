package com.closet.shipping.domain

/**
 * 교환 상태 enum (CP-28).
 *
 * REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> RESHIPPING -> COMPLETED
 * PD-14: 동일 가격 옵션만 교환 허용
 */
enum class ExchangeStatus {
    REQUESTED,
    PICKUP_SCHEDULED,
    PICKUP_COMPLETED,
    RESHIPPING,
    COMPLETED,
    REJECTED;

    fun canTransitionTo(target: ExchangeStatus): Boolean {
        return when (this) {
            REQUESTED -> target in setOf(PICKUP_SCHEDULED, REJECTED)
            PICKUP_SCHEDULED -> target in setOf(PICKUP_COMPLETED, REJECTED)
            PICKUP_COMPLETED -> target == RESHIPPING
            RESHIPPING -> target == COMPLETED
            COMPLETED -> false
            REJECTED -> false
        }
    }

    fun validateTransitionTo(target: ExchangeStatus) {
        require(canTransitionTo(target)) {
            "교환 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(COMPLETED, REJECTED)
}
