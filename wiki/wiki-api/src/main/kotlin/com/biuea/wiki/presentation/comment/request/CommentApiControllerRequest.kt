package com.biuea.wiki.presentation.comment.request

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    @field:NotBlank
    val content: String,
    val parentId: Long? = null,
)

data class UpdateCommentRequest(
    @field:NotBlank
    val content: String,
)
