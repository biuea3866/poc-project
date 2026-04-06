package com.closet.cs.domain

/**
 * 문의 상태
 */
enum class InquiryStatus {
    OPEN,
    ANSWERED,
    CLOSED,
    ;

    fun canTransitionTo(target: InquiryStatus): Boolean =
        when (this) {
            OPEN -> target in listOf(ANSWERED, CLOSED)
            ANSWERED -> target == CLOSED
            CLOSED -> false
        }

    fun validateTransitionTo(target: InquiryStatus) {
        require(canTransitionTo(target)) {
            "문의 상태를 ${this.name}에서 ${target.name}으로 변경할 수 없습니다"
        }
    }
}
