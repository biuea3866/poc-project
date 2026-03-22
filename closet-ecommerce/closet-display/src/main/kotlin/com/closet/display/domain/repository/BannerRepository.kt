package com.closet.display.domain.repository

import com.closet.display.domain.entity.Banner
import com.closet.display.domain.enums.BannerPosition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface BannerRepository : JpaRepository<Banner, Long> {

    @Query("""
        SELECT b FROM Banner b
        WHERE b.position = :position
          AND b.isVisible = true
          AND b.startAt <= :now
          AND b.endAt >= :now
          AND b.deletedAt IS NULL
        ORDER BY b.sortOrder ASC
    """)
    fun findActiveByPosition(
        @Param("position") position: BannerPosition,
        @Param("now") now: LocalDateTime
    ): List<Banner>
}
