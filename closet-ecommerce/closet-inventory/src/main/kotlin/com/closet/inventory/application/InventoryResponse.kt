package com.closet.inventory.application

import com.closet.inventory.domain.Inventory
import java.time.ZonedDateTime

data class InventoryResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val sku: String,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val reservedQuantity: Int,
    val safetyThreshold: Int,
    val belowSafetyThreshold: Boolean,
    val outOfStock: Boolean,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {
    companion object {
        fun from(inventory: Inventory): InventoryResponse {
            return InventoryResponse(
                id = inventory.id,
                productId = inventory.productId,
                productOptionId = inventory.productOptionId,
                sku = inventory.sku,
                totalQuantity = inventory.totalQuantity,
                availableQuantity = inventory.availableQuantity,
                reservedQuantity = inventory.reservedQuantity,
                safetyThreshold = inventory.safetyThreshold,
                belowSafetyThreshold = inventory.isBelowSafetyThreshold(),
                outOfStock = inventory.isOutOfStock(),
                createdAt = if (inventory.id != 0L) inventory.createdAt else null,
                updatedAt = if (inventory.id != 0L) inventory.updatedAt else null,
            )
        }
    }
}

data class InventoryResult(
    val success: Boolean,
    val message: String? = null,
    val insufficientItems: List<InsufficientItemInfo> = emptyList(),
) {
    data class InsufficientItemInfo(
        val productOptionId: Long,
        val sku: String,
        val requested: Int,
        val available: Int,
    )

    companion object {
        fun success(): InventoryResult = InventoryResult(success = true)

        fun insufficient(items: List<InsufficientItemInfo>): InventoryResult =
            InventoryResult(
                success = false,
                message = "재고가 부족합니다",
                insufficientItems = items,
            )
    }
}
