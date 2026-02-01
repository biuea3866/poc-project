package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.DocumentSummary
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentSummaryRepository : JpaRepository<DocumentSummary, Long> {

    fun findByDocumentIdAndDocumentRevisionId(documentId: Long, documentRevisionId: Long): DocumentSummary?

    fun findTopByDocumentIdOrderByIdDesc(documentId: Long): DocumentSummary?
}
