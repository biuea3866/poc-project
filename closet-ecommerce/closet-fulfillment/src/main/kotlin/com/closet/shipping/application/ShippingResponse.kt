package com.closet.shipping.application

import com.closet.shipping.domain.ExchangeRequest
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.Shipment
import com.closet.shipping.domain.ShippingTrackingLog
import java.time.ZonedDateTime

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
    val shippedAt: ZonedDateTime?,
    val deliveredAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
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
    val trackedAt: ZonedDateTime,
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
    val inspectedAt: ZonedDateTime?,
    val completedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
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

// === Exchange Response (CP-28) ===

data class ExchangeRequestResponse(
    val id: Long,
    val orderId: Long,
    val orderItemId: Long,
    val memberId: Long,
    val sellerId: Long,
    val originalProductOptionId: Long,
    val newProductOptionId: Long,
    val quantity: Int,
    val reason: String,
    val reasonDetail: String?,
    val status: String,
    val shippingFee: Long,
    val shippingFeePayer: String,
    val pickupTrackingNumber: String?,
    val newTrackingNumber: String?,
    val completedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(er: ExchangeRequest): ExchangeRequestResponse {
            return ExchangeRequestResponse(
                id = er.id,
                orderId = er.orderId,
                orderItemId = er.orderItemId,
                memberId = er.memberId,
                sellerId = er.sellerId,
                originalProductOptionId = er.originalProductOptionId,
                newProductOptionId = er.newProductOptionId,
                quantity = er.quantity,
                reason = er.reason.name,
                reasonDetail = er.reasonDetail,
                status = er.status.name,
                shippingFee = er.shippingFee.amount.toLong(),
                shippingFeePayer = er.shippingFeePayer,
                pickupTrackingNumber = er.pickupTrackingNumber,
                newTrackingNumber = er.newTrackingNumber,
                completedAt = er.completedAt,
                createdAt = if (er.id != 0L) er.createdAt else null,
                updatedAt = if (er.id != 0L) er.updatedAt else null,
            )
        }
    }
}
