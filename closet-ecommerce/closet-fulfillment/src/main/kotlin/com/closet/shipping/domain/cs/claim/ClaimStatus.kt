package com.closet.shipping.domain.cs.claim

enum class ClaimStatus {
    REQUESTED,
    APPROVED,
    COMPLETED,
    REJECTED,
    ;

    fun canTransitionTo(target: ClaimStatus): Boolean {
        return when (this) {
            REQUESTED -> target in setOf(APPROVED, REJECTED)
            APPROVED -> target == COMPLETED
            COMPLETED -> false
            REJECTED -> false
        }
    }

    fun validateTransitionTo(target: ClaimStatus) {
        require(canTransitionTo(target)) {
            "클레임 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this in setOf(COMPLETED, REJECTED)
}
