package com.closet.inventory.presentation.dto

import com.closet.inventory.domain.InventoryItem
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ReserveStockRequest(
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
    @field:NotBlank val orderId: String,
)

data class ReleaseStockRequest(
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
    @field:NotBlank val orderId: String,
)

data class DeductStockRequest(
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
    @field:NotBlank val orderId: String,
)

data class RestockRequest(
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
)

data class InventoryResponse(
    val id: Long,
    val productOptionId: Long,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val reservedQuantity: Int,
    val safetyThreshold: Int,
    val belowSafetyThreshold: Boolean,
) {
    companion object {
        fun from(item: InventoryItem): InventoryResponse {
            return InventoryResponse(
                id = item.id,
                productOptionId = item.productOptionId,
                totalQuantity = item.totalQuantity,
                availableQuantity = item.availableQuantity,
                reservedQuantity = item.reservedQuantity,
                safetyThreshold = item.safetyThreshold,
                belowSafetyThreshold = item.isBelowSafetyThreshold(),
            )
        }
    }
}
