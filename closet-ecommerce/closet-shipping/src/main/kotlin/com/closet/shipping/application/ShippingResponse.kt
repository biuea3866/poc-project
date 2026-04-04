package com.closet.shipping.application

import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShippingTrackingLog
import java.time.LocalDateTime

data class ShipmentResponse(
    val id: Long,
    val orderId: Long,
    val sellerId: Long,
    val memberId: Long,
    val carrier: String?,
    val trackingNumber: String?,
    val status: String,
    val receiverName: String,
    val receiverPhone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
    val shippedAt: LocalDateTime?,
    val deliveredAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(shipment: Shipment): ShipmentResponse {
            return ShipmentResponse(
                id = shipment.id,
                orderId = shipment.orderId,
                sellerId = shipment.sellerId,
                memberId = shipment.memberId,
                carrier = shipment.carrier,
                trackingNumber = shipment.trackingNumber,
                status = shipment.status.name,
                receiverName = shipment.receiverName,
                receiverPhone = shipment.receiverPhone,
                zipCode = shipment.zipCode,
                address = shipment.address,
                detailAddress = shipment.detailAddress,
                shippedAt = shipment.shippedAt,
                deliveredAt = shipment.deliveredAt,
                createdAt = if (shipment.id != 0L) shipment.createdAt else null,
                updatedAt = if (shipment.id != 0L) shipment.updatedAt else null,
            )
        }
    }
}

data class TrackingLogResponse(
    val id: Long,
    val shippingId: Long,
    val carrierStatus: String,
    val mappedStatus: String,
    val location: String?,
    val description: String?,
    val trackedAt: LocalDateTime,
) {
    companion object {
        fun from(log: ShippingTrackingLog): TrackingLogResponse {
            return TrackingLogResponse(
                id = log.id,
                shippingId = log.shippingId,
                carrierStatus = log.carrierStatus,
                mappedStatus = log.mappedStatus.name,
                location = log.location,
                description = log.description,
                trackedAt = log.trackedAt,
            )
        }
    }
}

data class ReturnRequestResponse(
    val id: Long,
    val orderId: Long,
    val orderItemId: Long,
    val memberId: Long,
    val sellerId: Long,
    val productOptionId: Long,
    val quantity: Int,
    val reason: String,
    val reasonDetail: String?,
    val status: String,
    val shippingFee: Long,
    val shippingFeePayer: String,
    val refundAmount: Long,
    val pickupTrackingNumber: String?,
    val inspectedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(rr: ReturnRequest): ReturnRequestResponse {
            return ReturnRequestResponse(
                id = rr.id,
                orderId = rr.orderId,
                orderItemId = rr.orderItemId,
                memberId = rr.memberId,
                sellerId = rr.sellerId,
                productOptionId = rr.productOptionId,
                quantity = rr.quantity,
                reason = rr.reason.name,
                reasonDetail = rr.reasonDetail,
                status = rr.status.name,
                shippingFee = rr.shippingFee.amount.toLong(),
                shippingFeePayer = rr.shippingFeePayer,
                refundAmount = rr.refundAmount.amount.toLong(),
                pickupTrackingNumber = rr.pickupTrackingNumber,
                inspectedAt = rr.inspectedAt,
                completedAt = rr.completedAt,
                createdAt = if (rr.id != 0L) rr.createdAt else null,
                updatedAt = if (rr.id != 0L) rr.updatedAt else null,
            )
        }
    }
}
