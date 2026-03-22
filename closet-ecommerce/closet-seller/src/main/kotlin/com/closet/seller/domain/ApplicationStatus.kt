package com.closet.seller.domain

/**
 * 입점 신청 상태
 */
enum class ApplicationStatus {
    SUBMITTED, REVIEWING, APPROVED, REJECTED;

    fun canTransitionTo(target: ApplicationStatus): Boolean = when (this) {
        SUBMITTED -> target == REVIEWING
        REVIEWING -> target in listOf(APPROVED, REJECTED)
        APPROVED -> false
        REJECTED -> false
    }

    fun validateTransitionTo(target: ApplicationStatus) {
        require(canTransitionTo(target)) {
            "신청 상태를 ${this.name}에서 ${target.name}으로 변경할 수 없습니다"
        }
    }
}
