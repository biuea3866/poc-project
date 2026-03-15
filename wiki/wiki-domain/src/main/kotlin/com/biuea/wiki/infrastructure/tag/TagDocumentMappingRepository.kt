package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TagDocumentMappingRepository: JpaRepository<TagDocumentMapping, Long> {
    fun findByDocumentId(documentId: Long): List<TagDocumentMapping>

    @Query("SELECT m FROM TagDocumentMapping m JOIN FETCH m.tag WHERE m.document.id IN :documentIds")
    fun findByDocumentIdIn(@Param("documentIds") documentIds: List<Long>): List<TagDocumentMapping>
}
