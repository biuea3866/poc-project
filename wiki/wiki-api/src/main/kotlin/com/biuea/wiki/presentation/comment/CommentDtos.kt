package com.biuea.wiki.presentation.comment

import com.biuea.wiki.domain.comment.Comment
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CommentTreeResponse(
    val id: Long,
    val authorId: Long,
    val content: String,
    val parentId: Long?,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val replies: List<CommentTreeResponse> = emptyList(),
) {
    companion object {
        fun from(comment: Comment, replies: List<Comment> = emptyList()): CommentTreeResponse =
            CommentTreeResponse(
                id = comment.id,
                authorId = comment.authorId,
                content = if (comment.isDeleted) "삭제된 댓글입니다." else comment.content,
                parentId = comment.parentId,
                isDeleted = comment.isDeleted,
                createdAt = comment.createdAt,
                replies = replies.map { from(it) },
            )
    }
}

data class CreateCommentRequest(
    @field:NotBlank
    val content: String,
    val parentId: Long? = null,
)

data class UpdateCommentRequest(
    @field:NotBlank
    val content: String,
)
