package com.biuea.wiki.domain.document

data class SaveDocumentCommand(
    val title: String,
    val content: String?,
    val parentId: Long?,
    val createdBy: Long,
)

data class UpdateDocumentCommand(
    val documentId: Long,
    val userId: Long,
    val title: String,
    val content: String?,
)

data class PublishDocumentCommand(
    val documentId: Long,
    val userId: Long,
)

data class DeleteDocumentCommand(
    val documentId: Long,
    val userId: Long,
)

data class RestoreDocumentCommand(
    val documentId: Long,
    val userId: Long,
)
