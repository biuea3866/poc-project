package com.biuea.wiki.presentation.comment

import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.domain.comment.CommentService
import com.biuea.wiki.domain.comment.CreateCommentCommand
import com.biuea.wiki.domain.comment.DeleteCommentCommand
import com.biuea.wiki.domain.comment.UpdateCommentCommand
import com.biuea.wiki.presentation.comment.request.CreateCommentRequest
import com.biuea.wiki.presentation.comment.request.UpdateCommentRequest
import com.biuea.wiki.presentation.comment.response.CommentListResponse
import com.biuea.wiki.presentation.comment.response.CommentResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CommentApiController(
    private val commentService: CommentService,
) {

    @GetMapping("/api/v1/documents/{documentId}/comments")
    fun listComments(
        @PathVariable documentId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<CommentListResponse> {
        val result = commentService.listComments(documentId, page, size)
        return ResponseEntity.ok(
            CommentListResponse(
                comments = result.content.map { CommentResponse.from(it) },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        )
    }

    @PostMapping("/api/v1/documents/{documentId}/comments")
    fun createComment(
        @PathVariable documentId: Long,
        @RequestBody @Valid request: CreateCommentRequest,
        authentication: Authentication,
    ): ResponseEntity<CommentResponse> {
        val user = authentication.principal as AuthenticatedUser
        val comment = commentService.createComment(
            CreateCommentCommand(
                documentId = documentId,
                content = request.content,
                parentId = request.parentId,
                createdBy = user.id,
            )
        )
        return ResponseEntity.ok(CommentResponse.from(comment))
    }

    @PutMapping("/api/v1/comments/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody @Valid request: UpdateCommentRequest,
        authentication: Authentication,
    ): ResponseEntity<CommentResponse> {
        val user = authentication.principal as AuthenticatedUser
        val comment = commentService.updateComment(
            UpdateCommentCommand(
                commentId = commentId,
                content = request.content,
                updatedBy = user.id,
            )
        )
        return ResponseEntity.ok(CommentResponse.from(comment))
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val user = authentication.principal as AuthenticatedUser
        commentService.deleteComment(
            DeleteCommentCommand(
                commentId = commentId,
                deletedBy = user.id,
            )
        )
        return ResponseEntity.noContent().build()
    }
}
