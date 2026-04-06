package com.closet.promotion.repository

import com.closet.promotion.domain.point.PointHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PointHistoryRepository : JpaRepository<PointHistory, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<PointHistory>
}
