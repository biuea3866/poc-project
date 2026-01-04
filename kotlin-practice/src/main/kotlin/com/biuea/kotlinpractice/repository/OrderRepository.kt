package com.biuea.kotlinpractice.repository

import com.biuea.kotlinpractice.domain.Order
import com.biuea.kotlinpractice.domain.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByCustomerName(customerName: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
}
