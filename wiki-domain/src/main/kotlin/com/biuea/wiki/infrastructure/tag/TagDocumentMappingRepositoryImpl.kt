package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.QTag
import com.biuea.wiki.domain.tag.entity.QTagDocumentMapping
import com.biuea.wiki.domain.tag.entity.QTagType
import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import com.querydsl.jpa.impl.JPAQueryFactory

class TagDocumentMappingRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TagDocumentMappingRepositoryCustom {

    override fun findByDocumentIdAndRevisionId(
        documentId: Long,
        documentRevisionId: Long,
    ): List<TagDocumentMapping> {
        val mapping = QTagDocumentMapping.tagDocumentMapping
        val tag = QTag.tag
        val tagType = QTagType.tagType

        return queryFactory
            .selectFrom(mapping)
            .join(mapping.tag, tag).fetchJoin()
            .join(tag.tagType, tagType).fetchJoin()
            .where(
                mapping.document.id.eq(documentId)
                    .and(mapping.documentRevision.id.eq(documentRevisionId))
            )
            .fetch()
    }
}
