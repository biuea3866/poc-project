package com.example.filepractice.poc.domain.entity

import java.time.LocalDateTime

data class Shipment(
    val id: Int,
    val orderId: Int,
    val trackingNumber: String,
    val carrier: String,
    val status: ShipmentStatus,
    val shippedAt: LocalDateTime?,
    val estimatedDeliveryAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val recipientName: String,
    val recipientPhone: String,
    val recipientAddress: String
)

enum class ShipmentStatus {
    PREPARING,
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED
}
