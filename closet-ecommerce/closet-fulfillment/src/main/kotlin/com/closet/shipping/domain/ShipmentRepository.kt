package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime
import java.util.Optional

interface ShipmentRepository : JpaRepository<Shipment, Long> {
    fun findByOrderId(orderId: Long): Optional<Shipment>

    fun findByIdAndSellerIdIn(
        id: Long,
        sellerIds: Collection<Long>,
    ): Optional<Shipment>

    fun findByStatusIn(statuses: Collection<ShippingStatus>): List<Shipment>

    fun findByStatusAndDeliveredAtLessThanEqual(
        status: ShippingStatus,
        cutoff: ZonedDateTime,
    ): List<Shipment>
}
