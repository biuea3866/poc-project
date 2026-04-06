package com.closet.display.domain.repository

import com.closet.display.domain.entity.Snap
import com.closet.display.domain.enums.SnapStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SnapRepository : JpaRepository<Snap, Long> {
    fun findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status: SnapStatus): List<Snap>

    fun findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId: Long): List<Snap>

    fun findByDeletedAtIsNullOrderByCreatedAtDesc(): List<Snap>
}
