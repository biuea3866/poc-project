package com.closet.display.domain.enums

enum class ExhibitionStatus {
    DRAFT,
    ACTIVE,
    ENDED,
    ;

    fun canTransitionTo(target: ExhibitionStatus): Boolean =
        when (this) {
            DRAFT -> target == ACTIVE
            ACTIVE -> target == ENDED
            ENDED -> false
        }

    fun validateTransitionTo(target: ExhibitionStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
