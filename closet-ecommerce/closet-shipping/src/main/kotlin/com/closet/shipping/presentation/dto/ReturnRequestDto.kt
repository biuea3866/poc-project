package com.closet.shipping.presentation.dto

import com.closet.shipping.domain.Carrier
import com.closet.shipping.domain.ReturnReasonType
import com.closet.shipping.domain.ReturnRequest
import com.closet.shipping.domain.ReturnStatus
import com.closet.shipping.domain.ReturnType
import com.closet.shipping.domain.ShippingFeeBearer
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateReturnRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val orderItemId: Long,
    @field:NotNull val type: ReturnType,
    @field:NotNull val reasonType: ReturnReasonType,
    val reasonDetail: String? = null,
    @field:NotNull val shippingFeeBearer: ShippingFeeBearer,
    val returnShippingFee: Long = 0,
)

data class ReturnRequestResponse(
    val id: Long,
    val orderId: Long,
    val orderItemId: Long,
    val type: ReturnType,
    val reasonType: ReturnReasonType,
    val reasonDetail: String?,
    val status: ReturnStatus,
    val returnTrackingNumber: String?,
    val returnCarrier: Carrier?,
    val shippingFeeBearer: ShippingFeeBearer,
    val returnShippingFee: Long,
    val requestedAt: LocalDateTime,
    val approvedAt: LocalDateTime?,
    val rejectedAt: LocalDateTime?,
) {
    companion object {
        fun from(returnRequest: ReturnRequest): ReturnRequestResponse {
            return ReturnRequestResponse(
                id = returnRequest.id,
                orderId = returnRequest.orderId,
                orderItemId = returnRequest.orderItemId,
                type = returnRequest.type,
                reasonType = returnRequest.reasonType,
                reasonDetail = returnRequest.reasonDetail,
                status = returnRequest.status,
                returnTrackingNumber = returnRequest.returnTrackingNumber,
                returnCarrier = returnRequest.returnCarrier,
                shippingFeeBearer = returnRequest.shippingFeeBearer,
                returnShippingFee = returnRequest.returnShippingFee,
                requestedAt = returnRequest.requestedAt,
                approvedAt = returnRequest.approvedAt,
                rejectedAt = returnRequest.rejectedAt,
            )
        }
    }
}
