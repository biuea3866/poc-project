package com.biuea.wiki.domain.tag

import com.biuea.wiki.domain.tag.entity.TagConstant

data class SaveTagCommand(
    val tags: List<TagInput>,
    val documentId: Long,
    val documentRevisionId: Long
) {
    data class TagInput(
        val name: String,
        val tagConstant: TagConstant
    )
}
