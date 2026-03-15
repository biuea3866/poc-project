package com.biuea.wiki.presentation.comment

import com.biuea.wiki.domain.auth.AuthenticatedUser
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class CommentController(
    private val commentService: CommentService,
) {
    @GetMapping("/documents/{id}/comments")
    fun getComments(
        @PathVariable id: Long,
    ): ResponseEntity<List<CommentTreeResponse>> {
        return ResponseEntity.ok(commentService.getComments(id))
    }

    @PostMapping("/documents/{id}/comments")
    fun createComment(
        @PathVariable id: Long,
        @RequestBody @Valid body: CreateCommentRequest,
        authentication: Authentication,
    ): ResponseEntity<CommentTreeResponse> {
        val user = authentication.principal as AuthenticatedUser
        val comment = commentService.createComment(
            documentId = id,
            authorId = user.id,
            content = body.content,
            parentId = body.parentId,
        )
        return ResponseEntity.ok(comment)
    }

    @PutMapping("/comments/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody @Valid body: UpdateCommentRequest,
        authentication: Authentication,
    ): ResponseEntity<CommentTreeResponse> {
        val user = authentication.principal as AuthenticatedUser
        val comment = commentService.updateComment(
            commentId = commentId,
            authorId = user.id,
            content = body.content,
        )
        return ResponseEntity.ok(comment)
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        val user = authentication.principal as AuthenticatedUser
        commentService.deleteComment(commentId = commentId, authorId = user.id)
        return ResponseEntity.noContent().build()
    }
}
