package com.closet.order.repository

import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long, pageable: Pageable): Page<Order>
    fun findByIdAndDeletedAtIsNull(id: Long): Order?

    @Query("SELECT o FROM Order o WHERE o.status = 'STOCK_RESERVED' AND o.reservationExpiresAt < :now")
    fun findExpiredReservations(now: LocalDateTime): List<Order>
}
