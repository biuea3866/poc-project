package com.closet.shipping.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface ShipmentRepository : JpaRepository<Shipment, Long> {

    fun findByOrderId(orderId: Long): Optional<Shipment>

    fun findByIdAndSellerIdIn(id: Long, sellerIds: Collection<Long>): Optional<Shipment>

    fun findByStatusIn(statuses: Collection<ShippingStatus>): List<Shipment>

    /**
     * 자동 구매확정 대상 조회.
     * 배송 완료(DELIVERED) 상태이고 delivered_at + 168시간 경과한 건.
     */
    @Query("""
        SELECT s FROM Shipment s
        WHERE s.status = 'DELIVERED'
        AND s.deliveredAt <= :cutoff
    """)
    fun findAutoConfirmCandidates(@Param("cutoff") cutoff: LocalDateTime): List<Shipment>
}
