package com.closet.promotion.repository

import com.closet.promotion.domain.point.PointBalance
import org.springframework.data.jpa.repository.JpaRepository

interface PointBalanceRepository : JpaRepository<PointBalance, Long> {
    fun findByMemberId(memberId: Long): PointBalance?
}
