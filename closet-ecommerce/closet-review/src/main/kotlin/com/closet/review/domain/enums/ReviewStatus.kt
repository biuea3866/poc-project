package com.closet.review.domain.enums

enum class ReviewStatus {
    ACTIVE,
    HIDDEN,
    DELETED;

    fun canTransitionTo(target: ReviewStatus): Boolean = when (this) {
        ACTIVE -> target in listOf(HIDDEN, DELETED)
        HIDDEN -> target in listOf(ACTIVE, DELETED)
        DELETED -> false
    }

    fun validateTransitionTo(target: ReviewStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
