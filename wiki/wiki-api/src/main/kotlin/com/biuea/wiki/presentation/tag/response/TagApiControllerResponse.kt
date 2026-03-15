package com.biuea.wiki.presentation.tag.response

import com.biuea.wiki.domain.tag.entity.TagConstant

data class TagTypeResponse(
    val types: List<TagTypeItem>,
) {
    data class TagTypeItem(
        val name: String,
    )

    companion object {
        fun from(constants: List<TagConstant>): TagTypeResponse {
            return TagTypeResponse(
                types = constants.map { TagTypeItem(name = it.name) }
            )
        }
    }
}
