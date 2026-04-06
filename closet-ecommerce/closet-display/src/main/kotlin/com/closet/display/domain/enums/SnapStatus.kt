package com.closet.display.domain.enums

enum class SnapStatus {
    ACTIVE,
    HIDDEN,
    REPORTED,
    ;

    fun canTransitionTo(target: SnapStatus): Boolean {
        return when (this) {
            ACTIVE -> target in setOf(HIDDEN, REPORTED)
            HIDDEN -> target == ACTIVE
            REPORTED -> target in setOf(HIDDEN, ACTIVE)
        }
    }

    fun validateTransitionTo(target: SnapStatus) {
        require(canTransitionTo(target)) {
            "스냅 상태를 ${this.name}에서 ${target.name}(으)로 변경할 수 없습니다"
        }
    }
}
