package com.biuea.wiki.application

import com.biuea.wiki.domain.document.entity.AiStatus
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentSummaryRepository
import com.biuea.wiki.infrastructure.tag.TagDocumentMappingRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetDocumentFacade(
    private val documentRepository: DocumentRepository,
    private val documentSummaryRepository: DocumentSummaryRepository,
    private val tagDocumentMappingRepository: TagDocumentMappingRepository,
) {
    @Transactional(readOnly = true)
    fun getDocument(documentId: Long, requestUserId: Long): DocumentDetailOutput {
        val document = documentRepository.findByIdAndStatusNot(documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")

        // DRAFT는 작성자 본인만 조회 가능
        if (document.isDraft() && document.createdBy != requestUserId) {
            throw IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")
        }

        val latestRevision = document.revisions.lastOrNull()
        val summary = latestRevision?.let {
            documentSummaryRepository.findByDocumentIdAndDocumentRevisionId(document.id, it.id)
        }
        val tags = latestRevision?.let {
            tagDocumentMappingRepository.findByDocumentIdAndRevisionId(document.id, it.id)
        } ?: emptyList()

        return DocumentDetailOutput(
            id = document.id,
            title = document.title,
            content = document.content,
            status = document.status,
            aiStatus = document.aiStatus,
            parentId = document.parent?.id,
            summary = summary?.content,
            tags = tags.map {
                DocumentDetailOutput.TagItem(
                    id = it.tag.id,
                    name = it.tag.name,
                    tagConstant = it.tag.tagType.tagConstant,
                )
            },
            createdBy = document.createdBy,
            revisionId = latestRevision?.id,
        )
    }

    @Transactional(readOnly = true)
    fun listDocuments(parentId: Long?, pageable: Pageable): Page<DocumentListItem> {
        return if (parentId == null) {
            documentRepository.findByParentIsNullAndStatus(DocumentStatus.ACTIVE, pageable)
        } else {
            documentRepository.findByParentIdAndStatus(parentId, DocumentStatus.ACTIVE, pageable)
        }.map { doc ->
            DocumentListItem(
                id = doc.id,
                title = doc.title,
                status = doc.status,
                aiStatus = doc.aiStatus,
                parentId = doc.parent?.id,
                childCount = doc.children.count { it.isActive() },
            )
        }
    }

    @Transactional(readOnly = true)
    fun listTrash(userId: Long, pageable: Pageable): Page<DocumentListItem> {
        return documentRepository.findByCreatedByAndStatus(userId, DocumentStatus.DELETED, pageable)
            .map { doc ->
                DocumentListItem(
                    id = doc.id,
                    title = doc.title,
                    status = doc.status,
                    aiStatus = doc.aiStatus,
                    parentId = doc.parent?.id,
                    childCount = 0,
                )
            }
    }
}

data class DocumentDetailOutput(
    val id: Long,
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
    val summary: String?,
    val tags: List<TagItem>,
    val createdBy: Long,
    val revisionId: Long?,
) {
    data class TagItem(
        val id: Long,
        val name: String,
        val tagConstant: TagConstant,
    )
}

data class DocumentListItem(
    val id: Long,
    val title: String,
    val status: DocumentStatus,
    val aiStatus: AiStatus,
    val parentId: Long?,
    val childCount: Int,
)
