package com.closet.member.domain.repository

import com.closet.member.domain.PointHistory
import org.springframework.data.jpa.repository.JpaRepository

interface PointHistoryRepository : JpaRepository<PointHistory, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<PointHistory>
}
