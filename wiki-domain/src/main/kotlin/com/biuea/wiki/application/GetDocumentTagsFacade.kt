package com.biuea.wiki.application

import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import com.biuea.wiki.infrastructure.tag.TagDocumentMappingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetDocumentTagsFacade(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
    private val tagDocumentMappingRepository: TagDocumentMappingRepository,
) {
    @Transactional(readOnly = true)
    fun getTags(documentId: Long): List<TagItem> {
        documentRepository.findByIdAndStatusNot(documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=$documentId")

        val latestRevision = documentRevisionRepository.findTopByDocumentIdOrderByCreatedAtDesc(documentId)
            ?: return emptyList()

        return tagDocumentMappingRepository
            .findByDocumentIdAndRevisionId(documentId, latestRevision.id)
            .map { mapping ->
                TagItem(
                    id = mapping.tag.id,
                    name = mapping.tag.name,
                    tagConstant = mapping.tag.tagType.tagConstant,
                )
            }
    }
}

data class TagItem(
    val id: Long,
    val name: String,
    val tagConstant: TagConstant,
)
