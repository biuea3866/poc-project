package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository

interface ShippingTrackingLogRepository : JpaRepository<ShippingTrackingLog, Long> {
    fun findByShippingIdOrderByTrackedAtAsc(shippingId: Long): List<ShippingTrackingLog>
}
