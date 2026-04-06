package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.timesale.TimeSaleOrder
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.ZonedDateTime

data class PurchaseTimeSaleRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val memberId: Long,
    @field:Positive val quantity: Int = 1,
)

data class TimeSaleOrderResponse(
    val id: Long,
    val timeSaleId: Long,
    val orderId: Long,
    val memberId: Long,
    val quantity: Int,
    val purchasedAt: ZonedDateTime,
) {
    companion object {
        fun from(order: TimeSaleOrder): TimeSaleOrderResponse =
            TimeSaleOrderResponse(
                id = order.id,
                timeSaleId = order.timeSaleId,
                orderId = order.orderId,
                memberId = order.memberId,
                quantity = order.quantity,
                purchasedAt = order.purchasedAt,
            )
    }
}
