package com.closet.display.domain.repository

import com.closet.display.domain.entity.Magazine
import org.springframework.data.jpa.repository.JpaRepository

interface MagazineRepository : JpaRepository<Magazine, Long> {
    fun findByIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc(): List<Magazine>

    fun findByCategoryAndIsPublishedTrueAndDeletedAtIsNullOrderByPublishedAtDesc(category: String): List<Magazine>

    fun findByDeletedAtIsNullOrderByCreatedAtDesc(): List<Magazine>
}
