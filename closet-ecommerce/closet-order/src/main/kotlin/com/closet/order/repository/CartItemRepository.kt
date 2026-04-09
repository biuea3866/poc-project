package com.closet.order.repository

import com.closet.order.domain.cart.CartItem
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {
    fun findByCartId(cartId: Long): List<CartItem>

    fun deleteByCartId(cartId: Long)
}
