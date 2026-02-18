package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.SaveDocumentCommand
import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.domain.document.entity.DocumentStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SaveDocumentFacade(
    private val documentService: DocumentService,
) {
    @Transactional
    fun saveDocument(input: SaveDocumentInput): SaveDocumentOutput {
        val (document, revision) = documentService.saveDocument(
            SaveDocumentCommand(
                title = input.title,
                content = input.content,
                parentId = input.parentId,
                createdBy = input.createdBy,
            )
        )
        return SaveDocumentOutput(
            id = document.id,
            title = document.title,
            content = document.content,
            status = document.status,
            aiStatus = document.aiStatus,
            revisionId = revision.id,
        )
    }
}

data class SaveDocumentInput(
    val title: String,
    val content: String?,
    val parentId: Long?,
    val createdBy: Long,
)

data class SaveDocumentOutput(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val revisionId: Long,
)
