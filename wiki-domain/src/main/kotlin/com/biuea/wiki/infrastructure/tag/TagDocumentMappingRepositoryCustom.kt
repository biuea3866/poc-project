package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.TagDocumentMapping

interface TagDocumentMappingRepositoryCustom {
    fun findByDocumentIdAndRevisionId(documentId: Long, documentRevisionId: Long): List<TagDocumentMapping>
}
