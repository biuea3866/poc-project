package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DeleteDocumentCommand
import com.biuea.wiki.domain.document.DocumentService
import org.springframework.stereotype.Component

@Component
class DeleteDocumentFacade(
    private val documentService: DocumentService,
) {
    fun delete(input: DeleteDocumentInput) {
        documentService.deleteDocument(
            DeleteDocumentCommand(
                documentId = input.documentId,
                userId = input.userId,
            )
        )
    }
}

data class DeleteDocumentInput(
    val documentId: Long,
    val userId: Long,
)
