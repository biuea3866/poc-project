package com.closet.member.domain.repository

import com.closet.member.domain.point.PointBalance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PointBalanceRepository : JpaRepository<PointBalance, Long> {
    fun findByMemberId(memberId: Long): Optional<PointBalance>
}
