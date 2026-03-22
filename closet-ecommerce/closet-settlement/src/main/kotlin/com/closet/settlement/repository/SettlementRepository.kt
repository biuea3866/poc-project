package com.closet.settlement.repository

import com.closet.settlement.domain.settlement.Settlement
import com.closet.settlement.domain.settlement.SettlementStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findBySellerIdAndStatus(sellerId: Long, status: SettlementStatus, pageable: Pageable): Page<Settlement>
    fun findBySellerId(sellerId: Long, pageable: Pageable): Page<Settlement>
    fun findByPeriodFromGreaterThanEqualAndPeriodToLessThanEqual(
        periodFrom: LocalDateTime,
        periodTo: LocalDateTime,
        pageable: Pageable,
    ): Page<Settlement>
}
