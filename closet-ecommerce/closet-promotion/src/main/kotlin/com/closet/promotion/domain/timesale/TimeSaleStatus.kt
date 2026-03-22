package com.closet.promotion.domain.timesale

enum class TimeSaleStatus {
    SCHEDULED,
    ACTIVE,
    ENDED,
    EXHAUSTED;

    fun canTransitionTo(target: TimeSaleStatus): Boolean {
        return when (this) {
            SCHEDULED -> target in setOf(ACTIVE)
            ACTIVE -> target in setOf(ENDED, EXHAUSTED)
            ENDED -> false
            EXHAUSTED -> false
        }
    }

    fun validateTransitionTo(target: TimeSaleStatus) {
        require(canTransitionTo(target)) {
            "타임세일 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(ENDED, EXHAUSTED)
}
