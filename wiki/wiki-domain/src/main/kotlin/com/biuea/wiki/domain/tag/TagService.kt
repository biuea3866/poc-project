package com.biuea.wiki.domain.tag

import com.biuea.wiki.domain.tag.entity.Tag
import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import com.biuea.wiki.domain.tag.entity.TagType
import com.biuea.wiki.infrastructure.document.DocumentRepository
import com.biuea.wiki.infrastructure.document.DocumentRevisionRepository
import com.biuea.wiki.infrastructure.tag.TagRepository
import com.biuea.wiki.infrastructure.tag.TagTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val tagTypeRepository: TagTypeRepository,
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository
) {
    @Transactional
    fun saveTags(command: SaveTagCommand): List<Tag> {
        val document = documentRepository.getReferenceById(command.documentId)
        val documentRevision = documentRevisionRepository.getReferenceById(command.documentRevisionId)

        val allConstants = command.tags.map { it.tagConstant }.distinct()
        val existingTagTypes = tagTypeRepository.findByTagConstantIn(allConstants)
        val tagTypeByConstant = existingTagTypes.associateBy { it.tagConstant }.toMutableMap()

        val missingConstants = allConstants - tagTypeByConstant.keys
        val newTagTypes = tagTypeRepository.saveAll(missingConstants.map { TagType.create(it) })
        newTagTypes.forEach { tagTypeByConstant[it.tagConstant] = it }

        val tagNames = command.tags.map { it.name }
        val tagTypes = tagTypeByConstant.values.toList()
        val existingTags = tagRepository.findByNameInAndTagTypeIn(tagNames, tagTypes)
        val existingTagMap = existingTags.associateBy { it.name to it.tagType.id }

        return command.tags.map { tag ->
            val tagType = tagTypeByConstant[tag.tagConstant]!!
            val existing = existingTagMap[tag.name to tagType.id]
            val aggregate = (existing ?: Tag.create(tag.name, tagType)).apply {
                this.validate()
                val tagDocumentMapping = TagDocumentMapping.create(this, document, documentRevision)
                this.addMapping(tagDocumentMapping)
            }
            tagRepository.save(aggregate)
        }
    }
}
