package com.closet.order.repository

import com.closet.order.domain.order.Order
import com.closet.order.domain.order.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByMemberIdAndDeletedAtIsNull(
        memberId: Long,
        pageable: Pageable,
    ): Page<Order>

    fun findByIdAndDeletedAtIsNull(id: Long): Order?

    fun findByStatusAndDeletedAtIsNullAndDeliveredAtIsNotNullAndDeliveredAtLessThanEqual(
        status: OrderStatus,
        cutoff: ZonedDateTime,
    ): List<Order>
}
