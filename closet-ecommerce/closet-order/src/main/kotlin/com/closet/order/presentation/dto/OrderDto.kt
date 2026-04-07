package com.closet.order.presentation.dto

import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderItem
import com.closet.order.domain.order.OrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateOrderRequest(
    @field:NotNull val memberId: Long,
    @field:NotNull val sellerId: Long,
    @field:NotEmpty @field:Valid val items: List<CreateOrderItemRequest>,
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val zipCode: String,
    @field:NotBlank val address: String,
    @field:NotBlank val detailAddress: String,
    val shippingFee: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
)

data class CreateOrderItemRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val productOptionId: Long,
    @field:NotBlank val productName: String,
    @field:NotBlank val optionName: String,
    @field:NotNull val categoryId: Long,
    @field:Min(1) val quantity: Int,
    @field:NotNull val unitPrice: BigDecimal,
)

data class CancelOrderRequest(
    @field:NotBlank val reason: String,
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val memberId: Long,
    val sellerId: Long,
    val totalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val shippingFee: BigDecimal,
    val paymentAmount: BigDecimal,
    val status: OrderStatus,
    val receiverName: String,
    val receiverPhone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
    val orderedAt: ZonedDateTime?,
    val items: List<OrderItemResponse>,
) {
    companion object {
        fun from(
            order: Order,
            items: List<OrderItem>,
        ): OrderResponse {
            return OrderResponse(
                id = order.id,
                orderNumber = order.orderNumber,
                memberId = order.memberId,
                sellerId = order.sellerId,
                totalAmount = order.totalAmount.amount,
                discountAmount = order.discountAmount.amount,
                shippingFee = order.shippingFee.amount,
                paymentAmount = order.paymentAmount.amount,
                status = order.status,
                receiverName = order.receiverName,
                receiverPhone = order.receiverPhone,
                zipCode = order.zipCode,
                address = order.address,
                detailAddress = order.detailAddress,
                orderedAt = order.orderedAt,
                items = items.map { OrderItemResponse.from(it) },
            )
        }
    }
}

data class OrderItemResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val productName: String,
    val optionName: String,
    val categoryId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val status: String,
) {
    companion object {
        fun from(item: OrderItem): OrderItemResponse {
            return OrderItemResponse(
                id = item.id,
                productId = item.productId,
                productOptionId = item.productOptionId,
                productName = item.productName,
                optionName = item.optionName,
                categoryId = item.categoryId,
                quantity = item.quantity,
                unitPrice = item.unitPrice.amount,
                totalPrice = item.totalPrice.amount,
                status = item.status.name,
            )
        }
    }
}
