package com.closet.shipping.presentation.dto

import com.closet.shipping.domain.Carrier
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShipmentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateShipmentRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val sellerId: Long,
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val address: String,
    val carrier: Carrier? = null,
    val trackingNumber: String? = null,
)

data class RegisterTrackingRequest(
    @field:NotNull val carrier: Carrier,
    @field:NotBlank val trackingNumber: String,
)

data class UpdateShipmentStatusRequest(
    @field:NotNull val status: ShipmentStatus,
)

data class ShipmentResponse(
    val id: Long,
    val orderId: Long,
    val sellerId: Long,
    val carrier: Carrier?,
    val trackingNumber: String?,
    val status: ShipmentStatus,
    val receiverName: String,
    val receiverPhone: String,
    val address: String,
    val shippedAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(shipment: Shipment): ShipmentResponse {
            return ShipmentResponse(
                id = shipment.id,
                orderId = shipment.orderId,
                sellerId = shipment.sellerId,
                carrier = shipment.carrier,
                trackingNumber = shipment.trackingNumber,
                status = shipment.status,
                receiverName = shipment.receiverName,
                receiverPhone = shipment.receiverPhone,
                address = shipment.address,
                shippedAt = shipment.shippedAt,
                deliveredAt = shipment.deliveredAt,
                createdAt = try { shipment.createdAt } catch (_: UninitializedPropertyAccessException) { null },
            )
        }
    }
}
