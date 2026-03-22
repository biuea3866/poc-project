package com.closet.shipping.repository

import com.closet.shipping.domain.ShipmentStatusHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentStatusHistoryRepository : JpaRepository<ShipmentStatusHistory, Long> {
    fun findByShipmentIdOrderByCreatedAtDesc(shipmentId: Long): List<ShipmentStatusHistory>
}
