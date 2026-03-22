package com.closet.shipping.repository

import com.closet.shipping.domain.Shipment
import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentRepository : JpaRepository<Shipment, Long> {
    fun findByOrderId(orderId: Long): List<Shipment>
}
