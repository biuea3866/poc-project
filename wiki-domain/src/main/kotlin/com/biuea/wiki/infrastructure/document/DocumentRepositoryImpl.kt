package com.biuea.wiki.infrastructure.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.document.entity.QDocument
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class DocumentRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DocumentRepositoryCustom {

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<Document> {
        val doc = QDocument.document

        val predicate = doc.status.eq(DocumentStatus.ACTIVE)
            .and(
                doc.title.containsIgnoreCase(keyword)
                    .or(doc.content.containsIgnoreCase(keyword))
            )

        val content = queryFactory
            .selectFrom(doc)
            .where(predicate)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(doc.count())
            .from(doc)
            .where(predicate)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}
