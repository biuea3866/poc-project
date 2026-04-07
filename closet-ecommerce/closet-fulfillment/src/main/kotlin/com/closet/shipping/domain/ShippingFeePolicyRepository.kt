package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ShippingFeePolicyRepository : JpaRepository<ShippingFeePolicy, Long> {
    fun findByTypeAndReasonAndIsActiveTrue(
        type: String,
        reason: String,
    ): ShippingFeePolicy?
}
