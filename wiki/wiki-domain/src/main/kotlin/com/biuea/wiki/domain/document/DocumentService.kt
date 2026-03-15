package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
) {
    @CacheEvict(value = ["documents", "documentList"], allEntries = true)
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

        return documentRepository.save(document)
    }

    @Cacheable(value = ["documents"], key = "#id")
    @Transactional(readOnly = true)
    fun getDocument(id: Long): Document {
        return documentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw IllegalArgumentException("Document not found: $id")
    }

    @Cacheable(value = ["documentList"], key = "'user:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    fun listDocumentsByUser(userId: Long, pageable: Pageable): Page<Document> {
        return documentRepository.findByCreatedByAndDeletedAtIsNull(userId, pageable)
    }

    @Cacheable(value = ["documentList"], key = "'root:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    fun listRootDocuments(pageable: Pageable): Page<Document> {
        return documentRepository.findByParentIsNullAndDeletedAtIsNull(pageable)
    }
}
