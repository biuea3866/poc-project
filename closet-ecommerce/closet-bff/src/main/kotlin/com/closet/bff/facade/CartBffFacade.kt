package com.closet.bff.facade

import com.closet.bff.client.OrderServiceClient
import com.closet.bff.dto.AddCartItemRequest
import org.springframework.stereotype.Service

@Service
class CartBffFacade(
    private val orderClient: OrderServiceClient,
) {
    fun addCartItem(memberId: Long, request: AddCartItemRequest): Any {
        return orderClient.addCartItem(memberId, request).data!!
    }

    fun updateCartItemQuantity(itemId: Long, quantity: Int): Any {
        return orderClient.updateCartItemQuantity(itemId, mapOf("quantity" to quantity)).data!!
    }

    fun removeCartItem(itemId: Long) {
        orderClient.removeCartItem(itemId)
    }
}
