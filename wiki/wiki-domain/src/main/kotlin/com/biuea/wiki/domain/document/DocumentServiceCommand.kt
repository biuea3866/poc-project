package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.DocumentStatus
import com.biuea.wiki.domain.tag.entity.TagConstant

data class SaveDocumentCommand(
    val title: String,
    val content: String?,
    val status: DocumentStatus = DocumentStatus.PENDING,
    val parentId: Long?,
    val createdBy: Long,
)

data class TagInput(
    val name: String,
    val tagConstant: TagConstant
)
