package com.biuea.wiki.presentation.comment

import com.biuea.wiki.domain.comment.Comment
import com.biuea.wiki.infrastructure.comment.CommentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
) {
    @Transactional(readOnly = true)
    fun getComments(documentId: Long): List<CommentTreeResponse> {
        val comments = commentRepository.findAllByDocumentId(documentId)
        val roots = comments.filter { it.parentId == null }
        val childrenByParent = comments.filter { it.parentId != null }.groupBy { it.parentId }
        return roots.map { root ->
            CommentTreeResponse.from(root, childrenByParent[root.id] ?: emptyList())
        }
    }

    fun createComment(documentId: Long, authorId: Long, content: String, parentId: Long?): CommentTreeResponse {
        if (parentId != null) {
            val parent = commentRepository.findById(parentId)
                .orElseThrow { IllegalArgumentException("부모 댓글을 찾을 수 없습니다: $parentId") }
            require(parent.parentId == null) { "대댓글의 대댓글은 허용되지 않습니다" }
        }
        val comment = commentRepository.save(
            Comment(
                documentId = documentId,
                authorId = authorId,
                content = content,
                parentId = parentId,
            )
        )
        return CommentTreeResponse.from(comment)
    }

    fun updateComment(commentId: Long, authorId: Long, content: String): CommentTreeResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId") }
        require(comment.authorId == authorId) { "작성자만 수정할 수 있습니다" }
        comment.content = content
        comment.updatedAt = LocalDateTime.now()
        val saved = commentRepository.save(comment)
        return CommentTreeResponse.from(saved)
    }

    fun deleteComment(commentId: Long, authorId: Long) {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId") }
        require(comment.authorId == authorId) { "작성자만 삭제할 수 있습니다" }
        comment.isDeleted = true
        comment.deletedAt = LocalDateTime.now()
        commentRepository.save(comment)
    }
}
