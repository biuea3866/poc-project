package com.biuea.wiki.application

import com.biuea.wiki.domain.document.DocumentService
import com.biuea.wiki.domain.document.SaveDocumentCommand
import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.SaveTagCommand
import com.biuea.wiki.domain.tag.TagService
import com.biuea.wiki.domain.tag.entity.TagConstant
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SaveDocumentFacade(
    private val documentService: DocumentService,
    private val tagService: TagService,
) {
    @Transactional
    fun saveDocument(input: SaveDocumentInput): SaveDocumentOutput {
        val document = documentService.saveDocument(
            SaveDocumentCommand(
                title = input.title,
                content = input.content,
                status = input.status,
                parentId = input.parentId,
                createdBy = input.createdBy
            )
        )
        val tags = tagService.saveTags(
            SaveTagCommand(
                tags = input.tags.map {
                    SaveTagCommand.TagInput(
                        name = it.name,
                        tagConstant = it.tagConstant
                    )
                },
                documentId = document.id,
                documentRevisionId = document.latestRevisionId
            )
        )

        return SaveDocumentOutput.of(document, tags)
    }
}

data class SaveDocumentInput(
    val title: String,
    val content: String?,
    val status: DocumentStatus,
    val parentId: Long?,
    val createdBy: Long,
    val tags: List<TagInput>
) {
    data class TagInput(
        val name: String,
        val tagConstant : TagConstant,
    )
}

data class SaveDocumentOutput(
    val document: Document,
    val tags: List<Tag>
) {
    data class Document(
        val id: Long,
        val title: String,
        val content: String?,
        val status: DocumentStatus,
    )

    data class Tag(
        val id: Long,
        val name: String,
        val tagConstant: TagConstant
    )

    companion object {
        fun of(
            document: com.biuea.wiki.domain.document.entity.Document,
            tags: List<com.biuea.wiki.domain.tag.entity.Tag>
        ): SaveDocumentOutput {
            return SaveDocumentOutput(
                document = Document(
                    id = document.id,
                    title = document.title,
                    content = document.content,
                    status = document.status
                ),
                tags = tags.map {
                    Tag(
                        id = it.id,
                        name = it.name,
                        tagConstant = it.tagType.tagConstant
                    )
                }
            )
        }
    }
}