package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.PublishDocumentCommand
import com.biuea.wiki.domain.event.DocumentCreatedEvent
import com.biuea.wiki.domain.event.DocumentEventPublisher
import org.springframework.stereotype.Component

@Component
class PublishDocumentFacade(
    private val documentService: DocumentService,
    private val documentEventPublisher: DocumentEventPublisher,
) {
    fun publish(input: PublishDocumentInput): PublishDocumentOutput {
        val (document, revision) = documentService.publishDocument(
            PublishDocumentCommand(
                documentId = input.documentId,
                userId = input.userId,
            )
        )

        documentEventPublisher.publishDocumentCreated(
            DocumentCreatedEvent(
                documentId = document.id,
                documentRevisionId = revision.id,
                title = document.title,
                content = document.content,
                userId = document.createdBy,
            )
        )

        return PublishDocumentOutput(
            documentId = document.id,
            revisionId = revision.id,
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
