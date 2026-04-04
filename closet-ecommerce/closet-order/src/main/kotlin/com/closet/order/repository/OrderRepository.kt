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

    /**
     * 자동 구매확정 대상 조회 (PD-16, PD-17).
     * DELIVERED 상태 + deliveredAt이 cutoff(168시간) 이전 + 반품/교환 미진행.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.status = com.closet.order.domain.order.OrderStatus.DELIVERED
        AND o.deletedAt IS NULL
        AND o.deliveredAt IS NOT NULL
        AND o.deliveredAt <= :cutoff
    """)
    fun findAutoConfirmCandidates(@Param("cutoff") cutoff: LocalDateTime): List<Order>
}
