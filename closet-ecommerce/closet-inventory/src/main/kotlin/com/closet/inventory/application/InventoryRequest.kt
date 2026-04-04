package com.closet.inventory.application

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CreateInventoryRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val productOptionId: Long,
    @field:NotBlank val sku: String,
    @field:Min(0) val totalQuantity: Int,
    @field:Min(0) val safetyThreshold: Int = 10,
)

data class InboundRequest(
    @field:Positive(message = "입고 수량은 0보다 커야 합니다")
    val quantity: Int,
    val reason: String? = null,
)

data class ReserveRequest(
    @field:NotNull val orderId: Long,
    @field:NotEmpty @field:Valid val items: List<ReserveItemRequest>,
)

data class ReserveItemRequest(
    @field:NotNull val productOptionId: Long,
    @field:Positive val quantity: Int,
)

data class DeductRequest(
    @field:NotNull val orderId: Long,
    @field:NotEmpty @field:Valid val items: List<DeductItemRequest>,
)

data class DeductItemRequest(
    @field:NotNull val productOptionId: Long,
    @field:Positive val quantity: Int,
)

data class ReleaseRequest(
    @field:NotNull val orderId: Long,
    @field:NotEmpty @field:Valid val items: List<ReleaseItemRequest>,
    val reason: String? = null,
)

data class ReleaseItemRequest(
    @field:NotNull val productOptionId: Long,
    @field:Positive val quantity: Int,
)
