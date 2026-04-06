package com.closet.display.domain.repository

import com.closet.display.domain.entity.Exhibition
import com.closet.display.domain.enums.ExhibitionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ExhibitionRepository : JpaRepository<Exhibition, Long> {
    fun findByStatusAndDeletedAtIsNullOrderByStartAtDesc(status: ExhibitionStatus): List<Exhibition>

    fun findByDeletedAtIsNullOrderByStartAtDesc(): List<Exhibition>
}
