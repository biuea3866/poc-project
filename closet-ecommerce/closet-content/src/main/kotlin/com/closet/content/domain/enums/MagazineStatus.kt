package com.closet.content.domain.enums

enum class MagazineStatus {
    DRAFT, PUBLISHED, ARCHIVED;

    fun canTransitionTo(target: MagazineStatus): Boolean = when (this) {
        DRAFT -> target == PUBLISHED
        PUBLISHED -> target == ARCHIVED
        ARCHIVED -> false
    }

    fun validateTransitionTo(target: MagazineStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
