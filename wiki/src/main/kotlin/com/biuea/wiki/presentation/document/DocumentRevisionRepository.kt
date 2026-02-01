package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.DocumentRevision
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRevisionRepository : JpaRepository<DocumentRevision, Long> {

    fun findByDocumentId(documentId: Long, pageable: Pageable): Page<DocumentRevision>

    fun findTopByDocumentIdOrderByCreatedAtDesc(documentId: Long): DocumentRevision?
}
