package com.closet.display.domain.repository

import com.closet.display.domain.entity.Banner
import com.closet.display.domain.enums.BannerPosition
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface BannerRepository : JpaRepository<Banner, Long> {
    fun findByPositionAndIsVisibleTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualAndDeletedAtIsNullOrderBySortOrderAsc(
        position: BannerPosition,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<Banner>

    fun findByDeletedAtIsNullOrderBySortOrderAsc(): List<Banner>
}
