package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.RestoreDocumentCommand
import org.springframework.stereotype.Component

@Component
class RestoreDocumentFacade(
    private val documentService: DocumentService,
) {
    fun restore(input: RestoreDocumentInput): RestoreDocumentOutput {
        val (document, restoredToRoot) = documentService.restoreDocument(
            RestoreDocumentCommand(
                documentId = input.documentId,
                userId = input.userId,
            )
        )
        return RestoreDocumentOutput(
            documentId = document.id,
            restoredToRoot = restoredToRoot,
        )
    }
}

data class RestoreDocumentInput(
    val documentId: Long,
    val userId: Long,
)

data class RestoreDocumentOutput(
    val documentId: Long,
    val restoredToRoot: Boolean,
)
