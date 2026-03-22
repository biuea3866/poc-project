package com.closet.display.domain.repository

import com.closet.display.domain.entity.RankingSnapshot
import com.closet.display.domain.enums.PeriodType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RankingSnapshotRepository : JpaRepository<RankingSnapshot, Long> {

    @Query("""
        SELECT r FROM RankingSnapshot r
        WHERE r.categoryId = :categoryId
          AND r.periodType = :periodType
        ORDER BY r.snapshotDate DESC, r.rankPosition ASC
    """)
    fun findByCategoryIdAndPeriodType(
        @Param("categoryId") categoryId: Long,
        @Param("periodType") periodType: PeriodType
    ): List<RankingSnapshot>
}
