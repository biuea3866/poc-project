package com.closet.settlement.domain.settlement

enum class SettlementStatus {
    PENDING,
    CALCULATED,
    CONFIRMED,
    PAID;

    fun canTransitionTo(target: SettlementStatus): Boolean {
        return when (this) {
            PENDING -> target == CALCULATED
            CALCULATED -> target == CONFIRMED
            CONFIRMED -> target == PAID
            PAID -> false
        }
    }

    fun validateTransitionTo(target: SettlementStatus) {
        require(canTransitionTo(target)) {
            "정산 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this == PAID
}
