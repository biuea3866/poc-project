package com.biuea.wiki.domain.document

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Document?

    fun findByParentIdAndDeletedAtIsNull(parentId: Long?, pageable: Pageable): Page<Document>

    fun findByCreatedByAndDeletedAtIsNull(createdBy: Long, pageable: Pageable): Page<Document>
}
