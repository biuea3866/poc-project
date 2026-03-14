package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentRevisionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRevisionJpaRepository : JpaRepository<DocumentRevisionJpaEntity, Long> {
    fun findByDocumentIdOrderByCreatedAtDesc(documentId: Long): List<DocumentRevisionJpaEntity>
}
