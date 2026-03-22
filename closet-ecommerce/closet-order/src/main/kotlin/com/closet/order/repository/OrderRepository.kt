package com.closet.order.repository

import com.closet.order.domain.order.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByMemberIdAndDeletedAtIsNull(memberId: Long, pageable: Pageable): Page<Order>
    fun findByIdAndDeletedAtIsNull(id: Long): Order?

    @Query(
        "SELECT o FROM Order o WHERE o.status = 'STOCK_RESERVED' " +
            "AND o.reservationExpiresAt IS NOT NULL " +
            "AND o.reservationExpiresAt < :now " +
            "AND o.deletedAt IS NULL"
    )
    fun findExpiredReservations(@Param("now") now: LocalDateTime): List<Order>
}
