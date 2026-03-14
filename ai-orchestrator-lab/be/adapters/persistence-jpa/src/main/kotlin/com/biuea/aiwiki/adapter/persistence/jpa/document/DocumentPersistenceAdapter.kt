package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentJpaEntity
import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentRevisionJpaEntity
import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import com.biuea.aiwiki.domain.document.port.DocumentRevisionRepository
import org.springframework.stereotype.Component

@Component
class DocumentPersistenceAdapter(
    private val documentJpaRepository: DocumentJpaRepository,
    private val documentRevisionJpaRepository: DocumentRevisionJpaRepository,
) : DocumentRepository, DocumentRevisionRepository {
    override fun save(document: Document): Document =
        documentJpaRepository.save(DocumentJpaEntity.from(document)).toDomain()

    override fun findById(id: Long): Document? =
        documentJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun searchActiveOwnedByUser(query: String, userId: Long): List<Document> =
        documentJpaRepository.findByCreatedByAndStatus(userId, DocumentStatus.ACTIVE.name)
            .map { it.toDomain() }
            .filter {
                val normalized = query.trim()
                normalized.isBlank() ||
                    it.title.contains(normalized, ignoreCase = true) ||
                    it.content.contains(normalized, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(normalized, ignoreCase = true) }
            }

    override fun save(revision: DocumentRevision): DocumentRevision =
        documentRevisionJpaRepository.save(DocumentRevisionJpaEntity.from(revision)).toDomain()

    override fun findByDocumentId(documentId: Long): List<DocumentRevision> =
        documentRevisionJpaRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
            .map { it.toDomain() }
}
