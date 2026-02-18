package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SearchDocumentFacade(
    private val documentRepository: DocumentRepository,
) {
    @Transactional(readOnly = true)
    fun search(input: SearchDocumentInput): Page<SearchDocumentItem> {
        return documentRepository.searchByKeyword(input.keyword, pageable = input.pageable)
            .map { doc ->
                SearchDocumentItem(
                    id = doc.id,
                    title = doc.title,
                    content = doc.content?.take(200),
                    status = doc.status,
                    aiStatus = doc.aiStatus,
                    parentId = doc.parent?.id,
                )
            }
    }
}

data class SearchDocumentInput(
    val keyword: String,
    val pageable: Pageable,
)

data class SearchDocumentItem(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
)
