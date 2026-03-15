package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentJpaRepository : JpaRepository<DocumentJpaEntity, Long> {
    fun findByCreatedByAndStatus(userId: Long, status: String): List<DocumentJpaEntity>
}
