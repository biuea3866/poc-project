package com.closet.common.outbox

enum class OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED;

    fun canTransitionTo(target: OutboxEventStatus): Boolean {
        return when (this) {
            PENDING -> target == PUBLISHED || target == FAILED
            FAILED -> target == PUBLISHED
            PUBLISHED -> false
        }
    }

    fun validateTransitionTo(target: OutboxEventStatus) {
        require(canTransitionTo(target)) {
            "Outbox 이벤트 상태를 $this 에서 $target 로 변경할 수 없습니다"
        }
    }
}
