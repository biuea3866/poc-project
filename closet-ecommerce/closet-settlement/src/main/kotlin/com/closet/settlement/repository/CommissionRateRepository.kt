package com.closet.settlement.repository

import com.closet.settlement.domain.commission.CommissionRate
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface CommissionRateRepository : JpaRepository<CommissionRate, Long> {
    fun findTopByCategoryIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
        categoryId: Long,
        effectiveFrom: LocalDateTime,
    ): CommissionRate?

    fun findByCategoryIdOrderByEffectiveFromDesc(categoryId: Long): List<CommissionRate>
}
