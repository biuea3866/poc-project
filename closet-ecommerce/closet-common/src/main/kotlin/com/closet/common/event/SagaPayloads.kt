package com.closet.common.event

/**
 * Saga 이벤트 페이로드 DTOs.
 * Order-Inventory-Payment Saga에서 사용하는 메시지 포맷.
 */

data class OrderCreatedPayload(
    val orderId: Long,
    val sagaId: String,
    val items: List<ItemPayload>,
)

data class ItemPayload(
    val productOptionId: Long,
    val quantity: Int,
)

data class PaymentRequestedPayload(
    val orderId: Long,
    val sagaId: String,
    val amount: Long,
)

data class InventoryReservedPayload(
    val orderId: Long,
    val sagaId: String,
)

data class InventoryFailedPayload(
    val orderId: Long,
    val sagaId: String,
    val reason: String?,
)

data class PaymentApprovedPayload(
    val orderId: Long,
    val sagaId: String,
    val paymentId: Long,
)

data class PaymentFailedPayload(
    val orderId: Long,
    val sagaId: String,
    val reason: String?,
)

data class InventoryReleasePayload(
    val orderId: Long,
    val sagaId: String,
    val items: List<ItemPayload>,
)
