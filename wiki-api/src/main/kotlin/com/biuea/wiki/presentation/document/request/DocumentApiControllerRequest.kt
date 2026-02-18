package com.biuea.wiki.presentation.document.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateDocumentRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    val content: String?,
    val parentId: Long?,
)

data class UpdateDocumentRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,
    val content: String?,
)
