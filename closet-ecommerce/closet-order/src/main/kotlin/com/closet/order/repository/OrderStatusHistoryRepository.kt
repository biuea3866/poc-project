package com.closet.order.repository

import com.closet.order.domain.order.OrderStatusHistory
import org.springframework.data.jpa.repository.JpaRepository

interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistory, Long> {
    fun findByOrderIdOrderByCreatedAtAsc(orderId: Long): List<OrderStatusHistory>
}
