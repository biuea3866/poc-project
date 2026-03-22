package com.closet.seller.domain

/**
 * 셀러 상태
 */
enum class SellerStatus {
    PENDING, ACTIVE, SUSPENDED, WITHDRAWN;

    fun canTransitionTo(target: SellerStatus): Boolean = when (this) {
        PENDING -> target == ACTIVE
        ACTIVE -> target in listOf(SUSPENDED, WITHDRAWN)
        SUSPENDED -> target in listOf(ACTIVE, WITHDRAWN)
        WITHDRAWN -> false
    }

    fun validateTransitionTo(target: SellerStatus) {
        require(canTransitionTo(target)) {
            "셀러 상태를 ${this.name}에서 ${target.name}으로 변경할 수 없습니다"
        }
    }
}
