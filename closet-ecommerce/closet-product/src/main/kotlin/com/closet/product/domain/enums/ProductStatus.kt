package com.closet.product.domain.enums

enum class ProductStatus {
    DRAFT,
    ACTIVE,
    SOLD_OUT,
    INACTIVE,
    ;

    fun canTransitionTo(target: ProductStatus): Boolean =
        when (this) {
            DRAFT -> target == ACTIVE
            ACTIVE -> target in listOf(SOLD_OUT, INACTIVE)
            SOLD_OUT -> target == ACTIVE
            INACTIVE -> target == ACTIVE
        }

    fun validateTransitionTo(target: ProductStatus) {
        require(canTransitionTo(target)) {
            "Cannot transition from $this to $target"
        }
    }
}
