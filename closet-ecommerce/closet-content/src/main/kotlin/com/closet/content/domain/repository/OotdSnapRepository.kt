package com.closet.content.domain.repository

import com.closet.content.domain.entity.OotdSnap
import com.closet.content.domain.enums.OotdSnapStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OotdSnapRepository : JpaRepository<OotdSnap, Long> {

    fun findByStatusAndDeletedAtIsNull(status: OotdSnapStatus, pageable: Pageable): Page<OotdSnap>

    fun findByMemberIdAndStatusAndDeletedAtIsNull(
        memberId: Long,
        status: OotdSnapStatus,
        pageable: Pageable
    ): Page<OotdSnap>
}
