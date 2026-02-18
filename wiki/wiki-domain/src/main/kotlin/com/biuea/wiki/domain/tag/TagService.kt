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
        return command.tags.map { tag ->
            val tagType = tagTypeRepository.findByTagConstant(tag.tagConstant)
                ?: tagTypeRepository.save(TagType.create(tag.tagConstant))
            val existing = tagRepository.findByNameAndTagType(tag.name, tagType)
            val aggregate = (existing ?: Tag.create(tag.name, tagType)).apply {
                this.validate()
                val tagDocumentMapping = TagDocumentMapping.create(this, document, documentRevision)
                this.addMapping(tagDocumentMapping)
            }
            tagRepository.save(aggregate)
        }
    }
}
