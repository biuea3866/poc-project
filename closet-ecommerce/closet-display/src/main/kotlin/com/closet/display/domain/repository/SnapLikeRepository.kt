package com.closet.display.domain.repository

import com.closet.display.domain.entity.SnapLike
import org.springframework.data.jpa.repository.JpaRepository

interface SnapLikeRepository : JpaRepository<SnapLike, Long> {
    fun existsBySnapIdAndMemberId(
        snapId: Long,
        memberId: Long,
    ): Boolean

    fun findBySnapIdAndMemberId(
        snapId: Long,
        memberId: Long,
    ): SnapLike?

    fun countBySnapId(snapId: Long): Long
}
