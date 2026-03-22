package com.closet.content.domain.repository

import com.closet.content.domain.entity.Magazine
import com.closet.content.domain.enums.MagazineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MagazineRepository : JpaRepository<Magazine, Long> {

    fun findByStatusAndDeletedAtIsNull(status: MagazineStatus, pageable: Pageable): Page<Magazine>

    fun findByDeletedAtIsNull(pageable: Pageable): Page<Magazine>

    @Query("SELECT DISTINCT m FROM Magazine m JOIN m.tags t WHERE t.tagName = :tagName AND m.deletedAt IS NULL")
    fun findByTagName(tagName: String, pageable: Pageable): Page<Magazine>
}
