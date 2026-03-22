package com.closet.settlement.presentation.dto

import com.closet.settlement.domain.commission.CommissionRate
import com.closet.settlement.domain.settlement.Settlement
import com.closet.settlement.domain.settlement.SettlementItem
import com.closet.settlement.domain.settlement.SettlementStatus
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

data class CalculateSettlementRequest(
    @field:NotNull val sellerId: Long,
    @field:NotNull val periodFrom: LocalDateTime,
    @field:NotNull val periodTo: LocalDateTime,
    val items: List<CalculateSettlementItemRequest> = emptyList(),
    val totalRefund: BigDecimal = BigDecimal.ZERO,
)

data class CalculateSettlementItemRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val orderItemId: Long,
    @field:NotNull val saleAmount: BigDecimal,
    @field:NotNull val categoryId: Long,
)

data class UpdateCommissionRateRequest(
    @field:NotNull val rate: BigDecimal,
    val effectiveFrom: LocalDateTime? = null,
)

data class SettlementResponse(
    val id: Long,
    val sellerId: Long,
    val periodFrom: LocalDateTime,
    val periodTo: LocalDateTime,
    val totalSales: BigDecimal,
    val totalCommission: BigDecimal,
    val totalRefund: BigDecimal,
    val netAmount: BigDecimal,
    val status: SettlementStatus,
    val items: List<SettlementItemResponse>,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(settlement: Settlement, items: List<SettlementItem> = emptyList()): SettlementResponse {
            return SettlementResponse(
                id = settlement.id,
                sellerId = settlement.sellerId,
                periodFrom = settlement.periodFrom,
                periodTo = settlement.periodTo,
                totalSales = settlement.totalSales,
                totalCommission = settlement.totalCommission,
                totalRefund = settlement.totalRefund,
                netAmount = settlement.netAmount,
                status = settlement.status,
                items = items.map { SettlementItemResponse.from(it) },
                createdAt = if (settlement.id != 0L) settlement.createdAt else null,
            )
        }
    }
}

data class SettlementItemResponse(
    val id: Long,
    val orderId: Long,
    val orderItemId: Long,
    val saleAmount: BigDecimal,
    val commissionRate: BigDecimal,
    val commissionAmount: BigDecimal,
) {
    companion object {
        fun from(item: SettlementItem): SettlementItemResponse {
            return SettlementItemResponse(
                id = item.id,
                orderId = item.orderId,
                orderItemId = item.orderItemId,
                saleAmount = item.saleAmount,
                commissionRate = item.commissionRate,
                commissionAmount = item.commissionAmount,
            )
        }
    }
}

data class CommissionRateResponse(
    val id: Long,
    val categoryId: Long,
    val rate: BigDecimal,
    val effectiveFrom: LocalDateTime,
) {
    companion object {
        fun from(commissionRate: CommissionRate): CommissionRateResponse {
            return CommissionRateResponse(
                id = commissionRate.id,
                categoryId = commissionRate.categoryId,
                rate = commissionRate.rate,
                effectiveFrom = commissionRate.effectiveFrom,
            )
        }
    }
}
