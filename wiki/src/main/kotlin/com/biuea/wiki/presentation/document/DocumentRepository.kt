package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Document?

    fun findByParentIsNullAndDeletedAtIsNull(pageable: Pageable): Page<Document>

    fun findByParentIdAndDeletedAtIsNull(parentId: Long, pageable: Pageable): Page<Document>

    fun findByCreatedByAndDeletedAtIsNull(createdBy: Long, pageable: Pageable): Page<Document>
}
