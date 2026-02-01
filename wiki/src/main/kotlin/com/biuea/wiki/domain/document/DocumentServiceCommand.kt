package com.biuea.wiki.domain.document

data class SaveDocumentCommand(
    val title: String,
    val content: String?,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val parentId: Long?,
    val createdBy: Long,
    val tags: List<TagInput> = emptyList()
)

data class TagInput(
    val name: String,
    val type: String
)
