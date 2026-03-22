package com.closet.order.repository

import com.closet.order.domain.order.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findByOrderId(orderId: Long): List<OrderItem>
}
