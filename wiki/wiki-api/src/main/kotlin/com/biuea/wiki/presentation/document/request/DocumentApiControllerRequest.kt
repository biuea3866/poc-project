package com.biuea.wiki.presentation.document.request

import com.biuea.wiki.domain.tag.entity.TagConstant
import jakarta.validation.constraints.NotBlank

data class CreateDocumentRequest(
    @field:NotBlank
    val title: String,
    val content: String? = null,
    val parentId: Long? = null,
    val tags: List<TagInputRequest> = emptyList(),
)

data class UpdateDocumentRequest(
    @field:NotBlank
    val title: String,
    val content: String? = null,
)

data class TagInputRequest(
    @field:NotBlank
    val name: String,
    val tagConstant: TagConstant,
)
