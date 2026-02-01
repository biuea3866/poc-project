package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.domain.document.entity.DocumentTagMap
import com.biuea.wiki.domain.document.entity.Tag
import com.biuea.wiki.domain.document.entity.TagType
import com.biuea.wiki.presentation.document.DocumentRepository
import com.biuea.wiki.presentation.document.DocumentTagMapRepository
import com.biuea.wiki.presentation.document.TagRepository
import com.biuea.wiki.presentation.document.TagTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val tagRepository: TagRepository,
    private val tagTypeRepository: TagTypeRepository,
    private val documentTagMapRepository: DocumentTagMapRepository
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
