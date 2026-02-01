package com.biuea.wiki.domain.document

import com.biuea.wiki.presentation.document.DocumentRepository
import com.biuea.wiki.presentation.document.DocumentTagMapRepository
import com.biuea.wiki.presentation.document.TagRepository
import com.biuea.wiki.presentation.document.TagTypeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val tagRepository: TagRepository,
    private val tagTypeRepository: TagTypeRepository,
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
            .map { TagInput(name = it.name.trim(), type = it.type.trim().uppercase()) }
            .filter { it.name.isNotBlank() && it.type.isNotBlank() }

        if (normalizedTags.isNotEmpty()) {
            val typeNames = normalizedTags.map { it.type }.toSet()
            val existingTypes = tagTypeRepository.findByNameIn(typeNames).associateBy { it.name }
            val newTypes = typeNames
                .filterNot { existingTypes.containsKey(it) }
                .map { TagType.create(it) }
            val savedTypes = if (newTypes.isNotEmpty()) tagTypeRepository.saveAll(newTypes) else emptyList()
            val allTypes = existingTypes.values.associateBy { it.name } + savedTypes.associateBy { it.name }

            val tagMaps = mutableListOf<DocumentTagMap>()
            val tagsToSave = mutableListOf<Tag>()

            normalizedTags.forEach { input ->
                val tagType = allTypes[input.type] ?: return@forEach
                val existingTag = tagRepository.findByNameAndTagType(input.name, tagType)
                val tag = existingTag ?: Tag.create(input.name, tagType).also { tagsToSave.add(it) }
                tagMaps.add(DocumentTagMap.create(tag, document, revision))
            }

            if (tagsToSave.isNotEmpty()) {
                tagRepository.saveAll(tagsToSave)
            }

            document.addTagMaps(tagMaps, revision)
            documentTagMapRepository.saveAll(tagMaps)
        }

        return documentRepository.save(document)
    }
}
