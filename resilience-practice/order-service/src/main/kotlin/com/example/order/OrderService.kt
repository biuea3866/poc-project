package com.example.order

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderService(
    private val paymentClient: PaymentClient,
    private val inventoryClient: InventoryClient,
) {
    fun place(req: PlaceOrderRequest): OrderResponse {
        val orderId = UUID.randomUUID().toString()

        val reserve = inventoryClient.reserve(
            ReserveRequest(orderId = orderId, sku = req.sku, quantity = req.quantity)
        )

        val payment = paymentClient.pay(
            PaymentRequest(orderId = orderId, amount = req.amount)
        ).join()

        return OrderResponse(
            orderId = orderId,
            inventoryStatus = reserve.status,
            paymentStatus = payment.status,
            paymentId = payment.paymentId,
        )
    }
}

data class PlaceOrderRequest(val sku: String, val quantity: Int, val amount: Long)
data class OrderResponse(
    val orderId: String,
    val inventoryStatus: String,
    val paymentStatus: String,
    val paymentId: String,
)
