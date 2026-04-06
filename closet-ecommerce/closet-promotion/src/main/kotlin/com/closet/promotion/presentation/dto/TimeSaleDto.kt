package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.timesale.TimeSale
import com.closet.promotion.domain.timesale.TimeSaleStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateTimeSaleRequest(
    @field:NotNull val productId: Long,
    @field:NotNull @field:Positive val salePrice: BigDecimal,
    @field:Positive val limitQuantity: Int,
    @field:NotNull val startAt: ZonedDateTime,
    @field:NotNull val endAt: ZonedDateTime,
)

data class TimeSaleResponse(
    val id: Long,
    val productId: Long,
    val salePrice: BigDecimal,
    val limitQuantity: Int,
    val soldCount: Int,
    val startAt: ZonedDateTime,
    val endAt: ZonedDateTime,
    val status: TimeSaleStatus,
    val remainingQuantity: Int,
) {
    companion object {
        fun from(timeSale: TimeSale): TimeSaleResponse =
            TimeSaleResponse(
                id = timeSale.id,
                productId = timeSale.productId,
                salePrice = timeSale.salePrice,
                limitQuantity = timeSale.limitQuantity,
                soldCount = timeSale.soldCount,
                startAt = timeSale.startAt,
                endAt = timeSale.endAt,
                status = timeSale.status,
                remainingQuantity = timeSale.limitQuantity - timeSale.soldCount,
            )
    }
}
