package com.biuea.wiki.infrastructure.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DocumentRepository : JpaRepository<Document, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Document?

    fun findByParentIsNullAndDeletedAtIsNull(pageable: Pageable): Page<Document>

    fun findByParentIdAndDeletedAtIsNull(parentId: Long, pageable: Pageable): Page<Document>

    fun findByCreatedByAndDeletedAtIsNull(createdBy: Long, pageable: Pageable): Page<Document>

    fun findByStatusAndDeletedAtIsNotNull(status: DocumentStatus, pageable: Pageable): Page<Document>

    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL AND (d.title LIKE %:keyword% OR d.content LIKE %:keyword%)")
    fun searchByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<Document>
}
