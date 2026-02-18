package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.domain.event.DocumentEventPublisher
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PublishDocumentFacade(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
    private val documentEventPublisher: DocumentEventPublisher,
) {
    @Transactional
    fun publish(input: PublishDocumentInput): PublishDocumentOutput {
        val document = documentRepository.findByIdAndStatusNot(input.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${input.documentId}")

        check(document.createdBy == input.userId) { "문서 발행 권한이 없습니다." }

        document.publish()

        val revision = DocumentRevision.create(document)
        document.addRevision(revision)
        documentRepository.save(document)

        documentEventPublisher.publishDocumentCreated(
            DocumentCreatedEvent(
                documentId = document.id,
                documentRevisionId = document.revisions.last().id,
                title = document.title,
                content = document.content,
                userId = document.createdBy,
            )
        )

        return PublishDocumentOutput(
            documentId = document.id,
            revisionId = document.revisions.last().id,
        )
    }
}

data class PublishDocumentInput(
    val documentId: Long,
    val userId: Long,
)

data class PublishDocumentOutput(
    val documentId: Long,
    val revisionId: Long,
)
