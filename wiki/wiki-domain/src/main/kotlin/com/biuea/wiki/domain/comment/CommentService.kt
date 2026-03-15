package com.biuea.wiki.domain.comment

import com.biuea.wiki.domain.comment.entity.Comment
import com.biuea.wiki.domain.comment.exception.CommentNotFoundException
import com.biuea.wiki.domain.comment.exception.CommentNotOwnedException
import com.biuea.wiki.domain.comment.exception.CommentOnInactiveDocumentException
import com.biuea.wiki.domain.common.PageResult
import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.infrastructure.comment.CommentRepository
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val documentRepository: DocumentRepository,
) {

    @Transactional
    fun createComment(command: CreateCommentCommand): Comment {
        val document = documentRepository.findByIdAndDeletedAtIsNull(command.documentId)
            ?: throw IllegalArgumentException("Document not found: ${command.documentId}")

        if (document.status != DocumentStatus.COMPLETED) {
            throw CommentOnInactiveDocumentException(command.documentId)
        }

        val parent = command.parentId?.let {
            commentRepository.findByIdAndDeletedAtIsNull(it)
                ?: throw CommentNotFoundException(it)
        }

        val comment = Comment(
            document = document,
            content = command.content,
            parent = parent,
            createdBy = command.createdBy,
            updatedBy = command.createdBy,
        )
        return commentRepository.save(comment)
    }

    @Transactional(readOnly = true)
    fun listComments(documentId: Long, page: Int, size: Int): PageResult<Comment> {
        documentRepository.findByIdAndDeletedAtIsNull(documentId)
            ?: throw IllegalArgumentException("Document not found: $documentId")
        val result = commentRepository.findTopLevelByDocumentId(documentId, PageRequest.of(page, size))
        return PageResult.from(result)
    }

    @Transactional
    fun updateComment(command: UpdateCommentCommand): Comment {
        val comment = commentRepository.findByIdAndDeletedAtIsNull(command.commentId)
            ?: throw CommentNotFoundException(command.commentId)

        if (!comment.isOwnedBy(command.updatedBy)) {
            throw CommentNotOwnedException(command.commentId, command.updatedBy)
        }

        comment.update(command.content, command.updatedBy)
        return commentRepository.save(comment)
    }

    @Transactional
    fun deleteComment(command: DeleteCommentCommand) {
        val comment = commentRepository.findByIdAndDeletedAtIsNull(command.commentId)
            ?: throw CommentNotFoundException(command.commentId)

        if (!comment.isOwnedBy(command.deletedBy)) {
            throw CommentNotOwnedException(command.commentId, command.deletedBy)
        }

        comment.softDelete(command.deletedBy)
        commentRepository.save(comment)
    }
}
