package com.closet.settlement.repository

import com.closet.settlement.domain.settlement.SettlementItem
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementItemRepository : JpaRepository<SettlementItem, Long> {
    fun findBySettlementId(settlementId: Long): List<SettlementItem>
}
