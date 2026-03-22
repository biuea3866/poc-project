package com.closet.display.domain.repository

import com.closet.display.domain.entity.Exhibition
import com.closet.display.domain.enums.ExhibitionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ExhibitionRepository : JpaRepository<Exhibition, Long> {

    @Query("""
        SELECT e FROM Exhibition e
        WHERE e.status = :status
          AND e.deletedAt IS NULL
        ORDER BY e.startAt DESC
    """)
    fun findByStatus(status: ExhibitionStatus): List<Exhibition>

    @Query("""
        SELECT e FROM Exhibition e
        WHERE e.status = 'ACTIVE'
          AND e.startAt <= :now
          AND e.endAt >= :now
          AND e.deletedAt IS NULL
        ORDER BY e.startAt DESC
    """)
    fun findActive(now: LocalDateTime): List<Exhibition>
}
