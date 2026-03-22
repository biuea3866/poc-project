package com.closet.promotion.domain.coupon

enum class CouponStatus {
    ACTIVE,
    EXPIRED,
    EXHAUSTED;

    fun canTransitionTo(target: CouponStatus): Boolean {
        return when (this) {
            ACTIVE -> target in setOf(EXPIRED, EXHAUSTED)
            EXPIRED -> false
            EXHAUSTED -> false
        }
    }

    fun validateTransitionTo(target: CouponStatus) {
        require(canTransitionTo(target)) {
            "쿠폰 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(EXPIRED, EXHAUSTED)
}
