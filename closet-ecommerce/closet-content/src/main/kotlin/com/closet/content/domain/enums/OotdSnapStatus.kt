package com.closet.content.domain.enums

enum class OotdSnapStatus {
    ACTIVE, HIDDEN, DELETED;

    fun canTransitionTo(target: OotdSnapStatus): Boolean = when (this) {
        ACTIVE -> target in listOf(HIDDEN, DELETED)
        HIDDEN -> target in listOf(ACTIVE, DELETED)
        DELETED -> false
    }

    fun validateTransitionTo(target: OotdSnapStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
