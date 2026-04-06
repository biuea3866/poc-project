package com.closet.promotion.repository

import com.closet.promotion.domain.point.PointBalance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PointBalanceRepository : JpaRepository<PointBalance, Long> {
    fun findByMemberId(memberId: Long): Optional<PointBalance>
}
