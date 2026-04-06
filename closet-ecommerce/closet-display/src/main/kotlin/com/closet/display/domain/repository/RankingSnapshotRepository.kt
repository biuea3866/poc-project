package com.closet.display.domain.repository

import com.closet.display.domain.entity.RankingSnapshot
import com.closet.display.domain.enums.PeriodType
import org.springframework.data.jpa.repository.JpaRepository

interface RankingSnapshotRepository : JpaRepository<RankingSnapshot, Long> {
    fun findByCategoryIdAndPeriodTypeOrderBySnapshotDateDescRankPositionAsc(
        categoryId: Long,
        periodType: PeriodType,
    ): List<RankingSnapshot>
}
