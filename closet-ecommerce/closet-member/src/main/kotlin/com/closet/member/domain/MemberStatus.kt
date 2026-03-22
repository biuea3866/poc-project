package com.closet.member.domain

/**
 * 회원 상태
 */
enum class MemberStatus {
    ACTIVE, INACTIVE, WITHDRAWN;

    fun canTransitionTo(target: MemberStatus): Boolean = when (this) {
        ACTIVE -> target in listOf(INACTIVE, WITHDRAWN)
        INACTIVE -> target == ACTIVE
        WITHDRAWN -> false
    }

    fun validateTransitionTo(target: MemberStatus) {
        require(canTransitionTo(target)) {
            "회원 상태를 ${this.name}에서 ${target.name}으로 변경할 수 없습니다"
        }
    }
}
