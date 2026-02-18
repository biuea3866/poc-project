package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.UpdateDocumentCommand
import org.springframework.stereotype.Component

@Component
class UpdateDocumentFacade(
    private val documentService: DocumentService,
) {
    fun update(input: UpdateDocumentInput): UpdateDocumentOutput {
        val (document, revision) = documentService.updateDocument(
            UpdateDocumentCommand(
                documentId = input.documentId,
                userId = input.userId,
                title = input.title,
                content = input.content,
            )
        )
        return UpdateDocumentOutput(
            documentId = document.id,
            revisionId = revision.id,
        )
    }
}

data class UpdateDocumentInput(
    val documentId: Long,
    val userId: Long,
    val title: String,
    val content: String?,
)

data class UpdateDocumentOutput(
    val documentId: Long,
    val revisionId: Long,
)
