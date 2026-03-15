package com.biuea.wiki.domain.comment

data class CreateCommentCommand(
    val documentId: Long,
    val content: String,
    val parentId: Long? = null,
    val createdBy: Long,
)

data class UpdateCommentCommand(
    val commentId: Long,
    val content: String,
    val updatedBy: Long,
)

data class DeleteCommentCommand(
    val commentId: Long,
    val deletedBy: Long,
)
