package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.tag.SaveTagCommand
import com.biuea.wiki.domain.tag.TagService
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val tagService: TagService
) {
    @Transactional
    fun saveDocument(command: SaveDocumentCommand): Document {
        val parent = command.parentId?.let { documentRepository.findByIdAndDeletedAtIsNull(it) }
        val document = Document(
            title = command.title,
            content = command.content,
            status = command.status,
            parent = parent,
            createdBy = command.createdBy,
            updatedBy = command.createdBy
        )

        val revision = DocumentRevision.create(document)
        document.addRevision(revision)
        document.validate()

        val savedDocument = documentRepository.save(document)

        if (command.tags.isNotEmpty()) {
            val documentRevisionId = savedDocument.latestRevisionId
                ?: throw IllegalStateException("Document revision must exist to save tags.")
            tagService.saveTags(
                SaveTagCommand(
                    tags = command.tags.map { SaveTagCommand.TagInput(it.name, it.tagConstant) },
                    documentId = savedDocument.id,
                    documentRevisionId = documentRevisionId
                )
            )
        }

        return savedDocument
    }
}
