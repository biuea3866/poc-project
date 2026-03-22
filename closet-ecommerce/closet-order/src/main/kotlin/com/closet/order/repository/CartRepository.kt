package com.closet.order.repository

import com.closet.order.domain.cart.Cart
import org.springframework.data.jpa.repository.JpaRepository

interface CartRepository : JpaRepository<Cart, Long> {
    fun findByMemberId(memberId: Long): Cart?
}
