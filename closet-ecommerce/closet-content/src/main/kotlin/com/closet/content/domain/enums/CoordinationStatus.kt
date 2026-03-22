package com.closet.content.domain.enums

enum class CoordinationStatus {
    ACTIVE, INACTIVE;

    fun canTransitionTo(target: CoordinationStatus): Boolean = when (this) {
        ACTIVE -> target == INACTIVE
        INACTIVE -> target == ACTIVE
    }

    fun validateTransitionTo(target: CoordinationStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
