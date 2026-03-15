package com.biuea.wiki.presentation.comment.response

import com.biuea.wiki.domain.comment.entity.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val documentId: Long,
    val content: String?,
    val isDeleted: Boolean,
    val parentId: Long?,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val replies: List<CommentResponse>,
) {
    companion object {
        fun from(comment: Comment): CommentResponse {
            val displayContent = if (comment.isDeleted) null else comment.content
            return CommentResponse(
                id = comment.id,
                documentId = comment.document.id,
                content = displayContent,
                isDeleted = comment.isDeleted,
                parentId = comment.parent?.id,
                createdBy = comment.createdBy,
                updatedBy = comment.updatedBy,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                replies = comment.replies
                    .filter { reply -> !reply.isDeleted || reply.replies.any { !it.isDeleted } }
                    .map { reply ->
                        CommentResponse(
                            id = reply.id,
                            documentId = reply.document.id,
                            content = if (reply.isDeleted) null else reply.content,
                            isDeleted = reply.isDeleted,
                            parentId = reply.parent?.id,
                            createdBy = reply.createdBy,
                            updatedBy = reply.updatedBy,
                            createdAt = reply.createdAt,
                            updatedAt = reply.updatedAt,
                            replies = emptyList(),
                        )
                    }
            )
        }
    }
}

data class CommentListResponse(
    val comments: List<CommentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
