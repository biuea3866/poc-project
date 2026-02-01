package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.DocumentTagMap
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentTagMapRepository : JpaRepository<DocumentTagMap, Long> {

    fun findByDocumentId(documentId: Long): List<DocumentTagMap>

    fun deleteByDocumentId(documentId: Long)

    fun findByDocumentIdAndDocumentRevisionId(documentId: Long, documentRevisionId: Long): List<DocumentTagMap>
}
