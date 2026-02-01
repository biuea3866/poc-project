package com.biuea.wiki.domain.document

import com.biuea.wiki.presentation.document.DocumentRepository
import com.biuea.wiki.presentation.document.DocumentTagMapRepository
import com.biuea.wiki.presentation.document.TagRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val tagRepository: TagRepository,
    private val documentTagMapRepository: DocumentTagMapRepository,
    private val objectMapper: ObjectMapper
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

        val revisionData = objectMapper.writeValueAsString(
            mapOf(
                "title" to document.title,
                "content" to document.content,
                "status" to document.status.name,
                "parentId" to parent?.id,
                "createdBy" to document.createdBy,
                "updatedBy" to document.updatedBy
            )
        )

        val revision = DocumentRevision(
            data = revisionData,
            document = document,
            createdBy = command.createdBy
        )
        document.addRevision(revision)

        val normalizedTags = command.tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        if (normalizedTags.isNotEmpty()) {
            val existingTags = tagRepository.findByNameIn(normalizedTags).associateBy { it.name }
            val newTags = normalizedTags
                .filterNot { existingTags.containsKey(it) }
                .map { Tag.create(it) }

            val savedNewTags = if (newTags.isNotEmpty()) tagRepository.saveAll(newTags) else emptyList()
            val allTags = existingTags.values + savedNewTags
            val tagMaps = allTags.map { DocumentTagMap.create(it, document, revision) }

            document.addTagMaps(tagMaps, revision)
            documentTagMapRepository.saveAll(tagMaps)
        }

        return documentRepository.save(document)
    }
}
