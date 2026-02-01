package com.biuea.wiki.domain.document

import org.springframework.data.jpa.repository.JpaRepository

interface DocumentSummaryRepository : JpaRepository<DocumentSummary, Long> {

    fun findByDocumentIdAndDocumentRevisionId(documentId: Long, documentRevisionId: Long): DocumentSummary?

    fun findTopByDocumentIdOrderByIdDesc(documentId: Long): DocumentSummary?
}
