package com.closet.content.domain.repository

import com.closet.content.domain.entity.Coordination
import com.closet.content.domain.enums.CoordinationStatus
import com.closet.content.domain.enums.CoordinationStyle
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CoordinationRepository : JpaRepository<Coordination, Long> {

    fun findByDeletedAtIsNull(pageable: Pageable): Page<Coordination>

    fun findByStyleAndStatusAndDeletedAtIsNull(
        style: CoordinationStyle,
        status: CoordinationStatus,
        pageable: Pageable
    ): Page<Coordination>
}
