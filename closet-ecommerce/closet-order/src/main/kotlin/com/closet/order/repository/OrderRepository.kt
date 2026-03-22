package com.closet.order.repository

import com.closet.order.domain.order.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long, pageable: Pageable): Page<Order>
    fun findByIdAndDeletedAtIsNull(id: Long): Order?
}
