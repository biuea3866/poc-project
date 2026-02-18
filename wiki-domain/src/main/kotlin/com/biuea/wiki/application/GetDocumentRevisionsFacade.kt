package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.DocumentRevisionData
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class GetDocumentRevisionsFacade(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
) {
    @Transactional(readOnly = true)
    fun getRevisions(documentId: Long, pageable: Pageable): Page<DocumentRevisionItem> {
        documentRepository.findByIdAndStatusNot(documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")

        return documentRevisionRepository.findByDocumentId(documentId, pageable)
            .map { revision ->
                DocumentRevisionItem(
                    id = revision.id,
                    data = revision.data,
                    createdBy = revision.createdBy,
                    createdAt = revision.createdAt,
                )
            }
    }
}

data class DocumentRevisionItem(
    val id: Long,
    val data: DocumentRevisionData,
    val createdBy: Long,
    val createdAt: ZonedDateTime,
)
