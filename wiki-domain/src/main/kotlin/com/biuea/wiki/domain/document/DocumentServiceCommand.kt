package com.biuea.wiki.domain.document

data class SaveDocumentCommand(
    val title: String,
    val content: String?,
    val parentId: Long?,
    val createdBy: Long,
)
