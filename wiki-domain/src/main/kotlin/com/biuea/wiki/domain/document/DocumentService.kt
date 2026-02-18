package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
) {
    @Transactional
    fun saveDocument(command: SaveDocumentCommand): Pair<Document, DocumentRevision> {
        val parent = command.parentId?.let {
            documentRepository.findByIdAndStatusIsNot(it)
                ?: throw IllegalArgumentException("부모 문서를 찾을 수 없습니다. id=$it")
        }
        val document = Document(
            title = command.title,
            content = command.content,
            parent = parent,
            createdBy = command.createdBy,
            updatedBy = command.createdBy,
        )
        val revision = DocumentRevision.create(document)
        document.addRevision(revision)
        document.validate()

        val saved = documentRepository.save(document)
        return Pair(saved, saved.revisions.last())
    }
}
