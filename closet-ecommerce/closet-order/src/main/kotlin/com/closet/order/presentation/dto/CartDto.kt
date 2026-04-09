package com.closet.order.presentation.dto

import com.closet.order.domain.cart.Cart
import com.closet.order.domain.cart.CartItem
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class AddCartItemRequest(
    @field:NotNull val memberId: Long,
    @field:NotNull val productId: Long,
    @field:NotNull val productOptionId: Long,
    @field:Min(1) val quantity: Int,
    @field:NotNull val unitPrice: BigDecimal,
)

data class UpdateCartItemRequest(
    @field:Min(1) val quantity: Int,
)

data class CartResponse(
    val id: Long,
    val memberId: Long,
    val items: List<CartItemResponse>,
) {
    companion object {
        fun from(
            cart: Cart,
            items: List<CartItem>,
        ): CartResponse {
            return CartResponse(
                id = cart.id,
                memberId = cart.memberId,
                items = items.map { CartItemResponse.from(it) },
            )
        }
    }
}

data class CartItemResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
) {
    companion object {
        fun from(item: CartItem): CartItemResponse {
            return CartItemResponse(
                id = item.id,
                productId = item.productId,
                productOptionId = item.productOptionId,
                quantity = item.quantity,
                unitPrice = item.unitPrice.amount,
            )
        }
    }
}
