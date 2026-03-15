package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.ai.AiAgentLog
import com.biuea.wiki.domain.common.PageResult
import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import com.biuea.wiki.infrastructure.ai.AiAgentLogRepository
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import com.biuea.wiki.infrastructure.tag.TagDocumentMappingRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
    private val tagDocumentMappingRepository: TagDocumentMappingRepository,
    private val aiAgentLogRepository: AiAgentLogRepository,
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

    @CacheEvict(value = ["documents", "documentList"], allEntries = true)
    @Transactional
    fun updateDocument(command: UpdateDocumentCommand): Document {
        val document = documentRepository.findByIdAndDeletedAtIsNull(command.id)
            ?: throw IllegalArgumentException("Document not found: ${command.id}")

        document.title = command.title
        document.content = command.content
        document.updatedBy = command.updatedBy

        val revision = DocumentRevision.create(document)
        document.addRevision(revision)

        return documentRepository.save(document)
    }

    @CacheEvict(value = ["documents", "documentList"], allEntries = true)
    @Transactional
    fun deleteDocument(id: Long) {
        val document = documentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw IllegalArgumentException("Document not found: $id")
        document.delete()
        document.children.forEach { it.delete() }
    }

    @CacheEvict(value = ["documents", "documentList"], allEntries = true)
    @Transactional
    fun restoreDocument(id: Long): Document {
        val document = documentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Document not found: $id") }
        if (!document.isDeleted()) throw IllegalStateException("Document is not deleted: $id")

        document.status = DocumentStatus.PENDING
        document.deletedAt = null
        if (document.parent != null && document.parent!!.isDeleted()) {
            document.parent = null
        }
        return document
    }

    @CacheEvict(value = ["documents", "documentList"], allEntries = true)
    @Transactional
    fun publishDocument(id: Long): Document {
        val document = documentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw IllegalArgumentException("Document not found: $id")
        document.status = DocumentStatus.COMPLETED
        return document
    }

    @Cacheable(value = ["documents"], key = "#id")
    @Transactional(readOnly = true)
    fun getDocument(id: Long): Document {
        return documentRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw IllegalArgumentException("Document not found: $id")
    }

    @Cacheable(value = ["documentList"], key = "'user:' + #userId + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    fun listDocumentsByUser(userId: Long, page: Int, size: Int): PageResult<Document> {
        return PageResult.from(documentRepository.findByCreatedByAndDeletedAtIsNull(userId, PageRequest.of(page, size)))
    }

    @Cacheable(value = ["documentList"], key = "'root:' + #page + ':' + #size")
    @Transactional(readOnly = true)
    fun listRootDocuments(page: Int, size: Int): PageResult<Document> {
        return PageResult.from(documentRepository.findByParentIsNullAndDeletedAtIsNull(PageRequest.of(page, size)))
    }

    @Transactional(readOnly = true)
    fun listDocumentsByParent(parentId: Long, page: Int, size: Int): PageResult<Document> {
        return PageResult.from(documentRepository.findByParentIdAndDeletedAtIsNull(parentId, PageRequest.of(page, size)))
    }

    @Transactional(readOnly = true)
    fun listTrash(page: Int, size: Int): PageResult<Document> {
        return PageResult.from(documentRepository.findByStatusAndDeletedAtIsNotNull(DocumentStatus.DELETED, PageRequest.of(page, size)))
    }

    @Transactional(readOnly = true)
    fun getRevisions(documentId: Long, page: Int, size: Int): PageResult<DocumentRevision> {
        documentRepository.findByIdAndDeletedAtIsNull(documentId)
            ?: throw IllegalArgumentException("Document not found: $documentId")
        return PageResult.from(documentRevisionRepository.findByDocumentId(documentId, PageRequest.of(page, size)))
    }

    @Transactional(readOnly = true)
    fun getDocumentTags(documentId: Long): List<TagDocumentMapping> {
        documentRepository.findByIdAndDeletedAtIsNull(documentId)
            ?: throw IllegalArgumentException("Document not found: $documentId")
        return tagDocumentMappingRepository.findByDocumentId(documentId)
    }

    @Transactional(readOnly = true)
    fun getAiStatus(documentId: Long, page: Int, size: Int): PageResult<AiAgentLog> {
        documentRepository.findByIdAndDeletedAtIsNull(documentId)
            ?: throw IllegalArgumentException("Document not found: $documentId")
        return PageResult.from(aiAgentLogRepository.findByDocumentId(documentId, PageRequest.of(page, size)))
    }
}
