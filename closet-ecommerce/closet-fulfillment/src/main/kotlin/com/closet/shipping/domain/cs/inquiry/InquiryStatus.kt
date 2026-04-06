package com.closet.shipping.domain.cs.inquiry

/**
 * 문의 상태 enum.
 *
 * PENDING: 접수 대기 (작성 직후)
 * IN_PROGRESS: 처리 중 (관리자가 확인)
 * ANSWERED: 답변 완료
 * CLOSED: 종료
 *
 * 상태 전이:
 * PENDING -> IN_PROGRESS, ANSWERED, CLOSED
 * IN_PROGRESS -> ANSWERED, CLOSED
 * ANSWERED -> IN_PROGRESS (추가 문의), CLOSED
 * CLOSED -> (터미널)
 */
enum class InquiryStatus {
    PENDING,
    IN_PROGRESS,
    ANSWERED,
    CLOSED,
    ;

    fun canTransitionTo(target: InquiryStatus): Boolean {
        return when (this) {
            PENDING -> target in setOf(IN_PROGRESS, ANSWERED, CLOSED)
            IN_PROGRESS -> target in setOf(ANSWERED, CLOSED)
            ANSWERED -> target in setOf(IN_PROGRESS, CLOSED)
            CLOSED -> false
        }
    }

    fun validateTransitionTo(target: InquiryStatus) {
        require(canTransitionTo(target)) {
            "문의 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }

    fun isTerminal(): Boolean = this == CLOSED
}
