package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ShippingFeePolicyRepository : JpaRepository<ShippingFeePolicy, Long> {
    fun findByTypeAndReasonAndIsActiveTrue(
        type: String,
        reason: String,
    ): Optional<ShippingFeePolicy>
}
