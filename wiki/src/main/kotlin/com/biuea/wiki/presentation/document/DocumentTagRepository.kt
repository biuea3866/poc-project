package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.DocumentTag
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentTagRepository : JpaRepository<DocumentTag, Long> {

    fun findByDocumentId(documentId: Long): List<DocumentTag>

    fun findByDocumentIdAndDocumentRevisionId(documentId: Long, documentRevisionId: Long): List<DocumentTag>
}
