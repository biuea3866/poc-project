package com.biuea.aiwiki.domain.document.port

import com.biuea.aiwiki.domain.document.model.DocumentRevision

interface DocumentRevisionRepository {
    fun save(revision: DocumentRevision): DocumentRevision
    fun findByDocumentId(documentId: Long): List<DocumentRevision>
}
